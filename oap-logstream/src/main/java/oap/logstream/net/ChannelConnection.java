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

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.io.Closeables;
import org.apache.commons.lang3.NotImplementedException;
import org.joda.time.DateTimeUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Slf4j
public class ChannelConnection implements Connection {

    private final long timeout;
    private SocketChannel channel;

    public ChannelConnection( String host, int port, long timeout ) {
        this.timeout = timeout;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking( false );
            channel.connect( new InetSocketAddress( host, port ) );
            if( !channel.finishConnect() ) throw new ConnectException( "cannot connect to " + host + ":" + port );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public void close() {
        Closeables.close( channel );
    }

    @Override
    public void write( byte[] buffer, int off, int length ) {
        try {
            ByteBuffer b = ByteBuffer.wrap( buffer, off, length );
            long start = DateTimeUtils.currentTimeMillis();
            while( b.hasRemaining() ) {
                if( channel.write( b ) == 0 ) {
                    if( DateTimeUtils.currentTimeMillis() - start > timeout )
                        throw new SocketTimeoutException( "unable to write for " + timeout + "ms" );
                    Threads.sleepSafely( 10 );
                } else start = DateTimeUtils.currentTimeMillis();
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public void write( byte b ) {
        write( new byte[] { b }, 0, 1 );
    }

    @Override
    public int read() {
        throw new NotImplementedException( "read" );
    }

    @Override
    public String toString() {
        return this.channel.toString();
    }


}
