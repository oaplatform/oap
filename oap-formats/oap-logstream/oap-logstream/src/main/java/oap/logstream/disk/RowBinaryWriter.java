package oap.logstream.disk;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamProtocol;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.logstream.formats.rowbinary.RowBinaryOutputStream;
import oap.util.FastByteArrayOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class RowBinaryWriter extends AbstractWriter<FileChannel> {
    protected RowBinaryWriter( Path logDirectory, String filePattern, LogId logId, int bufferSize, Timestamp timestamp, int maxVersions ) {
        super( LogFormat.ROW_BINARY_GZ, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions );
    }

    @Override
    public String write( LogStreamProtocol.ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        try {
            refresh();
            Path filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.debug( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    Files.ensureDirectory( filename.getParent() );
                    out = FileChannel.open( filename, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream( outputStream );
                    RowBinaryOutputStream rbOut = new RowBinaryOutputStream( gzip, List.of( logId.headers ) );
                    rbOut.close();

                    ByteBuffer byteBuffer = ByteBuffer.wrap( outputStream.array, 0, outputStream.length );
                    do {
                        out.write( byteBuffer );
                    } while( byteBuffer.hasRemaining() );

                    log.trace( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.debug( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    return write( protocolVersion, buffer, offset, length );
                }
            log.trace( "writing {} bytes to {}", length, this );

            ByteBuffer byteBuffer = ByteBuffer.wrap( buffer, offset, length );
            do {
                out.write( byteBuffer );
            } while( byteBuffer.hasRemaining() );
            out.force( true );

            return filename.toString();

        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            try {
                closeOutput();
            } finally {
                outFilename = null;
                out = null;
            }
            throw new LoggerException( e );
        }
    }
}
