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

package oap.ws;

import com.google.common.base.Preconditions;
import oap.http.Cookie;
import oap.http.server.nio.HttpServerExchange;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.Headers.CONTENT_TYPE;
import static oap.http.Http.Headers.LOCATION;
import static oap.http.Http.StatusCode.FOUND;
import static oap.http.Http.StatusCode.NOT_FOUND;
import static oap.http.Http.StatusCode.NO_CONTENT;
import static oap.http.Http.StatusCode.OK;

public class Response {
    public final HashMap<String, String> headers = new HashMap<>();
    public final ArrayList<Cookie> cookies = new ArrayList<>();
    public int code;
    public String contentType;
    public Object body;
    public boolean raw;
    public String reasonPhrase;

    public Response( int code ) {
        this( code, null );
    }

    public Response( int code, String reasonPhrase ) {
        this( code, reasonPhrase, null );
    }

    public Response( int code, String reasonPhrase, String contentType ) {
        this( code, reasonPhrase, contentType, null );
    }

    public Response( int code, String reasonPhrase, String contentType, Object body ) {
        this( code, reasonPhrase, contentType, body, false );
    }

    public Response( int code, String reasonPhrase, String contentType, Object body, boolean raw ) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
        this.contentType = contentType;
        this.body = body;
        this.raw = raw;
    }

    public static Response noContent() {
        return new Response( NO_CONTENT );
    }

    public static Response jsonOk() {
        return ok().withContentType( APPLICATION_JSON );
    }

    public static Response notFound() {
        return new Response( NOT_FOUND );
    }

    public static Response ok() {
        return new Response( OK );
    }

    public static Response redirect( URI uri ) {
        return redirect( uri.toString() );
    }

    public static Response redirect( String uri ) {
        return new Response( FOUND ).withHeader( LOCATION, uri );
    }

    public Response withStatusCode( int code ) {
        this.code = code;

        return this;
    }

    public Response withReasonPhrase( String reasonPhrase ) {
        this.reasonPhrase = reasonPhrase;

        return this;
    }

    public Response withContentType( String contentType ) {
        this.contentType = contentType;

        return this;
    }

    public Response withBody( Object body ) {
        return withBody( body, false );
    }

    public Response withBody( Object body, boolean raw ) {
        this.body = body;
        this.raw = raw;

        return this;
    }

    public Response withHeader( String name, String value ) {
        headers.put( name, value );

        return this;
    }

    public Response withCookie( Cookie cookie ) {
        cookies.add( cookie );

        return this;
    }

    public String bodyToString() {
        if( body == null ) return null;

        if( body instanceof byte[] bytes ) return new String( bytes );
        else if( body instanceof ByteBuffer byteBuffer ) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get( bytes );
            return new String( bytes );
        } else if( body instanceof String str ) {
            if( raw ) return str;
            else return HttpServerExchange.contentToString( false, str, contentType );
        } else if( body instanceof Consumer ) {
            @SuppressWarnings( "unchecked" )
            var cons = ( Consumer<ByteArrayOutputStream> ) body;
            var baos = new ByteArrayOutputStream();
            cons.accept( baos );
            body = baos.toByteArray();
            return new String( ( byte[] ) body );
        } else {
            Preconditions.checkArgument( !raw );
            return HttpServerExchange.contentToString( false, body, contentType );
        }

    }

    @SuppressWarnings( "unchecked" )
    public void send( HttpServerExchange exchange ) {
        exchange.setStatusCode( code );
        if( reasonPhrase != null ) exchange.setReasonPhrase( reasonPhrase );
        headers.forEach( exchange::setResponseHeader );
        cookies.forEach( exchange::setResponseCookie );
        if( contentType != null ) exchange.setResponseHeader( CONTENT_TYPE, contentType );
        if( body != null )
            if( body instanceof byte[] bytes ) exchange.send( bytes );
            else if( body instanceof ByteBuffer byteBuffer ) exchange.send( byteBuffer );
            else if( body instanceof String string )
                if( raw ) exchange.send( string );
                else exchange.send( HttpServerExchange.contentToString( false, string, contentType ) );
            else if( body instanceof Consumer cons ) cons.accept( exchange.getOutputStream() );
            else {
                Preconditions.checkArgument( !raw );
                exchange.send( HttpServerExchange.contentToString( false, body, contentType ) );
            }
        else exchange.endExchange();
    }
}
