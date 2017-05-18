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

import oap.io.Closeables;
import oap.io.Sockets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketConnection implements Connection {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;

    public SocketConnection( String host, int port, long soTimeout ) {
        this.socket = new Socket();
        try {
            this.socket.setKeepAlive( true );
            this.socket.setTcpNoDelay( true );
            this.socket.connect( new InetSocketAddress( host, port ) );
            this.socket.setSoTimeout( ( int ) soTimeout );
            this.out = new DataOutputStream( this.socket.getOutputStream() );
            this.in = new DataInputStream( this.socket.getInputStream() );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public boolean isConnected() {
        return !this.socket.isClosed() && this.socket.isConnected();
    }

    @Override
    public void close() throws IOException {
        Closeables.close( this.out );
        Closeables.close( this.in );
        Sockets.close( this.socket );
    }

    @Override
    public String toString() {
        return this.socket.toString();
    }

    @Override
    public void write( byte[] buffer, int off, int length ) {
        try {
            out.write( buffer, off, length );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public void write( byte b ) {
        try {
            out.write( b );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public int read() {
        try {
            return in.readInt();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
