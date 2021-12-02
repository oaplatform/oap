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

package oap.http.server.nio;

import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import oap.http.ContentTypes;
import oap.http.HttpStatusCodes;
import oap.json.Binder;
import oap.util.HashMaps;
import oap.util.function.Try;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class HttpServerExchange {
    private static final Map<String, Function<Object, String>> producers = HashMaps.of(
        TEXT_PLAIN.getMimeType(), String::valueOf,
        APPLICATION_JSON.getMimeType(), Binder.json::marshal
    );

    public final io.undertow.server.HttpServerExchange exchange;

    public HttpServerExchange( io.undertow.server.HttpServerExchange exchange ) {
        this.exchange = exchange;
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

    public static void registerProducer( String mimeType, Function<Object, String> producer ) {
        producers.put( mimeType, producer );
    }

    private static String contentToString( boolean raw, Object content, String contentType ) {
        return raw ? ( String ) content
            : producers.getOrDefault( contentType, String::valueOf ).apply( content );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public String ip() {
        return ip( exchange );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public String referrer() {
        return referrer( exchange );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public String ua() {
        return ua( exchange );
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
        exchange.setStatusCode( HttpStatusCodes.FOUND );
        exchange.getResponseHeaders().put( Headers.LOCATION, url );
    }

    public HttpMethod getRequestMethod() {
        return HttpMethod.valueOf( exchange.getRequestMethod().toString() );
    }

    public String getRequestCookieValue( String name ) {
        Cookie requestCookie = exchange.getRequestCookie( name );
        if( requestCookie == null ) return null;
        return requestCookie.getValue();
    }

    public HeaderMap getRequestHeaders() {
        return exchange.getRequestHeaders();
    }

    public String getRequestHeader( String name ) {
        HeaderValues values = exchange.getRequestHeaders().get( name );
        if( values == null || values.isEmpty() ) return null;
        return values.getFirst();
    }

    public void responseNotFound() {
        exchange.setStatusCode( HttpStatusCodes.NOT_FOUND );
    }

    public void responseNoContent() {
        exchange.setStatusCode( HttpStatusCodes.NO_CONTENT );
    }

    public String getRequestURI() {
        return exchange.getRequestURI();
    }

    public String getResolvedPath() {
        return exchange.getResolvedPath();
    }

    public String getRelativePath() {
        return exchange.getRelativePath();
    }

    public int getStatusCode() {
        return exchange.getStatusCode();
    }

    public HttpServerExchange setStatusCode( int statusCode ) {
        exchange.setStatusCode( statusCode );

        return this;
    }

    public void setStatusCodeReasonPhrase( int statusCode, String message ) {
        setStatusCode( statusCode );
        setReasonPhrase( message );
    }

    public void responseJson( int statusCode, String reasonPhrase, Object body ) {
        setStatusCode( statusCode );
        setReasonPhrase( reasonPhrase );
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public void responseJson( int statusCode, Object body ) {
        setStatusCode( statusCode );
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public void responseJson( Object body ) {
        setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON );
        Binder.json.marshal( body, exchange.getOutputStream() );
    }

    public void responseOk( byte[] content, String contentType ) {
        setStatusCode( HttpStatusCodes.OK );
        setResponseHeader( Headers.CONTENT_TYPE, contentType );
        exchange.getResponseSender().send( ByteBuffer.wrap( content ) );
    }

    public void responseOk( Object content, boolean raw, String contentType ) {
        setStatusCode( HttpStatusCodes.OK );
        setResponseHeader( Headers.CONTENT_TYPE, contentType );
        exchange.getResponseSender().send( contentToString( raw, content, contentType ) );
    }

    public byte[] readBody() throws IOException {
        var ret = new ByteArrayOutputStream();
        IOUtils.copy( getInputStream(), ret );

        return ret.toByteArray();
    }

    public void responseStream( Stream<?> content, boolean raw, String contentType ) {
        setStatusCode( HttpStatusCodes.OK );
        setResponseHeader( Headers.CONTENT_TYPE, contentType );

        var out = exchange.getOutputStream();

        content
            .map( v -> contentToString( raw, v, contentType ) )
            .forEach( Try.consume( v -> {
                out.write( v.getBytes() );
                out.write( '\n' );
            } ) );

        exchange.getResponseSender().send( contentToString( raw, content, contentType ) );
    }

    public HttpServerExchange addQueryParam( String name, String param ) {
        exchange.addQueryParam( name, param );

        return this;
    }

    public InputStream getInputStream() {
        return exchange.getInputStream();
    }

    public OutputStream getOutputStream() {
        return exchange.getOutputStream();
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

    public HttpServerExchange setResponseHeader( HttpString name, String value ) {
        exchange.getResponseHeaders().put( name, value );

        return this;
    }

    public HttpServerExchange setResponseHeader( String name, String value ) {
        exchange.getResponseHeaders().put( new HttpString( name ), value );

        return this;
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public void responseOk( String body, String contentType ) {
        setStatusCode( HttpStatusCodes.OK );
        setResponseHeader( Headers.CONTENT_TYPE, contentType );
        exchange.getResponseSender().send( body );
    }

    public HttpServerExchange setReasonPhrase( String message ) {
        exchange.setReasonPhrase( message );

        return this;
    }

    public boolean isResponseStarted() {
        return exchange.isResponseStarted();
    }

    public HttpServerExchange endExchange() {
        exchange.endExchange();

        return this;
    }

    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        HEAD,
        OPTIONS,
        PATCH
    }
}
