/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.util.Strings;
import oap.ws.Request;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.WsResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static oap.ws.Request.HttpMethod.GET;
import static oap.ws.Request.HttpMethod.POST;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.WsParam.From.REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

public class ReplicationWS {
    private static Logger logger = getLogger( ReplicationWS.class );

    @WsMethod( path = "/", method = GET )
    public WsResponse get(
        @WsParam( from = QUERY ) String service,
        @WsParam( from = QUERY ) long lastSyncTime
    ) {
        if( logger.isDebugEnabled() ) logger.debug( "get service =" + service + ", lastSyncTime = " + lastSyncTime );

        final ReplicationMaster<?> master = Application.service( service );

        if( master == null )
            return WsResponse.status( HttpURLConnection.HTTP_BAD_REQUEST, "service '" + service + "' not found." );

        final List<?> objects = master.get( lastSyncTime );

        if( objects.isEmpty() ) return WsResponse.NOT_MODIFIED;
        else return WsResponse.ok( objects );
    }

    @WsMethod( path = "/", method = POST )
    public WsResponse post(
        @WsParam( from = QUERY ) String service,
        @WsParam( from = REQUEST ) Request request
    ) throws IOException {

        final String body = request.body().map( Strings::readString ).orElse( "" );

        if( logger.isTraceEnabled() ) logger.trace( "post service =" + service + ", body = " + body );
        else if( logger.isDebugEnabled() ) logger.debug( "post service =" + service );

        final ReplicationMaster<?> master = Application.service( service );

        if( master == null )
            return WsResponse.status( HttpURLConnection.HTTP_BAD_REQUEST, "service '" + service + "' not found." );

        Object result = master.post( body, request.remoteAddress() );

        if( result instanceof List<?> && ((List<?>) result).isEmpty() ) return WsResponse.NO_CONTENT;
        else return WsResponse.ok( result );
    }
}
