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

package oap.http.server.apache;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.HttpServer;
import oap.io.IoStreams;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.KeyStore;

import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class SslHttpListener extends AbstractHttpListener {
    protected final Path keystoreLocation;
    protected final String keystorePassword;
    protected final int port;

    public SslHttpListener( HttpServer server, Path keystoreLocation, String keystorePassword, int port ) {
        super( server );
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.port = port;
    }

    public void start() {
        log.info( "port = {}", port );
    }

    @SneakyThrows
    @Override
    protected ServerSocket createSocket() {
        try( var inputStream = IoStreams.in( keystoreLocation, PLAIN ) ) {
            log.info( "keystore {} exists, trying to initialize", keystoreLocation );
            KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            keyStore.load( inputStream, keystorePassword.toCharArray() );

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm() );
            keyManagerFactory.init( keyStore, keystorePassword.toCharArray() );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( keyManagerFactory.getKeyManagers(), null, null );

            ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket();
            init( serverSocket );

            log.info( "successfully initialized secure https listener" );
            return serverSocket;
        } catch( BindException e ) {
            SslHttpListener.log.error( "cannot bind to port [{}]", port );
            throw e;
        }
    }

    private void init( ServerSocket serverSocket ) throws IOException {
        serverSocket.setReuseAddress( true );
        serverSocket.setSoTimeout( soTimeout );
        serverSocket.bind( new InetSocketAddress( port ), backlog );
    }
}
