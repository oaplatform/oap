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

package oap.logstream.net;

import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.LoggingBackend;
import org.slf4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class SocketLoggingBackend implements LoggingBackend {
    private static final Logger logger = getLogger( SocketLoggingBackend.class );

    private final String host;
    private final int port;
    private DataSocket socket;
    private Buffers buffers;
    private long syncInterval = 10000;
    private final Scheduled scheduled;

    public SocketLoggingBackend( String host, int port, Path location, int bufferSize ) {
        this.host = host;
        this.port = port;
        this.buffers = new Buffers( location, bufferSize );
        this.scheduled = Scheduler.scheduleWithFixedDelay( syncInterval, TimeUnit.MILLISECONDS, this::sync );
    }

    public void sync() {
        connect();
        buffers.flush();
        buffers.forEachReadyData( ( selector, data ) -> {
            try {
                logger.debug( "syncing " + data.length + " bytes to " + selector );
                DataOutputStream out = socket.getOutputStream();
                out.writeUTF( selector );
                out.writeInt( data.length );
                out.write( data );
                int written = socket.getInputStream().readInt();
                if( written == data.length ) return true;
                else {
                    logger.error( "checksum failed: " + written + ":" + data.length );
                    Closeables.close( socket );
                    return false;
                }
            } catch( IOException e ) {
                logger.warn( e.getMessage() );
                logger.warn( "closing " + socket );
                Closeables.close( socket );
                return false;
            }
        } );

    }

    @Override
    public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {
        buffers.put( fileName, buffer, offset, length );
    }

    private void connect() {
        if( this.socket == null || !socket.isConnected() ) {
            Closeables.close( socket );
            this.socket = new DataSocket( host, port );
        }
    }

    @Override
    public void close() {
        Scheduled.cancel( scheduled );
        Closeables.close( socket );
        Closeables.close( buffers );
    }

}
