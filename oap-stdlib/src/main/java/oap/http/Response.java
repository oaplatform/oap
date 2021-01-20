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

import lombok.extern.slf4j.Slf4j;
import oap.http.cors.RequestCors;
import oap.http.server.apache.entity.HttpGzipOutputStreamEntity;
import oap.util.Pair;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
public class Response {
    private final Request request;
    private org.apache.http.HttpResponse underlying;
    private RequestCors cors;

    public Response( Request request, org.apache.http.HttpResponse underlying, RequestCors cors ) {
        this.request = request;
        this.underlying = underlying;
        this.cors = cors;
    }

    public void respond( HttpResponse response ) {

        log.trace( "responding {} {}", response.code, response.reason );

        cors.applyTo( underlying );

        var isGzip = request.gzipSupported();

        underlying.setStatusCode( response.code );

        if( response.reason != null ) underlying.setReasonPhrase( response.reason );

        log.trace( "headers: {}", response.headers );

        for( Pair<String, String> header : response.headers )
            underlying.setHeader( header._1, header._2 );

        if( isGzip ) underlying.setHeader( "Content-encoding", "gzip" );

        for( Pair<String, String> cookie : response.cookies ) underlying.addHeader( cookie._1, cookie._2 );

        log.trace( "cookies: {}", response.cookies );

        if( response.contentEntity != null )
            if( isGzip ) underlying.setEntity( new HttpGzipOutputStreamEntity( out -> {
                try {
                    response.contentEntity.writeTo( out );
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            }, null ) );
            else underlying.setEntity( response.contentEntity );
    }

}
