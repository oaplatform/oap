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
package oap.io;

import oap.util.Strings;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static org.slf4j.LoggerFactory.getLogger;

public class Sockets {
    private static final Logger logger = getLogger( Sockets.class );

    public static String send( String host, int port, String data ) throws IOException {
        try( Socket socket = new Socket( host, port );
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream() ) {
            os.write( Strings.toByteArray( data ) );
            os.flush();
            socket.shutdownOutput();
            return Strings.readString( is );
        }
    }

    public static void close( Socket socket ) {
        if( !socket.isClosed() ) {
            try {
                socket.getInputStream().close();
                socket.shutdownInput();
            } catch( IOException e ) {
                if( !"Socket is closed".equals( e.getMessage() ) )
                    logger.error( e.getMessage(), e );
            }
            try {
                socket.getOutputStream().flush();
                socket.getOutputStream().close();
                socket.shutdownOutput();
            } catch( IOException e ) {
                if( !"Socket is closed".equals( e.getMessage() ) )
                    logger.error( e.getMessage(), e );
            }
            Closeables.close( socket );
        }
    }
}
