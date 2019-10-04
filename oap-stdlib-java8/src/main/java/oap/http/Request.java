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

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Try;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static oap.util.Pair.__;

public class Request {
    private static final Splitter SPLITTER = Splitter.on( ";" ).trimResults().omitEmptyStrings();
    public final HttpRequest underlying;
    public final Context context;
    public final Optional<InputStream> body;
    public final String uri;
    public final String ua;
    public final String referrer;
    public final String ip;
    @Deprecated
    private ListParams listParams;
    @Deprecated
    private UniqueParams uniqueParams;
    private ListMultimap<String, String> headers;
    private Map<String, String> cookies;
    private String requestLine;
    private ListMultimap<String, String> params;

    public Request( HttpRequest underlying, Context context ) {
        this.underlying = underlying;
        this.context = context;
        this.uri = underlying.getRequestLine().getUri();
        this.body = content( underlying ); // Headers have to be constructed at this point
        this.ua = header( "User-Agent" ).orElse( null );
        this.referrer = header( "Referrer" ).orElse( null );
        this.ip = header( "X-Forwarded-For" ).orElse( context.remoteAddress.getHostAddress() );
    }

    public Optional<String> parameter( String name ) {
        ensureParametersParsed();
        List<String> values = params.get( name );
        if( values.isEmpty() ) return Optional.empty();
        return Optional.ofNullable( values.get( 0 ) );
    }

    public String parameterOrDefault( String name, String def ) {
        ensureParametersParsed();
        List<String> values = params.get( name );
        if( values.isEmpty() ) return def;
        return values.get( 0 );
    }

    public List<String> parameters( String name ) {
        ensureParametersParsed();
        return params.get( name );
    }

    @SneakyThrows
    private void ensureParametersParsed() {
        if( this.params != null ) return;
        this.params = ArrayListMultimap.create();
        Url.parseQuery( Strings.substringAfter( uri, "?" ), this.params );
        val contentType = underlying.getFirstHeader( "Content-Type" );
        if( contentType != null && contentType.getValue().startsWith( "application/x-www-form-urlencoded" )
            && underlying instanceof HttpEntityEnclosingRequest )
            Url.parseQuery( EntityUtils.toString( ( ( HttpEntityEnclosingRequest ) underlying ).getEntity() ), this.params );

    }

    /**
     * @see #parameters(String)
     */
    @Deprecated
    @SneakyThrows
    public ListParams getListParams() {
        if( listParams == null ) {
            listParams = new ListParams();

            Url.parseQuery( Strings.substringAfter( uri, "?" ), listParams.params );
            val contentType = underlying.getFirstHeader( "Content-Type" );
            if( contentType != null && contentType.getValue().startsWith( "application/x-www-form-urlencoded" )
                && underlying instanceof HttpEntityEnclosingRequest )
                Url.parseQuery( EntityUtils.toString( ( ( HttpEntityEnclosingRequest ) underlying ).getEntity() ), listParams.params );
        }

        return listParams;
    }

    /**
     * @apiNote "application/x-www-form-urlencoded" not supported
     * @see #parameter(String)
     * @see #parameterOrDefault(String, String)
     */
    @Deprecated
    public UniqueParams getUniqueParams() {
        if( uniqueParams == null ) {
            uniqueParams = new UniqueParams();
            Url.parseQuery( Strings.substringAfter( uri, "?" ), uniqueParams.params );
        }

        return uniqueParams;
    }

    public ListMultimap<String, String> getHeaders() {
        if( headers == null ) {
            headers = Stream.of( underlying.getAllHeaders() )
                .map( h -> __( h.getName().toLowerCase(), h.getValue() ) )
                .collect( Maps.Collectors.toListMultimap() );
        }
        return headers;
    }

    public Map<String, String> getCookies() {
        if( cookies == null ) {
            cookies = header( "Cookie" )
                .map( cookie -> Stream.of( SPLITTER.split( cookie ).iterator() )
                    .map( s -> Strings.split( s, "=" ) )
                    .collect( Maps.Collectors.<String, String>toMap() ) )
                .orElse( Maps.of() );
        }
        return cookies;
    }

    public HttpMethod getHttpMethod() {
        return HttpMethod.valueOf( underlying.getRequestLine().getMethod().toUpperCase() );
    }

    public String getRequestLine() {
        if( requestLine == null )
            requestLine = Strings.substringBefore( uri, "?" ).substring( context.location.length() );
        return requestLine;
    }

    public String getBaseUrl() {
        return context.protocol.name().toLowerCase() + "://" + header( "Host" ).orElse( "localhost" );
    }

    public boolean isRequestGzipped() {
        List<String> headers = headers( "Content-Encoding" );
        for( String header : headers ) if( header.contains( "gzip" ) ) return true;

        return false;
    }

    @Deprecated
    public boolean isGzipSupport() {
        return gzipSupported();
    }

    public boolean gzipSupported() {
        List<String> headers = headers( "Accept-encoding" );
        for( String header : headers ) if( header.contains( "gzip" ) ) return true;

        return false;
    }

    @SneakyThrows
    private Optional<InputStream> content( HttpRequest underlying ) {
        if( underlying instanceof HttpEntityEnclosingRequest ) {
            InputStream content = ( ( HttpEntityEnclosingRequest ) underlying ).getEntity().getContent();
            return isRequestGzipped()
                ? Optional.of( new GZIPInputStream( content ) )
                : Optional.of( content );
        } else return Optional.empty();
    }

    public Optional<byte[]> readBody() {
        return body.map( Try.mapOrThrow( ByteStreams::toByteArray, HttpException.class ) );
    }

    public Optional<String> header( String name ) {
        return getHeaders().containsKey( name.toLowerCase() ) ? Lists.headOpt( getHeaders().get( name.toLowerCase() ) )
            : Optional.empty();
    }

    public List<String> headers( String name ) {
        return getHeaders().containsKey( name.toLowerCase() ) ? getHeaders().get( name.toLowerCase() ) : Lists.empty();
    }

    public Optional<String> cookie( String name ) {
        return Optional.ofNullable( getCookies().get( name ) );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper( this )
            .add( "baseUrl", getBaseUrl() )
            .add( "requestLine", getRequestLine() )
            .add( "method", getHttpMethod() )
            .add( "params", this.params )
            .omitNullValues()
            .toString();
    }

    public String toDetailedString() {
        return MoreObjects.toStringHelper( this )
            .add( "baseUrl", this.getBaseUrl() )
            .add( "method", this.getHttpMethod() )
            .add( "ip", this.ip )
            .add( "ua", this.ua )
            .add( "uri", this.uri )
            .add( "params", this.params )
            .add( "headers", this.getHeaders() )
            .add( "cookies", getCookies() )
            .omitNullValues()
            .toString();
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH
    }

    @ToString
    @Deprecated
    public static class ListParams {
        final ListMultimap<String, String> params = ArrayListMultimap.create();

        public Optional<String> parameterOpt( String name ) {
            val ret = params.get( name );
            return parameters( name ).stream().findFirst();
        }

        public String parameter( String name ) {
            val ret = params.get( name );
            if( ret == null ) return null;

            return ret.get( 0 );
        }

        public List<String> parameters( String name ) {
            return params.containsKey( name ) ? params.get( name ) : Collections.emptyList();

        }
    }

    @Deprecated
    public static class UniqueParams {
        public final HashMap<String, String> params = new HashMap<>();

        public Optional<String> parameterOpt( String name ) {
            return Optional.of( params.get( name ) );
        }

        public String parameter( String name ) {
            return params.get( name );
        }

        public boolean getBoolean( String name ) {
            val p = params.get( name );
            if( p == null ) return false;
            return Boolean.parseBoolean( p );
        }
    }
}
