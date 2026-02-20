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

import com.google.common.io.CountingOutputStream;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.logstream.formats.rowbinary.RowBinaryInputStream;
import oap.template.BinaryInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class TsvWriter extends AbstractWriter<CountingOutputStream> {
    private final WriterConfiguration.TsvConfiguration configuration;

    public TsvWriter( Path logDirectory, String filePattern, LogId logId,
                      WriterConfiguration.TsvConfiguration configuration,
                      int bufferSize, Timestamp timestamp,
                      int maxVersions ) {
        super( LogFormat.TSV_GZ, logDirectory, filePattern, logId, bufferSize, timestamp, maxVersions );

        this.configuration = configuration;
    }

    @Override
    public String write( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) throws LoggerException {
        lock.lock();
        try {
            if( closed ) {
                throw new LoggerException( "writer is already closed!" );
            }

            return switch( protocolVersion ) {
                case TSV_V1 -> writeTsvV1( protocolVersion, buffer, offset, length );
                case BINARY_V2 -> writeBinaryV2( protocolVersion, buffer, offset, length );
                case ROW_BINARY_V3 -> writeBinaryV3( protocolVersion, buffer, offset, length );
            };
        } finally {
            lock.unlock();
        }
    }

    private String writeTsvV1( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        try {
            refresh();
            Path filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.debug( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    out = new CountingOutputStream( IoStreams.out( filename, IoStreams.Encoding.from( filename ), bufferSize ) );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    out.write( logId.headers[0].getBytes( UTF_8 ) );
                    out.write( '\n' );
                    log.trace( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.debug( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    return write( protocolVersion, buffer, offset, length );
                }
            log.trace( "writing {} bytes to {}", length, this );

            out.write( buffer, offset, length );

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

    private String writeBinaryV2( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        try {
            refresh();
            Path filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.debug( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    out = new CountingOutputStream( IoStreams.out( filename, IoStreams.Encoding.from( filename ), bufferSize ) );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    out.write( String.join( "\t", logId.headers ).getBytes( UTF_8 ) );
                    out.write( '\n' );
                    log.trace( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.debug( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    return write( protocolVersion, buffer, offset, length );
                }
            log.trace( "writing {} bytes to {}", length, this );

            convertToTsvV2( buffer, offset, length, line -> out.write( line ) );

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

    private String writeBinaryV3( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length ) {
        try {
            refresh();
            Path filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.debug( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    out = new CountingOutputStream( IoStreams.out( filename, IoStreams.Encoding.from( filename ), bufferSize ) );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    out.write( String.join( "\t", logId.headers ).getBytes( UTF_8 ) );
                    out.write( '\n' );
                    log.trace( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.debug( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    return write( protocolVersion, buffer, offset, length );
                }
            log.trace( "writing {} bytes to {}", length, this );

            convertToTsvV3( buffer, offset, length, line -> out.write( line ), logId.headers, logId.types );

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

    private void convertToTsvV2( byte[] buffer, int offset, int length, IOExceptionConsumer<byte[]> cons ) throws IOException {
        BinaryInputStream bis = new BinaryInputStream( new ByteArrayInputStream( buffer, offset, length ) );

        StringBuilder sb = new StringBuilder();
        TemplateAccumulatorTsv ta = new TemplateAccumulatorTsv( sb, configuration.dateTime32Format );
        Object obj = bis.readObject();
        while( obj != null ) {
            boolean first = true;
            while( obj != null && obj != BinaryInputStream.EOL ) {
                if( !first ) {
                    sb.append( '\t' );
                } else {
                    first = false;
                }
                ta.accept( obj );
                obj = bis.readObject();
            }
            cons.accept( ta.addEol( obj == BinaryInputStream.EOL ).getBytes() );
            sb.setLength( 0 );
            obj = bis.readObject();
        }
    }

    private void convertToTsvV3( byte[] buffer, int offset, int length, IOExceptionConsumer<byte[]> cons, String[] headers, byte[][] types ) throws IOException {
        RowBinaryInputStream bis = new RowBinaryInputStream( new ByteArrayInputStream( buffer, offset, length ), headers, types );

        StringBuilder sb = new StringBuilder();
        TemplateAccumulatorTsv ta = new TemplateAccumulatorTsv( sb, configuration.dateTime32Format );

        List<Object> row = bis.readRow();
        while( row != null ) {
            boolean first = true;
            for( Object item : row ) {
                if( !first ) {
                    sb.append( '\t' );
                } else {
                    first = false;
                }
                ta.accept( item );
            }
            cons.accept( ta.addEol( true ).getBytes() );
            sb.setLength( 0 );
            row = bis.readRow();
        }
    }

    @FunctionalInterface
    public interface IOExceptionConsumer<T> {
        void accept( T t ) throws IOException;
    }
}
