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

package oap.logstream.disk;

import oap.compression.Compression;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.Logger;
import oap.logstream.Timestamp;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.TemplateEngineFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Result;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.logstream.formats.RowBinaryAssertion.assertRowBinaryFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DiskLoggerBackendTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;
    private final TemplateEngineFixture templateEngineFixture;

    public DiskLoggerBackendTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        templateEngineFixture = fixture( new TemplateEngineFixture() );
    }

    @Test
    public void spaceAvailable() {
        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), Timestamp.BPH_12, 4000, "localhost" ) ) ) {
            backend.start();

            assertTrue( backend.isLoggingAvailable() );
            backend.requiredFreeSpace *= 1000;
            assertFalse( backend.isLoggingAvailable() );
            backend.requiredFreeSpace /= 1000;
            assertTrue( backend.isLoggingAvailable() );
        }
    }

    @Test
    public void testPatternByType() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 16 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );

        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), Timestamp.BPH_12, 4000, "localhost" ) ) ) {
            backend.filePattern = "{{ PREFIX }}/{{ LOG_TYPE }}_{{ LOG_VERSION }}_{{ INTERVAL }}.tsv.gz";
            backend.filePatternByType.put( "LOG_TYPE_WITH_DIFFERENT_FILE_PATTERN",
                new DiskLoggerBackend.FilePatternConfiguration( "{{ PREFIX }}/{{ LOG_TYPE }}_{{ LOG_VERSION }}_{{ MINUTE }}.parquet" ) );
            backend.start();

            Logger logger = new Logger( backend );
            //log a line to lfn1
            logger.log( Map.of( "PREFIX", "lfn1" ), "log_type_with_default_file_pattern", headers, types, lines );
            logger.log( Map.of( "PREFIX", "lfn1" ), "log_type_with_different_file_pattern", headers, types, lines );

            backend.refresh( true );

            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/log_type_with_default_file_pattern_59193f7e0-1_03.tsv.gz.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/log_type_with_different_file_pattern_59193f7e0-1_16.parquet.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
        }
    }

    @Test
    public void testWriteSync() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );
        //init new logger
        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            backend.start();

            Logger logger = new Logger( backend );
            //log a line to lfn1
            Result<Void, LogstreamException> result = logger.log( Map.of( "PREFIX", "lfn1" ), "log", headers, types, lines );

            assertThat( result.isSuccess() ).isTrue();
            assertThat( result.successValue ).isNull();
            assertThat( backend.getLogFile( new LogId( "log", Map.of( "PREFIX", "lfn1" ), headers, types ) ).outFilename )
                .isEqualTo( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz.rb.gz" ) );
            assertThat( backend.getLogFile( new LogId( "log", Map.of( "PREFIX", "unknown" ), headers, types ) ) ).isNull();

            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
        }
    }

    /**
     * the state after a crash: the file is written (and synced), .metadata.completed is missing, a torn buffer at the end
     *
     * @return the data file and its size before the torn buffer
     */
    private Path crashedFile( byte[] lines, byte[] torn, String[] headers, byte[][] types ) throws IOException {
        Path file;
        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            Logger logger = new Logger( backend );
            logger.log( Map.of( "PREFIX", "lfn1" ), "log", headers, types, lines );
            file = backend.getLogFile( new LogId( "log", Map.of( "PREFIX", "lfn1" ), headers, types ) ).outFilename;
        }

        Files.delete( new LogFile( file ).getCompletedFile() );
        Files.write( file, torn, StandardOpenOption.APPEND );
        return file;
    }

    @Test
    public void recoveryOnStart() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );

        Path file = crashedFile( lines, java.util.Arrays.copyOf( lines, lines.length / 2 ), headers, types );
        long sizeBeforeTorn = Files.size( file ) - lines.length / 2;

        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            CopyOnWriteArrayList<Path> closed = new CopyOnWriteArrayList<>();
            backend.addNotificationListener( closed::add );
            backend.start();

            assertThat( closed ).containsExactly( file );
            assertThat( Files.size( file ) ).isEqualTo( sizeBeforeTorn );
            assertThat( new LogFile( file ).isCompleted() ).isTrue();
            assertRowBinaryFile( file, GZIP ).containsExactlyInAnyOrderEntriesOf(
                List.of( "12345678", "rrrr5678" ),
                List.of( "1", "2" )
            );

            Logger logger = new Logger( backend );
            logger.log( Map.of( "PREFIX", "lfn1" ), "log", headers, types, lines );
            Path next = backend.getLogFile( new LogId( "log", Map.of( "PREFIX", "lfn1" ), headers, types ) ).outFilename;
            assertThat( next ).isNotEqualTo( file );
            assertThat( next.getFileName().toString() ).contains( "-2-2015" );
        }
    }

    /**
     * a file of the previous version (with .metadata.transaction) left by a crash: truncated to the committed offset,
     * completed; the new data goes to the next version
     */
    @Test
    @SuppressWarnings( "deprecation" )
    public void recoveryOfLegacyFileOnStart() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );
        byte[] uncommitted = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "not", "committed" ) ) ) );

        // a complete frame after the committed offset: written, but not committed
        Path file = crashedFile( lines, uncommitted, headers, types );
        long committed = Files.size( file ) - uncommitted.length;
        LogFile logFile = new LogFile( file );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( committed ) );

        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            backend.start();

            assertThat( Files.size( file ) ).isEqualTo( committed );
            assertThat( logFile.isCompleted() ).isTrue();
            assertThat( logFile.getTransactionFile() ).exists();
            assertRowBinaryFile( file, GZIP ).containsExactlyInAnyOrderEntriesOf(
                List.of( "12345678", "rrrr5678" ),
                List.of( "1", "2" )
            );

            Logger logger = new Logger( backend );
            logger.log( Map.of( "PREFIX", "lfn1" ), "log", headers, types, lines );
            Path next = backend.getLogFile( new LogId( "log", Map.of( "PREFIX", "lfn1" ), headers, types ) ).outFilename;
            assertThat( next ).isNotEqualTo( file );
            assertThat( Files.size( file ) ).isEqualTo( committed );
        }
    }

    @Test
    public void periodicSync() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "1" ) ) ) );

        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            backend.syncMode = SyncMode.PERIODIC;
            backend.syncInterval = Dates.h( 1 );
            backend.start();

            new Logger( backend ).log( Map.of( "PREFIX", "lfn1" ), "log", headers, types, lines );

            LogFile logFile = backend.writers.asMap().values().iterator().next().getLogFile();
            assertThat( logFile.hasUnsyncedData() ).isTrue();

            backend.sync();
            assertThat( logFile.hasUnsyncedData() ).isFalse();
        }
    }

    private static final String[] SHARD_HEADERS = new String[] { "REQUEST_ID", "REQUEST_ID2" };
    private static final byte[][] SHARD_TYPES = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };

    private static void log( DiskLoggerBackend backend, String id ) {
        byte[] lines;
        try {
            lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( id, id ) ) ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        Result<Void, LogstreamException> result = new Logger( backend ).log( Map.of( "PREFIX", "lfn1" ), "log", SHARD_HEADERS, SHARD_TYPES, lines );
        assertThat( result.isSuccess() ).isTrue();
    }

    private DiskLoggerBackend shardedBackend( int shrinkWindows ) {
        DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) );
        backend.maxShards = 4;
        // any wait grows the shards
        backend.shardGrowWait = 0;
        backend.shardShrinkWindows = shrinkWindows;
        // the windows are ended by the test only
        backend.refreshInitDelay = Dates.h( 1 );
        return backend;
    }

    /**
     * logs from another thread while shard 0 is held: with one active shard the log waits for it, then the window ends
     */
    private static void growTo2( DiskLoggerBackend backend, WriterShards shards, String id ) throws Exception {
        assertThat( shards.activeShards() ).isEqualTo( 1 );
        ReentrantLock lock = shards.shardWriter( 0 ).lock;
        try( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor() ) {
            Future<?> future;
            lock.lock();
            try {
                future = executor.submit( () -> log( backend, id ) );
                long deadline = System.currentTimeMillis() + 10_000;
                while( !lock.hasQueuedThreads() ) {
                    assertThat( System.currentTimeMillis() ).isLessThan( deadline );
                    Thread.sleep( 1 );
                }
                Thread.sleep( 5 );
            } finally {
                lock.unlock();
            }
            future.get( 10, TimeUnit.SECONDS );
        }
        shards.adjust();
        assertThat( shards.activeShards() ).isEqualTo( 2 );
    }

    /**
     * logs from another thread while shard 0 is held: with two active shards the log goes to shard 1 without a wait
     */
    private static void logToShard1( DiskLoggerBackend backend, WriterShards shards, String id ) throws Exception {
        ReentrantLock lock = shards.shardWriter( 0 ).lock;
        lock.lock();
        try( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor() ) {
            executor.submit( () -> log( backend, id ) ).get( 10, TimeUnit.SECONDS );
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void shardsGrow() throws Exception {
        Dates.setTimeFixed( 2015, 10, 10, 1 );

        try( DiskLoggerBackend backend = shardedBackend( 30 ) ) {
            backend.start();

            log( backend, "1" );
            WriterShards shards = backend.writers.asMap().values().iterator().next();
            growTo2( backend, shards, "2" );
            logToShard1( backend, shards, "3" );

            Path file0 = shards.shardWriter( 0 ).currentFile();
            Path file1 = shards.shardWriter( 1 ).currentFile();
            assertThat( file0 ).isEqualTo( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz.rb.gz" ) );
            assertThat( file1 ).isEqualTo( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz-s1.rb.gz" ) );
            assertThat( new LogFile( file0 ).getLogMetadata().getString( "SHARD" ) ).isNull();
            assertThat( new LogFile( file1 ).getLogMetadata().getString( "SHARD" ) ).isEqualTo( "1" );
            assertThat( backend.getLogFiles( new LogId( "log", Map.of( "PREFIX", "lfn1" ), SHARD_HEADERS, SHARD_TYPES ), LogStreamCompression.GZIP ) )
                .extracting( f -> f.outFilename )
                .containsExactly( file0, file1 );

            backend.refresh( true );
            // one window without waits: the shards stay
            assertThat( shards.activeShards() ).isEqualTo( 2 );

            assertThat( new LogFile( file0 ).isCompleted() ).isTrue();
            assertThat( new LogFile( file1 ).isCompleted() ).isTrue();
            assertRowBinaryFile( file0, GZIP ).containsExactlyInAnyOrderEntriesOf( List.of( "1", "1" ), List.of( "2", "2" ) );
            assertRowBinaryFile( file1, GZIP ).containsExactlyInAnyOrderEntriesOf( List.of( "3", "3" ) );
        }
    }

    @Test
    public void shardsShrink() throws Exception {
        Dates.setTimeFixed( 2015, 10, 10, 1 );

        try( DiskLoggerBackend backend = shardedBackend( 2 ) ) {
            backend.start();

            log( backend, "1" );
            WriterShards shards = backend.writers.asMap().values().iterator().next();
            growTo2( backend, shards, "2" );

            // a window without appends changes nothing
            shards.adjust();
            assertThat( shards.activeShards() ).isEqualTo( 2 );

            // quiet windows: never more than one shard held at once
            log( backend, "3" );
            shards.adjust();
            assertThat( shards.activeShards() ).isEqualTo( 2 );
            log( backend, "4" );
            shards.adjust();
            assertThat( shards.activeShards() ).isEqualTo( 1 );

            // shard 0 only
            long shard1Before = written( shards, 1 );
            long shard0Before = written( shards, 0 );
            log( backend, "5" );
            log( backend, "6" );
            assertThat( written( shards, 1 ) ).isEqualTo( shard1Before );
            assertThat( written( shards, 0 ) ).isGreaterThan( shard0Before );
        }
    }

    private static long written( WriterShards shards, int shard ) {
        RowBinaryWriter writer = shards.shardWriter( shard );
        LogFile logFile = writer != null ? writer.getLogFile() : null;
        return logFile != null ? logFile.written() : 0;
    }

    @Test
    public void recoverySkipsOpenShardFiles() throws Exception {
        Dates.setTimeFixed( 2015, 10, 10, 1 );

        try( DiskLoggerBackend backend = shardedBackend( 30 ) ) {
            backend.start();

            log( backend, "1" );
            WriterShards shards = backend.writers.asMap().values().iterator().next();
            growTo2( backend, shards, "2" );
            logToShard1( backend, shards, "3" );

            Path file0 = shards.shardWriter( 0 ).currentFile();
            Path file1 = shards.shardWriter( 1 ).currentFile();
            assertThat( shards.currentFiles() ).containsExactly( file0, file1 );

            backend.recover();

            assertThat( new LogFile( file0 ).isCompleted() ).isFalse();
            assertThat( new LogFile( file1 ).isCompleted() ).isFalse();
            assertThat( shards.currentFiles() ).containsExactly( file0, file1 );

            logToShard1( backend, shards, "4" );
            backend.refresh( true );
            assertRowBinaryFile( file1, GZIP ).containsExactlyInAnyOrderEntriesOf( List.of( "3", "3" ), List.of( "4", "4" ) );
        }
    }

    @Test
    public void invalidShardSettingsAreRejected() {
        try( DiskLoggerBackend backend = prefixed( new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) ) {
            backend.maxShards = 1000;
            assertThatThrownBy( backend::start ).isInstanceOf( IllegalArgumentException.class );
            backend.maxShards = 0;
            assertThatThrownBy( backend::start ).isInstanceOf( IllegalArgumentException.class );
        }
    }

    /**
     * the file prefix is a property of the log id: {{ PREFIX }}
     */
    private static DiskLoggerBackend prefixed( DiskLoggerBackend backend ) {
        backend.filePattern = "{{ PREFIX }}/" + backend.filePattern;
        return backend;
    }
}
