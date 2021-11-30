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

package oap.http.server.undertow;

import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import oap.http.ContentTypes;
import oap.json.Binder;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpServerExchange {
    public final io.undertow.server.HttpServerExchange exchange;
    public final String ua;
    public final String referrer;
    public final String ip;

    public HttpServerExchange( io.undertow.server.HttpServerExchange exchange ) {
        this.exchange = exchange;
        this.ua = ua( exchange );
        this.referrer = referrer( exchange );
        this.ip = ip( exchange );
    }

    public static String ua( io.undertow.server.HttpServerExchange hsExchange ) {
        var values = hsExchange.getRequestHeaders().get( Headers.USER_AGENT );

        return values != null ? values.getFirst() : null;
    }

    public static String referrer( io.undertow.server.HttpServerExchange hsExchange ) {
        var values = hsExchange.getRequestHeaders().get( Headers.REFERER );

        return values != null ? values.getFirst() : null;
    }

    public static String ip( io.undertow.server.HttpServerExchange hsExchange ) {
        var values = hsExchange.getRequestHeaders().get( Headers.X_FORWARDED_FOR );

        if( values != null ) return values.getFirst();

        return hsExchange.getDestinationAddress().getAddress().getHostAddress();
    }

    public static boolean gzipSupported( io.undertow.server.HttpServerExchange hsExchange ) {
        var values = hsExchange.getRequestHeaders().get( Headers.ACCEPT_ENCODING );
        if( values == null ) return false;

        for( String value : values ) if( value.contains( "gzip" ) ) return true;

        return false;
    }

    public static boolean isRequestGzipped( io.undertow.server.HttpServerExchange hsExchange ) {
        List<String> values = hsExchange.getRequestHeaders().get( Headers.CONTENT_ENCODING );
        if( values == null ) return false;

        for( String value : values ) if( value.contains( "gzip" ) ) return true;

        return false;
    }

    public static boolean getBooleanParameter( Map<String, Deque<String>> requestParameters, String name ) {
        var values = requestParameters.get( name );
        return values != null && !values.isEmpty() && Boolean.parseBoolean( values.getFirst() );
    }

    public static String getStringParameter( Map<String, Deque<String>> requestParameters, String name ) {
        var values = requestParameters.get( name );
        return values != null && !values.isEmpty() ? values.getFirst() : null;
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public boolean gzipSupported() {
        return gzipSupported( exchange );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public boolean getBooleanParameter( String name ) {
        return getBooleanParameter( exchange.getQueryParameters(), name );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public String getStringParameter( String name ) {
        return getStringParameter( exchange.getQueryParameters(), name );
    }

    public void redirect( String url ) {
        exchange.setStatusCode( StatusCodes.FOUND );
        exchange.getResponseHeaders().put( Headers.LOCATION, url );
    }

    public String getRequestMethod() {
        return exchange.getRequestMethod().toString();
    }

    public String getRequestCookieValue( String name ) {
        Cookie requestCookie = exchange.getRequestCookie( name );
        if( requestCookie == null ) return null;
        return requestCookie.getValue();
    }

    public void noContent() {
        exchange.setStatusCode( StatusCodes.NO_CONTENT );
    }

    public String getRequestURI() {
        return exchange.getRequestURI();
    }

    public int getStatusCode() {
        return exchange.getStatusCode();
    }

    public void setStatusCode( int statusCode ) {
        exchange.setStatusCode( statusCode );
    }

    public void setStatusCodeReasonPhrase( int statusCode, String message ) {
        setStatusCode( statusCode );
        setReasonPhrase( message );
    }

    public void responseJson( int statusCode, String reasonPhrase, Object body ) {
        setStatusCode( statusCode );
        setReasonPhrase( reasonPhrase );
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON.getMimeType() );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public void responseJson( int statusCode, Object body ) {
        setStatusCode( statusCode );
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON.getMimeType() );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public void responseJson( Object body ) {
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON.getMimeType() );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public HttpServerExchange addQueryParam( String name, String param ) {
        exchange.addQueryParam( name, param );

        return this;
    }

    public String header( String headerName, String defaultValue ) {
        var values = exchange.getRequestHeaders().get( headerName );
        return values != null ? values.getFirst() : defaultValue;
    }

    public Optional<String> cookie( String name ) {
        return Optional.ofNullable( exchange.getRequestCookie( "name" ) ).map( Cookie::getValue );
    }

    public HttpServerExchange setResponseCookie( Cookie cookie ) {
        exchange.setResponseCookie( cookie );

        return this;
    }

    public Iterable<Cookie> responseCookies() {
        return exchange.responseCookies();
    }

    public void notFound() {
        exchange.setStatusCode( StatusCodes.NOT_FOUND );
    }

    public void setResponseHeader( HttpString name, String value ) {
        exchange.getResponseHeaders().put( name, value );
    }

    public void setResponseHeader( String name, String value ) {
        exchange.getResponseHeaders().put( new HttpString( name ), value );
    }

    public void ok( String body, String contentType ) {
        setStatusCode( StatusCodes.OK );
        setResponseHeader( Headers.CONTENT_TYPE, contentType );
        exchange.getResponseSender().send( body );
    }

    public void setReasonPhrase( String message ) {
        exchange.setReasonPhrase( message );
    }
}
