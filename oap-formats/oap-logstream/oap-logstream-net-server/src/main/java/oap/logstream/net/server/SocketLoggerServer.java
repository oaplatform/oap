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
package oap.logstream.net.server;

import lombok.extern.slf4j.Slf4j;
import oap.io.content.ContentReader;
import oap.logstream.AbstractLoggerBackend;
import oap.logstream.LogStreamProtocol;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.message.server.MessageListener;
import oap.template.BinaryUtils;
import oap.tsv.Tsv;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static oap.logstream.LogStreamProtocol.MESSAGE_TYPE;

@Slf4j
public class SocketLoggerServer implements MessageListener, Closeable {
    private final AbstractLoggerBackend backend;

    public SocketLoggerServer( AbstractLoggerBackend backend ) {
        this.backend = backend;
    }

    @Override
    public byte getId() {
        return MESSAGE_TYPE;
    }

    @Override
    public String getInfo() {
        return "logstream";
    }

    @Override
    public short run( int protocolVersion, String hostName, int size, byte[] data, String md5 ) {
        if( !backend.isLoggingAvailable() ) {
            return LogStreamProtocol.STATUS_BACKEND_LOGGER_NOT_AVAILABLE;
        }
        try( DataInputStream in = new DataInputStream( new ByteArrayInputStream( data ) ) ) {
            switch( protocolVersion ) {
                case 1, 2, 3 -> readBinaryV3( ProtocolVersion.valueOf( protocolVersion ), hostName, in );
                default -> {
                    return LogStreamProtocol.INVALID_VERSION;
                }
            }
        } catch( EOFException e ) {
            log.debug( "[" + hostName + "] " + " ended, closed" );
            throw new LoggerException( e );
        } catch( LoggerException e ) {
            log.error( "[" + hostName + "] ", e );
            throw e;
        } catch( Exception e ) {
            log.error( "[" + hostName + "] ", e );
            throw new LoggerException( e );
        }

        return LogStreamProtocol.STATUS_OK;
    }

    private void readBinaryV3( ProtocolVersion version, String hostName, DataInputStream in ) throws IOException {
        in.readLong(); // digestion control
        int length = in.readInt();
        String filePreffix = in.readUTF();
        String logType = in.readUTF();
        String clientHostname = in.readUTF();

        int headersSize = in.readInt();
        String[] headers = new String[headersSize];
        for( int i = 0; i < headersSize; i++ ) {
            headers[i] = in.readUTF();
        }

        byte[][] types = new byte[headersSize][];
        for( int x = 0; x < headersSize; x++ ) {
            byte tSize = in.readByte();
            byte[] t = new byte[tSize];
            for( int y = 0; y < tSize; y++ ) {
                t[y] = in.readByte();
            }
            types[x] = t;
        }

        byte propertiesSize = in.readByte();
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        for( int i = 0; i < propertiesSize; i++ ) {
            properties.put( in.readUTF(), in.readUTF() );
        }

        byte[] buffer = new byte[length];
        in.readFully( buffer, 0, length );

        if( log.isTraceEnabled() ) {
            List<List<Object>> lines = new ArrayList<>();
            switch( version ) {
                case TSV_V1 -> ContentReader.read( buffer, Tsv.tsv.ofSeparatedValues() ).toList()
                    .forEach( line -> lines.add( Collections.singletonList( line ) ) );
                case BINARY_V2 -> lines.addAll( BinaryUtils.read( buffer ) );
                case ROW_BINARY_V3 -> lines.addAll( RowBinaryUtils.read( buffer, headers, types ) );
            }

            lines.forEach( line ->
                log.trace( "[{}] logging (properties {} filePreffix {} logType {} headers {} types {}, length {}, line {})",
                    hostName, properties, filePreffix, logType, headers, types, length, line
                )
            );
        }

        backend.log( version, clientHostname, filePreffix, properties, logType, headers, types, buffer, 0, length );
    }

    @Override
    public void close() {
//        @ToDo consider closing backend
//        backend.close();
    }
}
