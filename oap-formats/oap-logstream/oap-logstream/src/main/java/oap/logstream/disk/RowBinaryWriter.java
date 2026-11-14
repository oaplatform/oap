package oap.logstream.disk;

import lombok.extern.slf4j.Slf4j;
import oap.logstream.LogId;
import oap.compression.Compression;
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
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class RowBinaryWriter extends AbstractWriter {
    public final LogStreamCompression compression;

    public RowBinaryWriter( TemplateEngine templateEngine, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification ) {
        this( templateEngine, LogStreamCompression.GZIP, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification );
    }

    /**
     * @param compression codec of the incoming payload; the payload is appended to the file as is
     */
    public RowBinaryWriter( TemplateEngine templateEngine, LogStreamCompression compression, Path logDirectory, String filePattern, LogId logId,
                            int bufferSize, Timestamp timestamp, int maxVersions, String hostname, FileWriterNotification notification ) {
        super( templateEngine, compression.logFormat, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname, notification );
        this.compression = compression;
    }

    private byte[] compressHeader( byte[] header, int length ) throws IOException {
        return switch( compression ) {
            case GZIP -> Compression.gzip( header, 0, length );
            case LZ4 -> Compression.lz4( header, 0, length );
            case ZSTD -> Compression.zstd( header, 0, length, LogStreamCompression.ZSTD.defaultLevel );
        };
    }

    @Override
    public String write( LogStreamProtocol.ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        lock.lock();
        try {
            refresh();
            Path filename = filename();
            if( logFile == null ) {
                LogFile checkLogFile = new LogFile( filename );
                if( !checkLogFile.existsAndValid() && !checkLogFile.isCompleted() ) {
                    log.debug( "[{}] open new file v{}", filename, fileVersion );
                    logFile = checkLogFile.create( logId );

                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    logFile.addProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion, Inet.hostname() ) );

                    log.trace( "[{}] write headers {}", filename, logId.headers );

                    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
                    RowBinaryOutputStream rbOut = new RowBinaryOutputStream( outputStream, List.of( logId.headers ), logId.types );
                    rbOut.close();

                    byte[] header = compressHeader( outputStream.array, outputStream.length );
                    logFile.writeAndCommitTransaction( header, 0, header.length );
                } else {
                    log.debug( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    return write( protocolVersion, buffer, offset, length );
                }
            }

            log.trace( "writing {} bytes to {}", length, this );

            logFile.beginTransactionWriteAndCommitTransaction( buffer, offset, length );

            return filename.toString();

        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            try {
                closeOutput();
            } finally {
                logFile = null;
            }
            throw new LoggerException( e );
        } finally {
            lock.unlock();
        }
    }
}
