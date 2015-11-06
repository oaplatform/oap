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

package oap.replication;

import oap.application.Application;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;

import static oap.http.Request.HttpMethod.POST;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

public class ReplicationWS {
    private static Logger logger = getLogger( ReplicationWS.class );

    @WsMethod( path = "/", method = POST )
    public Object post(
        @WsParam( from = BODY ) RpcData rpcData,
        @WsParam( from = REQUEST ) Request request
    ) throws IOException {

        final String service = rpcData.service;

        if( logger.isTraceEnabled() ) logger.trace( "post service =" + rpcData );
         if( logger.isDebugEnabled() ) logger.debug( "post service =" + service );

        final Object master = Application.service( service );

        if( master == null )
            return HttpResponse.status( HttpURLConnection.HTTP_BAD_REQUEST, "service '" + service + "' not found." );

        try {
            final Method method = master.getClass()
                .getMethod( rpcData.method, rpcData.arguments.stream().map( v -> v.type ).toArray( Class[]::new ) );

            final Object result = method.invoke( master, rpcData.arguments.stream().map( v -> v.value ).toArray() );

            return HttpResponse.ok( result );

        } catch( NoSuchMethodException e ) {
            logger.debug( e.getMessage(), e );
            return HttpResponse.status( HttpURLConnection.HTTP_BAD_REQUEST,
                "service '" + service + "', method '" + rpcData.method + "' not found." );
        } catch( InvocationTargetException | IllegalAccessException e ) {
            logger.debug( e.getMessage(), e );
            return HttpResponse.status( HttpURLConnection.HTTP_INTERNAL_ERROR,
                e.getMessage() );
        }
    }
}
