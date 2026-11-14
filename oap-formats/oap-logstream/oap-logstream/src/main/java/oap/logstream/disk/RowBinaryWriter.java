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
        super( templateEngine, compression.logFormat, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification );
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

    @Override
    public String write( LogStreamProtocol.ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        boolean sync = syncMode == SyncMode.ALWAYS;

        lock.lock();
        try {
            refresh();

            ByteBuffer payload = ByteBuffer.wrap( buffer, offset, length );

            if( logFile == null ) {
                Path filename = openNewFile();

                log.trace( "[{}] write headers {} and {} bytes", filename, logId.headers, length );
                try {
                    logFile.append( sync, ByteBuffer.wrap( header ), payload );
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

                return filename.toString();
            }

            log.trace( "writing {} bytes to {}", length, this );

            try {
                logFile.append( sync, payload );
            } catch( LoggerException e ) {
                // the file is truncated to the last complete frame: close it, the next write goes to the next version
                log.error( "[{}] {}", logFile.outFilename, e.getMessage(), e );
                try {
                    closeOutput();
                } catch( Exception closeError ) {
                    e.addSuppressed( closeError );
                } finally {
                    logFile = null;
                }
                throw e;
            }

            return logFile.outFilename.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * creates a file for the first free version: a version is taken if any of its files (data, metadata, completed, legacy transaction) exists
     */
    private Path openNewFile() {
        while( true ) {
            Path filename = filename();
            LogFile checkLogFile = new LogFile( filename );
            if( !checkLogFile.exists() ) {
                log.debug( "[{}] open new file v{}", filename, fileVersion );
                String version = new LogIdTemplate( logId ).getHashWithVersion( fileVersion, Inet.hostname() );
                logFile = checkLogFile.create( logId, Map.of( "VERSION", version ) );
                return filename;
            }

            log.debug( "[{}] file exists v{}", filename, fileVersion );
            fileVersion += 1;
            if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
            refresh();
        }
    }
}
