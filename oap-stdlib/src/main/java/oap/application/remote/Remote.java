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
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.util.Result;
import oap.util.function.Try;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.xnio.Options.READ_TIMEOUT;
import static org.xnio.Options.WRITE_TIMEOUT;

@Slf4j
public class Remote implements HttpHandler {
    private final Counter errorMetrics;
    private final Counter successMetrics;

    private final FST.SerializationMethod serialization;
    private final int port;
    private final String context;
    private final Kernel kernel;
    private final long timeout;
    private final Undertow undertow;

    public Remote( FST.SerializationMethod serialization, int port, String context, Kernel kernel, long timeout ) {
        this.serialization = serialization;
        this.port = port;
        this.context = context;
        this.kernel = kernel;
        this.timeout = timeout;

        undertow = Undertow
            .builder()
            .addHttpListener( port, "0.0.0.0" )
            .setServerOption( UndertowOptions.ALWAYS_SET_KEEP_ALIVE, true )
            .setSocketOption( READ_TIMEOUT, ( int ) timeout )
            .setSocketOption( WRITE_TIMEOUT, ( int ) timeout )
            .setHandler( Handlers.pathTemplate().add( context, new BlockingHandler( this ) ) )
            .build();

        errorMetrics = Metrics.counter( "remote_server", Tags.of( "status", "error" ) );
        successMetrics = Metrics.counter( "remote_server", Tags.of( "status", "success" ) );
    }

    public void start() {
        log.info( "port = {}, timeout = {}, serialization = {}, context = {}", port, timeout, serialization, context );

        undertow.start();
    }

    public void preStop() {
        undertow.stop();
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        var fst = new FST( serialization );

        exchange.getRequestReceiver().receiveFullBytes( ( ex, body ) -> {
            var invocation = getRemoteInvocation( fst, body );

            var service = kernel.service( "*", invocation.service );

            service.ifPresentOrElse( s -> {
                    try {
                        Result<Object, Throwable> r;
                        int status = HTTP_OK;
                        try {
                            r = Result.success( s.getClass()
                                .getMethod( invocation.method, invocation.types() )
                                .invoke( s, invocation.values() ) );
                        } catch( NoSuchMethodException | IllegalAccessException e ) {
                            errorMetrics.increment();
                            // transport error - illegal setup
                            // wrapping into RIE to be handled at client's properly
                            log.error( "method [{}] doesn't exist or access isn't allowed", invocation.method );
                            log.debug( "method '{}' types {} parameters {}", invocation.method, List.of( invocation.types() ), List.of( invocation.values() ) );
                            log.debug( e.getMessage(), e );
                            status = HTTP_NOT_FOUND;
                            r = Result.failure( new RemoteInvocationException( e ) );
                        } catch( InvocationTargetException e ) {
                            errorMetrics.increment();
                            // application error
                            r = Result.failure( e.getCause() );
                            log.debug( "exception occurred on call to method [{}]", invocation.method );
                            log.trace( "method '{}' types {} parameters {}", invocation.method, List.of( invocation.types() ), List.of( invocation.values() ) );
                            log.trace( e.getMessage(), e );
                        }
                        exchange.setStatusCode( status );
                        exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, APPLICATION_OCTET_STREAM.toString() );
                        var result = r;

                        try( var outputStream = exchange.getOutputStream();
                             var bos = new BufferedOutputStream( outputStream );
                             var dos = new DataOutputStream( bos ) ) {
                            dos.writeBoolean( result.isSuccess() );

                            if( !result.isSuccess() ) fst.writeObjectWithSize( dos, result.failureValue );
                            else if( result.successValue instanceof Stream<?> ) {
                                dos.writeBoolean( true );

                                ( ( Stream<?> ) result.successValue ).forEach( Try.consume( obj ->
                                    fst.writeObjectWithSize( dos, obj ) ) );
                                dos.writeInt( 0 );
                            } else {
                                dos.writeBoolean( false );
                                fst.writeObjectWithSize( dos, result.successValue );
                            }
                        }
                        successMetrics.increment();
                    } catch( Throwable e ) {
                        log.error( "invocation = {}", invocation );
                        log.error( e.getMessage(), e );
                    }
                },
                () -> {
                    errorMetrics.increment();
                    exchange.setStatusCode( HTTP_NOT_FOUND );
                    exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, TEXT_PLAIN.toString() );
                    exchange.getResponseSender().send( invocation.service + " not found" );
                }
            );
        } );
    }

    @SneakyThrows
    public RemoteInvocation getRemoteInvocation( FST fst, byte[] body ) {
        var dis = new DataInputStream( new ByteArrayInputStream( body ) );
        var version = dis.readInt();

        var invocation = ( RemoteInvocation ) fst.readObjectWithSize( dis );
        log.trace( "invoke v{} - {}", version, invocation );
        return invocation;
    }
}
