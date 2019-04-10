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
package oap.http.nio;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.Handler;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.cors.CorsPolicy;
import oap.io.IoStreams;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class NioServer implements oap.http.HttpServer {

    private static Logger logger = LoggerFactory.getLogger( Server.class );
    private final int port;
    protected Path keystoreLocation;
    protected String keystorePassword;

    private UriHttpAsyncRequestHandlerMapper mapper = new UriHttpAsyncRequestHandlerMapper();
    private HttpServer server;

    @SneakyThrows
    public NioServer( int port, int workers, boolean registerStatic ) {
        this.port = port;
        if( registerStatic )
            this.mapper.register( "/static/*", new NioClasspathResourceHandler( "/static", "/WEB-INF" ) );

        var ioReactorConfig = IOReactorConfig.custom().setIoThreadCount( workers ).build();
        var httpProcessor = HttpProcessorBuilder.create()
            .add( new ResponseDate() )
            .add( new ResponseServer( "OAP Server/1.0" ) )
            .add( new ResponseContent() )
            .add( new ResponseConnControl() )
            .build();

        SSLContext sslContext = getSslContext( port );


        server = ServerBootstrap.bootstrap()
            .setListenerPort( port )
            .setServerInfo( "OAP Server/1.0" )
            .setConnectionReuseStrategy( DefaultClientConnectionReuseStrategy.INSTANCE )
            .setHttpProcessor( httpProcessor )
            .setIOReactorConfig( ioReactorConfig )
            .setSslContext( sslContext )
            .setExceptionLogger( ex -> log.debug( ex.getMessage(), ex ) )
            .setHandlerMapper( mapper )
            .create();
    }

    public SSLContext getSslContext( int port ) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        SSLContext sslContext;
        if( keystoreLocation != null && Files.exists( keystoreLocation ) ) {
            try( var inputStream = IoStreams.in( keystoreLocation, PLAIN ) ) {
                log.info( "Keystore {} exists, trying to initialize", keystoreLocation );
                KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
                keyStore.load( inputStream, keystorePassword.toCharArray() );

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm() );
                keyManagerFactory.init( keyStore, keystorePassword.toCharArray() );

                sslContext = SSLContext.getInstance( "TLS" );
                sslContext.init( keyManagerFactory.getKeyManagers(), null, null );

                log.info( "Successfully initialized secure http listener" );
            } catch( BindException e ) {
                log.error( "Cannot bind to port [{}]", port );
                throw e;
            }
        } else {
            throw new CertificateException( keystoreLocation + " not found" );
        }
        return sslContext;
    }

    @Override
    public void bind( String context, CorsPolicy corsPolicy, Handler handler, Protocol protocol ) {
        String location = "/" + context + "/*";
        this.mapper.register( location, new NioHandlerAdapter( "/" + context, handler, corsPolicy, protocol ) );
        logger.info( handler + " bound to " + location );

    }

    @Override
    public void unbind( String context ) {
        this.mapper.unregister( "/" + context + "/*" );
    }

    @Override
    public void accepted( Socket socket ) {
        throw new UnsupportedOperationException( "NioServer is not yet supported" );
    }

    public void start() {
        try {
            logger.info( "starting [localhost:" + port + "]..." );

            server.start();

        } catch( Exception e ) {
            logger.error( e.getMessage() + " [" + server.getEndpoint().getAddress() + ":" + port + "]", e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void stop() {
        server.shutdown( 1, TimeUnit.SECONDS );
        try {
            server.awaitTermination( 60, TimeUnit.SECONDS );
        } catch( InterruptedException e ) {
            logger.debug( e.getMessage(), e );
        }

        logger.info( "server gone down" );
    }

}
