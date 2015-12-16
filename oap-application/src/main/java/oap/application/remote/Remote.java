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

import lombok.extern.slf4j.Slf4j;
import oap.application.Application;
import oap.http.Cors;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.http.Request;
import oap.http.Response;
import oap.json.Binder;

import java.lang.reflect.InvocationTargetException;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Slf4j
public class Remote implements Handler {
    private HttpServer server;
    private String context;
    private Cors cors = new Cors();

    public Remote( HttpServer server, String context ) {
        this.server = server;
        this.context = context;
    }

    public void start() {
        server.bind( context, cors, this );
    }

    @Override
    public void handle( Request request, Response response ) {
        RemoteInvocation invocation = request.body
            .<RemoteInvocation>map( bytes -> Binder.jsonWithTyping.unmarshal( RemoteInvocation.class, bytes ) )
            .orElseThrow( () -> new RemoteInvocationException( "no invocation data" ) );

        if( log.isTraceEnabled() ) log.trace( "invoke:" + invocation );

        Object service = Application.service( invocation.service );

        if( service == null ) response.respond( HttpResponse.status( HTTP_NOT_FOUND,
            invocation.service + " not found" ) );
        else try {
            Object result = service.getClass()
                .getMethod( invocation.method, invocation.types() )
                .invoke( service, invocation.values() );
            response.respond( HttpResponse.ok( Binder.jsonWithTyping.marshal( result ), true, APPLICATION_JSON ) );
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
