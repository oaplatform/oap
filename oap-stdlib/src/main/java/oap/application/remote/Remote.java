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
import org.apache.http.entity.ContentType;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class Remote implements HttpHandler {
    private final FST.SerializationMethod serialization;
    private final ThreadLocal<oap.application.remote.FST> fst = new ThreadLocal<FST>() {
        public FST initialValue() {
            return new FST( serialization );
        }
    };
    private final GenericCorsPolicy cors = GenericCorsPolicy.DEFAULT;

    private final int port;
    private final String context;
    private final Kernel kernel;
    private final Undertow undertow;

    public Remote( FST.SerializationMethod serialization, int port, String context, Kernel kernel ) {
        this.serialization = serialization;
        this.port = port;
        this.context = context;
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
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        FST fst = this.fst.get();

        exchange.getRequestReceiver().receiveFullBytes( ( ex, body ) -> {
            var invocation = ( RemoteInvocation ) fst.conf.asObject( body );

            log.trace( "invoke {}", invocation );

            Optional<Object> service = kernel.service( invocation.service );

            service.ifPresentOrElse( s -> {
                    Result<Object, Throwable> result;
                    try {
                        result = Result.success( s.getClass()
                            .getMethod( invocation.method, invocation.types() )
                            .invoke( s, invocation.values() ) );
                    } catch( NoSuchMethodException | IllegalAccessException e ) {
                        result = Result.failure( e );
                        log.trace( "Method [{}] doesn't exist or access isn't allowed", invocation.method );
                    } catch( InvocationTargetException e ) {
                        result = Result.failure( e.getCause() );
                        log.trace( "Exception occurred on call to method [{}]", invocation.method );
                    }
                    exchange.setStatusCode( HTTP_OK );
                    exchange.getResponseSender().send( ByteBuffer.wrap( fst.conf.asByteArray( result ) ) );
                    exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, APPLICATION_OCTET_STREAM.toString() );
                },
                () -> {
                    exchange.setStatusCode( HTTP_NOT_FOUND );
                    exchange.getResponseSender().send( invocation.service + " not found" );
                    exchange.getResponseHeaders().add( Headers.CONTENT_TYPE, ContentType.DEFAULT_TEXT.toString() );
                }
            );
        } );
    }
}
