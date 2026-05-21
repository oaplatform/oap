package oap.logstream.disk;

import lombok.extern.slf4j.Slf4j;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
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
import java.util.zip.GZIPOutputStream;

@Slf4j
public class RowBinaryWriter extends AbstractWriter {
    public RowBinaryWriter( TemplateEngine templateEngine, Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp, int maxVersions, String hostname ) {
        super( templateEngine, LogFormat.ROW_BINARY_GZ, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions, hostname );
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
                    GZIPOutputStream gzip = new GZIPOutputStream( outputStream );
                    RowBinaryOutputStream rbOut = new RowBinaryOutputStream( gzip, List.of( logId.headers ), logId.types );
                    rbOut.close();

                    logFile.writeAndCommitTransaction( outputStream.array, 0, outputStream.length );
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
