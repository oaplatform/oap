/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.message.client;

import lombok.extern.slf4j.Slf4j;
import oap.message.client.MessageSender.SpoolLimit;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Disk spool of a {@link MessageSender}: a segmented append-only log per priority.
 * <pre>
 * &lt;directory&gt;/&lt;clientIdHex&gt;/.lock                 - locked (FileChannel.lock) while the owner lives
 * &lt;directory&gt;/&lt;clientIdHex&gt;/p&lt;priority&gt;/&lt;seq&gt;.seg  - see {@link SpoolSegment}
 * </pre>
 * The directory of a dead sender ( its lock can be taken ) is adopted: its segments are moved into the own directory.
 */
@Slf4j
final class MessageSpool implements Closeable {
    static final String LOCK = ".lock";
    private static final Pattern PRIORITY_DIRECTORY = Pattern.compile( "p(-?\\d+)" );
    private static final long FREE_SPACE_REFRESH = 1000;
    /**
     * the spool directories locked by this JVM: closing any channel of a locked file releases the lock of the process
     * ( POSIX ), so the adoption must not even open them
     */
    private static final Set<Path> LOCKED = ConcurrentHashMap.newKeySet();

    final Path directory;
    final Path root;
    private final String uniqueName;
    private final ConcurrentSkipListMap<Integer, Stream> streams = new ConcurrentSkipListMap<>( Comparator.reverseOrder() );
    private final Set<SpoolSegment> segments = ConcurrentHashMap.newKeySet();
    private final AtomicLong bytes = new AtomicLong();
    private final AtomicLong seq = new AtomicLong();
    /**
     * read records not yet final ( in the memory of the sender )
     */
    private final AtomicInteger inFlight = new AtomicInteger();
    private final ReentrantLock openLock = new ReentrantLock();
    private FileChannel lockChannel;
    private FileLock fileLock;
    private volatile boolean opened;
    private volatile boolean closed;
    private volatile long freeSpace = -1;
    private volatile long freeSpaceTime;

    MessageSpool( Path directory, long clientId, String uniqueName ) {
        this.directory = directory.toAbsolutePath().normalize();
        this.root = this.directory.resolve( Long.toHexString( clientId ) );
        this.uniqueName = uniqueName;
    }

    private void open() throws IOException {
        if( opened ) return;

        openLock.lock();
        try {
            if( opened ) return;
            if( closed ) throw new ClosedChannelException();

            Files.createDirectories( root );
            try {
                // another sender of this JVM can check for a moment whether the directory is an orphan
                for( int i = 0; !LOCKED.add( root ); i++ ) {
                    if( i >= 100 ) throw new IOException( "cannot lock " + root );
                    Thread.sleep( 10 );
                }
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new IOException( e );
            }

            FileChannel channel = null;
            try {
                channel = FileChannel.open( root.resolve( LOCK ), CREATE, WRITE );
                FileLock lock = null;
                // another process can hold it for a moment while it checks whether the directory is an orphan
                for( int i = 0; lock == null; i++ ) {
                    lock = channel.tryLock();
                    if( lock == null ) {
                        if( i >= 100 ) throw new IOException( "cannot lock " + root.resolve( LOCK ) );
                        Thread.sleep( 10 );
                    }
                }
                lockChannel = channel;
                fileLock = lock;
            } catch( IOException e ) {
                if( channel != null ) channel.close();
                LOCKED.remove( root );
                throw e;
            } catch( InterruptedException e ) {
                channel.close();
                LOCKED.remove( root );
                Thread.currentThread().interrupt();
                throw new IOException( e );
            }
            opened = true;
            log.info( "[{}] spool {}", uniqueName, root );
        } finally {
            openLock.unlock();
        }
    }

    private Stream stream( int priority ) {
        return streams.computeIfAbsent( priority, Stream::new );
    }

    /**
     * @param segmentSize the active segment is closed when it reaches the size
     * @return number of bytes written
     */
    int append( int priority, byte messageType, short version, byte[] md5, byte[] data, int offset, int length,
                long segmentSize, SpoolSync sync ) throws IOException {
        if( closed ) throw new ClosedChannelException();
        open();
        return stream( priority ).append( messageType, version, md5, data, offset, length, segmentSize, sync );
    }

    /**
     * the next record of the highest priority, null - nothing to replay; the record is in flight until
     * {@link #ack(SpoolSegment)} / {@link #release()}
     */
    SpoolSegment.Record next( Consumer<String> event ) {
        if( closed ) return null;
        for( Stream stream : streams.values() ) {
            SpoolSegment.Record record = stream.next( event );
            if( record != null ) {
                inFlight.incrementAndGet();
                return record;
            }
        }
        return null;
    }

    /**
     * the record is final ( delivered or dropped )
     */
    void ack( SpoolSegment segment ) {
        inFlight.decrementAndGet();
        if( segment.ack() ) delete( segment );
    }

    /**
     * the record is dropped from memory without a final status, it stays on disk ( replayed by the next owner )
     */
    void release() {
        inFlight.decrementAndGet();
    }

    int inFlight() {
        return inFlight.get();
    }

    private void delete( SpoolSegment segment ) {
        try {
            Files.deleteIfExists( segment.path );
        } catch( IOException e ) {
            log.error( "[{}] cannot delete {}", uniqueName, segment.path, e );
        }
        if( segments.remove( segment ) ) bytes.addAndGet( -segment.bytes );
        log.trace( "[{}] deleted {}", uniqueName, segment );
    }

    /**
     * fdatasync of the active segments with appended data
     */
    void sync() {
        for( Stream stream : streams.values() ) {
            SpoolSegment.Writer writer = stream.writer;
            if( writer == null || !writer.dirty ) continue;
            try {
                writer.force();
            } catch( ClosedChannelException ignored ) {
                // rolled, synced by the roll
            } catch( IOException e ) {
                log.error( "[{}] sync {}", uniqueName, writer.segment.path, e );
            }
        }
    }

    /**
     * moves the segments of the dead senders ( their lock can be taken ) into the own directory
     *
     * @param expiration a directory without segments is deleted when its lock file is older
     * @return number of adopted segments
     */
    int adoptOrphans( long expiration ) {
        if( closed || !Files.isDirectory( directory ) ) return 0;

        int adopted = 0;
        List<Path> candidates = new ArrayList<>();
        try( DirectoryStream<Path> stream = Files.newDirectoryStream( directory, Files::isDirectory ) ) {
            for( Path path : stream ) {
                Path dir = path.toAbsolutePath().normalize();
                if( !dir.equals( root ) && !LOCKED.contains( dir ) ) candidates.add( dir );
            }
        } catch( IOException e ) {
            log.error( "[{}] spool {}", uniqueName, directory, e );
            return 0;
        }

        for( Path dir : candidates ) {
            if( !LOCKED.add( dir ) ) continue;
            try {
                adopted += adopt( dir, expiration );
            } catch( NoSuchFileException ignored ) {
                // deleted by its owner / another sender
            } catch( IOException e ) {
                log.error( "[{}] cannot adopt {}", uniqueName, dir, e );
            } finally {
                LOCKED.remove( dir );
            }
        }

        return adopted;
    }

    private int adopt( Path dir, long expiration ) throws IOException {
        Path lockFile = dir.resolve( LOCK );
        int adopted = 0;
        try( FileChannel channel = FileChannel.open( lockFile, CREATE, WRITE ) ) {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch( OverlappingFileLockException e ) {
                return 0;
            }
            if( lock == null ) return 0; // alive

            try( DirectoryStream<Path> priorities = Files.newDirectoryStream( dir, Files::isDirectory ) ) {
                for( Path priorityDir : priorities ) {
                    var matcher = PRIORITY_DIRECTORY.matcher( priorityDir.getFileName().toString() );
                    if( !matcher.matches() ) continue;
                    int priority = Integer.parseInt( matcher.group( 1 ) );

                    List<Path> files = new ArrayList<>();
                    try( DirectoryStream<Path> segmentFiles = Files.newDirectoryStream( priorityDir, "*" + SpoolSegment.EXTENSION ) ) {
                        segmentFiles.forEach( files::add );
                    }
                    files.sort( Comparator.comparing( p -> p.getFileName().toString() ) );

                    for( Path file : files ) {
                        if( adopt( priority, file ) ) adopted++;
                    }

                    try {
                        Files.deleteIfExists( priorityDir );
                    } catch( DirectoryNotEmptyException ignored ) {
                        // not a segment
                    }
                }
            }
        }

        // a directory without segments may belong to a sender that is just starting: deleted only when expired
        if( adopted > 0
            || expiration > 0 && Files.getLastModifiedTime( lockFile ).toMillis() + expiration < System.currentTimeMillis() ) {
            try {
                Files.deleteIfExists( lockFile );
                Files.deleteIfExists( dir );
            } catch( DirectoryNotEmptyException ignored ) {
                // unknown files
            }
        }

        if( adopted > 0 ) log.info( "[{}] adopted {} segment(s) of {}", uniqueName, adopted, dir );
        return adopted;
    }

    private boolean adopt( int priority, Path file ) throws IOException {
        open();
        Stream stream = stream( priority );
        Files.createDirectories( stream.directory );
        SpoolSegment segment = new SpoolSegment( stream.directory.resolve( SpoolSegment.fileName( seq.incrementAndGet() ) ), priority );
        Files.move( file, segment.path, StandardCopyOption.ATOMIC_MOVE );

        SpoolSegment.scan( segment );
        if( segment.records == 0 ) {
            Files.deleteIfExists( segment.path );
            return false;
        }
        segments.add( segment );
        bytes.addAndGet( segment.bytes );
        stream.closed.add( segment );
        log.debug( "[{}] adopted {} -> {}", uniqueName, file, segment );
        return true;
    }

    SpoolLimit limit( long maxAge, long maxSize, long minFreeSpace ) {
        if( maxSize > 0 && bytes.get() >= maxSize ) return SpoolLimit.SIZE;
        if( maxAge > 0 ) {
            long oldest = oldestTime();
            if( oldest != Long.MAX_VALUE && DateTimeUtils.currentTimeMillis() - oldest >= maxAge ) return SpoolLimit.AGE;
        }
        if( minFreeSpace > 0 ) {
            long free = freeSpace();
            if( free >= 0 && free < minFreeSpace ) return SpoolLimit.FREE_SPACE;
        }
        return SpoolLimit.NONE;
    }

    /**
     * usable space of the file store of the spool, refreshed at most once a second; -1 - unknown
     */
    long freeSpace() {
        long now = System.currentTimeMillis();
        if( freeSpace >= 0 && now - freeSpaceTime < FREE_SPACE_REFRESH ) return freeSpace;

        Path path = root;
        while( path != null && !Files.exists( path ) ) path = path.getParent();
        long free = -1;
        if( path != null ) {
            try {
                free = Files.getFileStore( path ).getUsableSpace();
            } catch( IOException e ) {
                log.warn( "[{}] free space of {}: {}", uniqueName, path, e.getMessage() );
            }
        }
        freeSpace = free;
        freeSpaceTime = now;
        return free;
    }

    long bytes() {
        return bytes.get();
    }

    int segments() {
        return segments.size();
    }

    long messages() {
        long messages = 0;
        for( SpoolSegment segment : segments ) messages += segment.pending();
        return messages;
    }

    /**
     * enqueue time of the oldest not deleted segment; Long.MAX_VALUE - empty
     */
    long oldestTime() {
        long oldest = Long.MAX_VALUE;
        for( SpoolSegment segment : segments ) oldest = Math.min( oldest, segment.firstTime );
        return oldest;
    }

    long oldestAge() {
        long oldest = oldestTime();
        return oldest == Long.MAX_VALUE ? 0 : Math.max( 0, DateTimeUtils.currentTimeMillis() - oldest );
    }

    /**
     * syncs and closes the segments, releases the lock; the not delivered segments stay for the next owner
     */
    @Override
    public void close() {
        openLock.lock();
        try {
            if( closed ) return;
            closed = true;

            for( Stream stream : streams.values() ) stream.close();

            if( opened ) {
                try {
                    fileLock.release();
                    lockChannel.close();
                } catch( IOException e ) {
                    log.error( "[{}] cannot release {}", uniqueName, root.resolve( LOCK ), e );
                }
                LOCKED.remove( root );

                if( segments.isEmpty() ) deleteRoot();
            }
        } finally {
            openLock.unlock();
        }
    }

    private void deleteRoot() {
        try {
            for( Stream stream : streams.values() ) Files.deleteIfExists( stream.directory );
            Files.deleteIfExists( root.resolve( LOCK ) );
            Files.deleteIfExists( root );
        } catch( DirectoryNotEmptyException ignored ) {
            // unknown files
        } catch( IOException e ) {
            log.warn( "[{}] cannot delete {}: {}", uniqueName, root, e.getMessage() );
        }
    }

    private final class Stream {
        final int priority;
        final Path directory;
        /**
         * closed segments in the order of replay
         */
        final ConcurrentLinkedDeque<SpoolSegment> closed = new ConcurrentLinkedDeque<>();
        private final ReentrantLock writeLock = new ReentrantLock();
        private final ReentrantLock readLock = new ReentrantLock();
        /**
         * guarded by writeLock, read without the lock by {@link MessageSpool#sync()}
         */
        volatile SpoolSegment.Writer writer;
        // guarded by readLock
        private SpoolSegment.Reader reader;

        Stream( int priority ) {
            this.priority = priority;
            this.directory = root.resolve( "p" + priority );
        }

        int append( byte messageType, short version, byte[] md5, byte[] data, int offset, int length,
                    long segmentSize, SpoolSync sync ) throws IOException {
            writeLock.lock();
            try {
                if( MessageSpool.this.closed ) throw new ClosedChannelException();

                SpoolSegment.Writer writer = this.writer;
                if( writer == null ) writer = this.writer = newWriter();

                int written;
                try {
                    written = writer.append( messageType, version, DateTimeUtils.currentTimeMillis(), md5, data, offset, length );
                } catch( IOException e ) {
                    // the next message goes to a new segment
                    roll();
                    throw e;
                }
                bytes.addAndGet( written );

                if( sync == SpoolSync.ALWAYS ) writer.force();
                if( writer.position >= segmentSize ) roll();

                return written;
            } finally {
                writeLock.unlock();
            }
        }

        private SpoolSegment.Writer newWriter() throws IOException {
            Files.createDirectories( directory );
            SpoolSegment segment = new SpoolSegment( directory.resolve( SpoolSegment.fileName( seq.incrementAndGet() ) ), priority );
            SpoolSegment.Writer writer = segment.writer();
            segments.add( segment );
            bytes.addAndGet( segment.bytes );
            return writer;
        }

        /**
         * closes the active segment and makes it available for the replay; under writeLock
         */
        private void roll() {
            SpoolSegment.Writer writer = this.writer;
            if( writer == null ) return;
            this.writer = null;

            try {
                writer.force();
            } catch( IOException e ) {
                log.error( "[{}] sync {}", uniqueName, writer.segment.path, e );
            }
            try {
                writer.close();
            } catch( IOException e ) {
                log.error( "[{}] close {}", uniqueName, writer.segment.path, e );
            }

            if( writer.segment.records > 0 ) {
                closed.add( writer.segment );
            } else if( segments.remove( writer.segment ) ) {
                bytes.addAndGet( -writer.segment.bytes );
                try {
                    Files.deleteIfExists( writer.segment.path );
                } catch( IOException e ) {
                    log.error( "[{}] cannot delete {}", uniqueName, writer.segment.path, e );
                }
            }
        }

        SpoolSegment.Record next( Consumer<String> event ) {
            readLock.lock();
            try {
                while( true ) {
                    if( reader == null ) {
                        SpoolSegment segment = closed.poll();
                        if( segment == null ) {
                            if( writer == null ) return null;

                            writeLock.lock();
                            try {
                                SpoolSegment.Writer writer = this.writer;
                                if( writer != null && writer.segment.records > 0 ) roll();
                            } finally {
                                writeLock.unlock();
                            }
                            segment = closed.poll();
                            if( segment == null ) return null;
                        }

                        try {
                            reader = segment.reader();
                        } catch( IOException e ) {
                            log.error( "[{}] cannot read {}", uniqueName, segment.path, e );
                            event.accept( "corrupted" );
                            if( segment.readDone() ) delete( segment );
                            continue;
                        }
                    }

                    SpoolSegment.Record record;
                    try {
                        record = reader.next();
                    } catch( IOException e ) {
                        log.error( "[{}] cannot read {}", uniqueName, reader, e );
                        reader.corrupted = true;
                        record = null;
                    }

                    if( record != null ) {
                        record.segment().read();
                        return record;
                    }

                    SpoolSegment segment = readerSegment();
                    if( reader.corrupted ) {
                        log.warn( "[{}] {}: invalid record, the rest of the segment is skipped", uniqueName, segment.path );
                        event.accept( "corrupted" );
                    }
                    closeReader();
                    if( segment.readDone() ) delete( segment );
                }
            } finally {
                readLock.unlock();
            }
        }

        private SpoolSegment readerSegment() {
            return reader.segment();
        }

        private void closeReader() {
            try {
                reader.close();
            } catch( IOException e ) {
                log.warn( "[{}] close: {}", uniqueName, e.getMessage() );
            }
            reader = null;
        }

        void close() {
            writeLock.lock();
            try {
                SpoolSegment.Writer writer = this.writer;
                if( writer != null ) {
                    this.writer = null;
                    try {
                        writer.force();
                        writer.close();
                    } catch( IOException e ) {
                        log.error( "[{}] close {}", uniqueName, writer.segment.path, e );
                    }
                    if( writer.segment.records == 0 && segments.remove( writer.segment ) ) {
                        try {
                            Files.deleteIfExists( writer.segment.path );
                        } catch( IOException e ) {
                            log.warn( "[{}] cannot delete {}: {}", uniqueName, writer.segment.path, e.getMessage() );
                        }
                    }
                }
            } finally {
                writeLock.unlock();
            }

            readLock.lock();
            try {
                if( reader != null ) closeReader();
            } finally {
                readLock.unlock();
            }
        }
    }
}
