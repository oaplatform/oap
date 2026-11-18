package oap.logstream.disk;

import lombok.extern.slf4j.Slf4j;
import oap.compression.Compression;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamCompression;
import oap.logstream.LogStreamProtocol;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.logstream.formats.rowbinary.RowBinaryOutputStream;
import oap.net.Inet;
import oap.template.TemplateEngine;
import oap.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public class RowBinaryWriter extends AbstractWriter {
    public final LogStreamCompression compression;
    public final SyncMode syncMode;
    /**
     * RowBinary header (column names and types) compressed with the file codec; depends only on (LogId, compression)
     */
    private final byte[] header;

    public RowBinaryWriter( TemplateEngine templateEngine, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification ) {
        this( templateEngine, LogStreamCompression.GZIP, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification );
    }

    /**
     * @param compression codec of the incoming payload; the payload is appended to the file as is
     */
    public RowBinaryWriter( TemplateEngine templateEngine, LogStreamCompression compression, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification ) {
        this( templateEngine, compression, SyncMode.ALWAYS, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification );
    }

    public RowBinaryWriter( TemplateEngine templateEngine, LogStreamCompression compression, SyncMode syncMode, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification ) {
        this( templateEngine, compression, syncMode, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification, 0 );
    }

    /**
     * @param shard shard index of the (LogId, codec) key: k &gt; 0 writes {@code <pattern>-s<k>.<codec extension>} with
     *              the metadata property {@code SHARD=k}
     */
    public RowBinaryWriter( TemplateEngine templateEngine, LogStreamCompression compression, SyncMode syncMode, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification, int shard ) {
        super( templateEngine, compression.logFormat, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification, shard );
        this.compression = compression;
        this.syncMode = syncMode;
        this.header = compressedHeader( logId, compression );
    }

    private static byte[] compressedHeader( LogId logId, LogStreamCompression compression ) throws LoggerException {
        try {
            FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
            RowBinaryOutputStream rbOut = new RowBinaryOutputStream( outputStream, List.of( logId.headers ), logId.types );
            rbOut.close();

            return switch( compression ) {
                case GZIP -> Compression.gzip( outputStream.array, 0, outputStream.length );
                case LZ4 -> Compression.lz4( outputStream.array, 0, outputStream.length );
                case ZSTD -> Compression.zstd( outputStream.array, 0, outputStream.length, LogStreamCompression.ZSTD.defaultLevel );
            };
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    /**
     * Appends the buffer to the file under the writer lock; with {@link SyncMode#ALWAYS} commits it outside the lock,
     * so the messages written meanwhile share one force (group commit).
     */
    @Override
    public String write( LogStreamProtocol.ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        Pending pending;
        lock.lock();
        try {
            pending = append( protocolVersion, buffer, offset, length );
        } finally {
            lock.unlock();
        }
        if( syncMode == SyncMode.ALWAYS ) commit( pending );
        return pending.filename();
    }

    /**
     * Writes the buffer (page cache only), does not commit it. Must be called under {@link #lock}.
     */
    Pending append( LogStreamProtocol.ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        refresh();

        ByteBuffer payload = ByteBuffer.wrap( buffer, offset, length );

        if( logFile == null ) {
            Path filename = openNewFile();

            log.trace( "[{}] write headers {} and {} bytes", filename, logId.headers, length );
            try {
                return new Pending( logFile, logFile.write( ByteBuffer.wrap( header ), payload ), filename.toString() );
            } catch( LoggerException e ) {
                // nothing has been written: free the version
                log.error( "[{}] {}", filename, e.getMessage(), e );
                try {
                    logFile.close();
                } catch( Exception closeError ) {
                    e.addSuppressed( closeError );
                } finally {
                    logFile.delete();
                    logFile = null;
                }
                throw e;
            }
        }

        log.trace( "writing {} bytes to {}", length, this );

        try {
            return new Pending( logFile, logFile.write( payload ), logFile.outFilename.toString() );
        } catch( LoggerException e ) {
            log.error( "[{}] {}", logFile.outFilename, e.getMessage(), e );
            if( syncMode == SyncMode.ALWAYS && logFile.isFailed() ) {
                // a concurrent commit failed: drop the bytes nobody got an ACK for
                abortOutput( e );
            } else {
                // the file is truncated to the last complete frame: close it, the next write goes to the next version
                try {
                    closeOutput();
                } catch( Exception closeError ) {
                    e.addSuppressed( closeError );
                } finally {
                    logFile = null;
                }
            }
            throw e;
        }
    }

    /**
     * Commits the pending write outside the lock. On failure the file (if it is still the current one) is truncated
     * to the committed data, closed and the next write goes to the next version.
     */
    void commit( Pending pending ) throws LoggerException {
        try {
            pending.file().commit( pending.end() );
        } catch( LoggerException e ) {
            lock.lock();
            try {
                if( logFile == pending.file() ) {
                    log.error( "[{}] {}", logFile.outFilename, e.getMessage(), e );
                    abortOutput( e );
                }
            } finally {
                lock.unlock();
            }
            throw e;
        }
    }

    /**
     * under lock, after a failed commit: the bytes after committed belong to the messages that get an error
     */
    private void abortOutput( LoggerException cause ) {
        try {
            logFile.truncateToCommitted();
            closeOutput();
        } catch( Exception e ) {
            cause.addSuppressed( e );
            // not truncated: closed without .completed, recover() repairs it on the next start
            if( logFile != null ) failOutput( cause );
        }
    }

    /**
     * a test hook
     */
    protected LogFile newLogFile( Path filename ) {
        return new LogFile( filename );
    }

    /**
     * creates a file for the first free version: a version is taken if any of its files (data, metadata, completed, legacy transaction) exists
     */
    private Path openNewFile() {
        while( true ) {
            Path filename = filename();
            LogFile checkLogFile = newLogFile( filename );
            if( !checkLogFile.exists() ) {
                log.debug( "[{}] open new file v{}", filename, fileVersion );
                String version = new LogIdTemplate( logId ).getHashWithVersion( fileVersion, Inet.hostname() );
                logFile = checkLogFile.create( logId, shard == 0
                    ? Map.of( "VERSION", version )
                    : Map.of( "VERSION", version, "SHARD", String.valueOf( shard ) ) );
                return filename;
            }

            log.debug( "[{}] file exists v{}", filename, fileVersion );
            fileVersion += 1;
            if( fileVersion > maxVersions ) throw new IllegalStateException( s( "version > ${maxVersions}" ) );
            refresh();
        }
    }

    /**
     * a write not committed yet
     *
     * @param end the end of its bytes in the file
     */
    record Pending( LogFile file, long end, String filename ) {}
}
