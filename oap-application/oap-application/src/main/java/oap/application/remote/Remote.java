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
package oap.application.remote;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.util.function.Try;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Http.ContentType.APPLICATION_OCTET_STREAM;
import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.Headers.CONTENT_TYPE;


@Slf4j
public class Remote implements HttpHandler {
    private final Counter errorMetrics;
    private final Counter successMetrics;

    private final String context;
    private final RemoteServices services;

    public Remote( String context, RemoteServices services, NioHttpServer server ) {
        this( context, services, server, null );
    }

    public Remote( String context, RemoteServices services, NioHttpServer server, String port ) {
        this.context = context;
        this.services = services;

        log.debug( "Initializing remote for {}...", services.getName() );

        if( port != null ) {
            server.bind( context, this, port );
        } else {
            server.bind( context, this );
        }

        errorMetrics = Metrics.counter( "remote_server", Tags.of( "status", "error" ) );
        successMetrics = Metrics.counter( "remote_server", Tags.of( "status", "success" ) );
    }

    public void start() {
        log.info( "context {}", context );
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        RemoteInvocation invocation = null;
        try {

            invocation = getRemoteInvocation( exchange.getInputStream() );

            Object service = services.get( invocation.service );

            if( service == null ) {
                errorMetrics.increment();
                exchange.setStatusCode( HTTP_NOT_FOUND );
                exchange.setResponseHeader( CONTENT_TYPE, TEXT_PLAIN );
                exchange.setReasonPhrase( invocation.service + " not found among " + services.keySet() + " in " + services.getName() );
                return;
            }
            CompletableFuture<?> result;
            MutableInt status = new MutableInt( HTTP_OK );
            try {
                Object invokeResult = service.getClass()
                    .getMethod( invocation.method, invocation.types() )
                    .invoke( service, invocation.values() );

                if( invokeResult instanceof CompletableFuture<?> cf ) {
                    result = cf;
                } else {
                    result = CompletableFuture.completedFuture( invokeResult );
                }

            } catch( NoSuchMethodException | IllegalAccessException e ) {
                // transport error - illegal setup
                // wrapping into RIE to be handled at client's properly
                log.error( "method [{}#{}] doesn't exist or access isn't allowed",
                    service.getClass().getCanonicalName(), invocation.method, e );
                log.debug( "method '{}' types {} parameters {}",
                    invocation.method,
                    invocation.types() != null ? List.of( invocation.types() ) : null,
                    invocation.values() != null ? List.of( invocation.values() ) : null );
                status.setValue( HTTP_NOT_FOUND );
                result = CompletableFuture.failedFuture( new RemoteInvocationException( e ) );
            } catch( InvocationTargetException e ) {
                // application error
                result = CompletableFuture.failedFuture( e.getCause() );
                log.warn( "{} occurred on call to method [{}#{}]",
                    e.getCause().getClass().getCanonicalName(), service.getClass().getCanonicalName(), invocation.method, e );
                log.debug( "method '{}' types {} parameters {}",
                    invocation.method,
                    invocation.types() != null ? List.of( invocation.types() ) : null,
                    invocation.values() != null ? List.of( invocation.values() ) : null );
            }

            RemoteInvocation finalInvocation = invocation;
            result.whenComplete( ( v, ex ) -> {
                exchange.setStatusCode( status.getValue() );
                exchange.setResponseHeader( CONTENT_TYPE, APPLICATION_OCTET_STREAM );

                try( OutputStream outputStream = exchange.getOutputStream();
                     BufferedOutputStream bos = new BufferedOutputStream( outputStream );
                     DataOutputStream dos = new DataOutputStream( bos ) ) {
                    dos.writeBoolean( ex == null );

                    if( ex != null ) {
                        FstConsts.writeObjectWithSize( dos, ex );
                    } else if( v instanceof Stream<?> ) {
                        dos.writeBoolean( true );

                        ( ( Stream<?> ) v ).forEach( Try.consume( obj -> {
                            dos.writeBoolean( true );
                            FstConsts.writeObjectWithSize( dos, obj );
                        } ) );
                        dos.writeBoolean( false );
                    } else {
                        dos.writeBoolean( false );
                        FstConsts.writeObjectWithSize( dos, v );
                    }
                } catch( Throwable e ) {
                    log.error( "invocation {}", finalInvocation, e );
                }
                if( ex != null ) {
                    successMetrics.increment();
                } else {
                    errorMetrics.increment();
                }
            } );

        } catch( Throwable e ) {
            log.error( "invocation {}", invocation, e );
        }
    }

    public RemoteInvocation getRemoteInvocation( InputStream body ) throws IOException {
        DataInputStream dis = new DataInputStream( body );
        int version = dis.readInt();

        RemoteInvocation invocation = FstConsts.<RemoteInvocation>readObjectWithSize( dis );
        log.trace( "invoke v{} - {}", version, invocation );
        return invocation;
    }
}
