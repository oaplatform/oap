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
package oap.http;

import oap.http.cors.CorsPolicy;
import oap.http.cors.RequestCors;
import oap.util.Pair;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Response {
    private static Logger logger = getLogger( Response.class );

    private org.apache.http.HttpResponse resp;
    private RequestCors cors;

    public Response( org.apache.http.HttpResponse resp, RequestCors cors ) {
        this.resp = resp;
        this.cors = cors;
    }

    public void respond( HttpResponse response ) {
        if( logger.isTraceEnabled() ) logger.trace( "responding " + response.code + " " + response.reasonPhrase );

        cors.setHeaders( resp );

        resp.setStatusCode( response.code );

        if( response.reasonPhrase != null )
            resp.setReasonPhrase( response.reasonPhrase );
        if( !response.headers.isEmpty() )
            for( Pair<String, String> header : response.headers )
                resp.setHeader( header._1, header._2 );
        if( response.contentEntity != null ) resp.setEntity( response.contentEntity );
    }

}
