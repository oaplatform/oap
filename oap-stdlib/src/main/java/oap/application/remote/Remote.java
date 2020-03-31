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
import oap.http.cors.GenericCorsPolicy;
import oap.util.Result;
import oap.util.Try;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

@Slf4j
public class Remote implements HttpHandler {
    private final FST.SerializationMethod serialization;
    private final ThreadLocal<oap.application.remote.FST> fst = new ThreadLocal<>() {
        public FST initialValue() {
            return new FST( serialization );
        }
    };
    private final GenericCorsPolicy cors = GenericCorsPolicy.DEFAULT;

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
        FST fst = this.fst.get();

        exchange.getRequestReceiver().receiveFullBytes( ( ex, body ) -> {
            var invocation = ( RemoteInvocation ) fst.conf.asObject( body );

            log.trace( "invoke {}", invocation );

            Optional<Object> service = kernel.service( invocation.service );

            service.ifPresentOrElse( s -> {
                    Result<Object, Throwable> result;
                    int status = HTTP_OK;
                    try {
                        result = Result.success( s.getClass()
                            .getMethod( invocation.method, invocation.types() )
                            .invoke( s, invocation.values() ) );
                    } catch( NoSuchMethodException | IllegalAccessException e ) {
                        // transport error - illegal setup
                        // wrapping into RIE to be handled at client's properly
                        log.error( "method [{}] doesn't exist or access isn't allowed", invocation.method );
                        status = HTTP_NOT_FOUND;
                        result = Result.failure( new RemoteInvocationException( e ) );
                    } catch( InvocationTargetException e ) {
                        // application error
                        result = Result.failure( e.getCause() );
                        log.trace( "exception occurred on call to method [{}]", invocation.method );
                    }
                    exchange.setStatusCode( status );
                    exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, APPLICATION_OCTET_STREAM.toString() );

                    try( var os = fst.conf.getObjectOutput( exchange.getOutputStream() ) ) {
                        os.writeBoolean( result.isSuccess() );
                        if( !result.isSuccess() )
                            os.writeObject( result.failureValue );
                        else {
                            if( result.successValue instanceof Stream ) {
                                os.writeBoolean( true );
                                ( ( Stream ) result.successValue ).forEach( Try.consume( (obj -> {
                                    os.writeObject( obj );
                                } ) ) );
                            } else {
                                os.writeBoolean( false );
                                os.writeObject( result.successValue );
                            }
                        }
                        os.flush();
                    } catch( IOException e ) {
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
