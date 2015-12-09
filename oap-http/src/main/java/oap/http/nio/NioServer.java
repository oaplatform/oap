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

import com.fasterxml.jackson.annotation.JsonProperty;
import oap.http.Handler;
import oap.http.Server;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

public class NioServer implements oap.http.HttpServer {

    private static Logger logger = LoggerFactory.getLogger( Server.class );
    private UriHttpAsyncRequestHandlerMapper mapper = new UriHttpAsyncRequestHandlerMapper();
    private final int port;
    @JsonProperty( "default-headers" )
    private LinkedHashMap<String, String> defaultHeaders = new LinkedHashMap<>();
    private HttpServer server;

    public NioServer( int port ) {
        this.port = port;
        this.mapper.register( "/static/*", new NioClasspathResourceHandler( "/static", "/WEB-INF" ) );

        IOReactorConfig config = IOReactorConfig.custom()
            .setTcpNoDelay( true )
            .setSoKeepAlive( true )
            .build();

        server = ServerBootstrap.bootstrap()
            .setListenerPort( port )
            .setServerInfo( "OAP Server/1.0" )
            .setIOReactorConfig( config )
            .setExceptionLogger( ExceptionLogger.STD_ERR )
            .setHandlerMapper( mapper )
            .create();
    }

    @Override
    public void bind( String context, Handler handler ) {
        String location = "/" + context + "/*";
        this.mapper.register( location, new NioHandlerAdapter( "/" + context, handler, defaultHeaders ) );
        logger.info( handler + " bound to " + location );

    }

    public void unbind( String context ) {
        this.mapper.unregister( "/" + context + "/*" );
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
