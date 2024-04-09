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
package oap.remote;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.util.Result;
import oap.util.function.Try;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
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

    private final FST.SerializationMethod serialization;
    private final String context;
    private final RemoteServices services;

    public Remote( FST.SerializationMethod serialization, String context, RemoteServices services, NioHttpServer server ) {
        this( serialization, context, services, server, null );
    }

    public Remote( FST.SerializationMethod serialization, String context, RemoteServices services, NioHttpServer server, String port ) {
        this.serialization = serialization;
        this.context = context;
        log.debug( "Initializing remote for {}...", services.getName() );
        this.services = services;

        if( port != null ) {
            server.bind( context, this, port );
        } else {
            server.bind( context, this );
        }

        errorMetrics = Metrics.counter( "remote_server", Tags.of( "status", "error" ) );
        successMetrics = Metrics.counter( "remote_server", Tags.of( "status", "success" ) );
    }

    public void start() {
        log.info( "serialization = {}, context = {}", serialization, context );
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        RemoteInvocation invocation = null;
        try {
            var fst = new FST( serialization );

            invocation = getRemoteInvocation( fst, exchange.getInputStream() );

            Object service = services.get( invocation.service );

            if( service == null ) {
                errorMetrics.increment();
                exchange.setStatusCode( HTTP_NOT_FOUND );
                exchange.setResponseHeader( CONTENT_TYPE, TEXT_PLAIN );
                exchange.setReasonPhrase( invocation.service + " not found among " + services.keySet() + " in " + services.getName() );
                return;
            }
            Result<Object, Throwable> result;
            int status = HTTP_OK;
            try {
                Object invokeResult = service.getClass()
                    .getMethod( invocation.method, invocation.types() )
                    .invoke( service, invocation.values() );
                result = Result.success( invokeResult );
            } catch( NoSuchMethodException | IllegalAccessException e ) {
                // transport error - illegal setup
                // wrapping into RIE to be handled at client's properly
                log.error( "method [{}#{}] doesn't exist or access isn't allowed",
                    service.getClass().getCanonicalName(), invocation.method, e );
                log.debug( "method '{}' types {} parameters {}",
                    invocation.method,
                    invocation.types() != null ? List.of( invocation.types() ) : null,
                    invocation.values() != null ? List.of( invocation.values() ) : null );
                status = HTTP_NOT_FOUND;
                result = Result.failure( new RemoteInvocationException( e ) );
            } catch( InvocationTargetException e ) {
                // application error
                result = Result.failure( e.getCause() );
                log.warn( "{} occurred on call to method [{}#{}]",
                    e.getCause().getClass().getCanonicalName(), service.getClass().getCanonicalName(), invocation.method, e );
                log.debug( "method '{}' types {} parameters {}",
                    invocation.method,
                    invocation.types() != null ? List.of( invocation.types() ) : null,
                    invocation.values() != null ? List.of( invocation.values() ) : null );
            }
            exchange.setStatusCode( status );
            exchange.setResponseHeader( CONTENT_TYPE, APPLICATION_OCTET_STREAM );

            try( var outputStream = exchange.getOutputStream();
                 var bos = new BufferedOutputStream( outputStream );
                 var dos = new DataOutputStream( bos ) ) {
                dos.writeBoolean( result.isSuccess() );

                if( !result.isSuccess() ) {
                    fst.writeObjectWithSize( dos, result.failureValue );
                } else if( result.successValue instanceof Stream<?> ) {
                    dos.writeBoolean( true );

                    ( ( Stream<?> ) result.successValue ).forEach( Try.consume( obj ->
                        fst.writeObjectWithSize( dos, obj ) ) );
                    dos.writeInt( 0 );
                } else {
                    dos.writeBoolean( false );
                    fst.writeObjectWithSize( dos, result.successValue );
                }
            }
            if( result.isSuccess() ) {
                successMetrics.increment();
            } else {
                errorMetrics.increment();
            }
        } catch( Throwable e ) {
            log.error( "invocation = {}", invocation, e );
        }
    }

    @SneakyThrows
    public RemoteInvocation getRemoteInvocation( FST fst, InputStream body ) {
        var dis = new DataInputStream( body );
        var version = dis.readInt();

        var invocation = ( RemoteInvocation ) fst.readObjectWithSize( dis );
        log.trace( "invoke v{} - {}", version, invocation );
        return invocation;
    }
}
