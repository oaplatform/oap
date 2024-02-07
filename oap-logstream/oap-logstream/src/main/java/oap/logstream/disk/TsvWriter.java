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
import oap.logstream.InvalidProtocolVersionException;
import oap.logstream.LogId;
import oap.logstream.LogIdTemplate;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.Timestamp;
import oap.template.BinaryInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

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

    public synchronized void write( ProtocolVersion protocolVersion, byte[] buffer, Consumer<String> error ) throws LoggerException {
        write( protocolVersion, buffer, 0, buffer.length, error );
    }

    @Override
    public synchronized void write( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length, Consumer<String> error ) throws LoggerException {
        if( closed ) {
            throw new LoggerException( "writer is already closed!" );
        }

        switch( protocolVersion ) {
            case TSV_V1 -> writeTsvV1( protocolVersion, buffer, offset, length, error );
            case BINARY_V2 -> writeBinaryV2( protocolVersion, buffer, offset, length, error );
            default -> throw new InvalidProtocolVersionException( "tsv", protocolVersion.version );
        }
    }

    private void writeTsvV1( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length, Consumer<String> error ) {
        try {
            refresh();
            var filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.info( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    out = new CountingOutputStream( IoStreams.out( filename, IoStreams.Encoding.from( filename ), bufferSize ) );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    out.write( logId.headers[0].getBytes( UTF_8 ) );
                    out.write( '\n' );
                    log.debug( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.info( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    write( protocolVersion, buffer, offset, length, error );
                    return;
                }
            log.trace( "writing {} bytes to {}", length, this );

            out.write( buffer, offset, length );

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

    private void writeBinaryV2( ProtocolVersion protocolVersion, byte[] buffer, int offset, int length, Consumer<String> error ) {
        try {
            refresh();
            var filename = filename();
            if( out == null )
                if( !java.nio.file.Files.exists( filename ) ) {
                    log.info( "[{}] open new file v{}", filename, fileVersion );
                    outFilename = filename;
                    out = new CountingOutputStream( IoStreams.out( filename, IoStreams.Encoding.from( filename ), bufferSize ) );
                    LogIdTemplate logIdTemplate = new LogIdTemplate( logId );
                    new LogMetadata( logId ).withProperty( "VERSION", logIdTemplate.getHashWithVersion( fileVersion ) ).writeFor( filename );

                    out.write( String.join( "\t", logId.headers ).getBytes( UTF_8 ) );
                    out.write( '\n' );
                    log.debug( "[{}] write headers {}", filename, logId.headers );
                } else {
                    log.info( "[{}] file exists v{}", filename, fileVersion );
                    fileVersion += 1;
                    if( fileVersion > maxVersions ) throw new IllegalStateException( "version > " + maxVersions );
                    write( protocolVersion, buffer, offset, length, error );
                    return;
                }
            log.trace( "writing {} bytes to {}", length, this );

            convertToTsv( buffer, offset, length, line -> out.write( line ) );

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

    private void convertToTsv( byte[] buffer, int offset, int length, IOExceptionConsumer<byte[]> cons ) throws IOException {
        var bis = new BinaryInputStream( new ByteArrayInputStream( buffer, offset, length ) );

        var sb = new StringBuilder();
        TemplateAccumulatorTsv ta = new TemplateAccumulatorTsv( sb, configuration.dateTime32Format );
        Object obj = bis.readObject();
        while( obj != null ) {
            while( obj != null && obj != BinaryInputStream.EOL ) {
                if( !sb.isEmpty() ) sb.append( '\t' );
                ta.accept( obj );
                obj = bis.readObject();
            }
            cons.accept( ta.addEol( obj == BinaryInputStream.EOL ).getBytes() );
            sb.setLength( 0 );
            obj = bis.readObject();
        }
    }

    @FunctionalInterface
    public interface IOExceptionConsumer<T> {
        void accept( T t ) throws IOException;
    }
}
