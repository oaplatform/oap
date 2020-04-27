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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.util.Result;
import oap.util.Try;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

@Slf4j
public class Remote implements HttpHandler {
    private final FST.SerializationMethod serialization;
    private final Kernel kernel;
    private final Undertow undertow;

    public Remote( FST.SerializationMethod serialization, int port, String context, Kernel kernel ) {
        this.serialization = serialization;
        this.kernel = kernel;

        undertow = Undertow
            .builder()
            .addHttpListener( port, "0.0.0.0" )
            .setHandler( Handlers.pathTemplate().add( context, new BlockingHandler( this ) ) )
            .build();
    }

    public void start() {
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
            var invocation = ( RemoteInvocation ) fst.configuration.asObject( body );
            log.trace( "invoke {}", invocation );

            var service = kernel.service( invocation.service );

            service.ifPresentOrElse( s -> {
                    try {
                        Result<Object, Throwable> r;
                        int status = HTTP_OK;
                        try {
                            r = Result.success( s.getClass()
                                .getMethod( invocation.method, invocation.types() )
                                .invoke( s, invocation.values() ) );
                        } catch( NoSuchMethodException | IllegalAccessException e ) {
                            // transport error - illegal setup
                            // wrapping into RIE to be handled at client's properly
                            log.error( "method [{}] doesn't exist or access isn't allowed", invocation.method );
                            status = HTTP_NOT_FOUND;
                            r = Result.failure( new RemoteInvocationException( e ) );
                        } catch( InvocationTargetException e ) {
                            // application error
                            r = Result.failure( e.getCause() );
                            log.trace( "exception occurred on call to method [{}]", invocation.method );
                        }
                        exchange.setStatusCode( status );
                        exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, APPLICATION_OCTET_STREAM.toString() );
                        var result = r;

                        try( var outputStream = exchange.getOutputStream();
                             var bos = new BufferedOutputStream( outputStream );
                             var dos = new DataOutputStream( bos ) ) {
                            dos.writeBoolean( result.isSuccess() );

                            if( !result.isSuccess() ) {
                                fst.writeObjectWithSize( dos, result.failureValue );
                            } else {
                                if( result.successValue instanceof Stream<?> ) {
                                    dos.writeBoolean( true );

                                    ( ( Stream ) result.successValue ).forEach( Try.consume( ( obj -> {
                                        fst.writeObjectWithSize( dos, obj );
                                    } ) ) );
                                    dos.writeInt( 0 );
                                } else {
                                    dos.writeBoolean( false );
                                    fst.writeObjectWithSize( dos, result.successValue );
                                }
                            }
                        }
                    } catch( Throwable e ) {
                        log.error( "invocation = {}", invocation );
                        log.error( e.getMessage(), e );
                    }
                },
                () -> {
                    exchange.setStatusCode( HTTP_NOT_FOUND );
                    exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, TEXT_PLAIN.toString() );
                    exchange.getResponseSender().send( invocation.service + " not found" );
                }
            );
        } );
    }
}
