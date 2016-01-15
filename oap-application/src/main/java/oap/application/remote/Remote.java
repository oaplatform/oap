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

import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import oap.application.Application;
import oap.http.*;
import oap.util.Try;

import java.lang.reflect.InvocationTargetException;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class Remote implements Handler {
    private static ThreadLocal<FST> fst = new ThreadLocal<FST>() {
        public FST initialValue() {
            return new FST();
        }
    };
    private HttpServer server;
    private String context;
    private Cors cors = new Cors();

    public Remote( HttpServer server, String context ) {
        this.server = server;
        this.context = context;
    }

    public void start() {
        server.bind( context, cors, this, false );
    }

    @Override
    public void handle( Request request, Response response ) {
        final FST fst = Remote.fst.get();

        RemoteInvocation invocation = request.body
            .map( Try.map( bytes -> ( RemoteInvocation ) fst.conf.asObject( ByteStreams.toByteArray( bytes ) ) ) )
            .orElseThrow( () -> new RemoteInvocationException( "no invocation data" ) );

        if( log.isTraceEnabled() ) log.trace( "invoke:" + invocation );

        Object service = Application.service( invocation.service );

        if( service == null ) response.respond( HttpResponse.status( HTTP_NOT_FOUND,
            invocation.service + " not found" ) );
        else try {
            Object result = service.getClass()
                .getMethod( invocation.method, invocation.types() )
                .invoke( service, invocation.values() );
            response.respond( HttpResponse.bytes( fst.conf.asByteArray( result ), APPLICATION_OCTET_STREAM ) );
        } catch( NoSuchMethodException e ) {
            log.debug( e.getMessage(), e );
            response.respond( HttpResponse.status( HTTP_NOT_FOUND,
                "service " + invocation.service + "." + invocation.method + " not found." ) );
        } catch( InvocationTargetException | IllegalAccessException e ) {
            log.debug( e.getMessage(), e );
            response.respond( HttpResponse.status( HTTP_INTERNAL_ERROR, e.getMessage() ) );
        }
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
