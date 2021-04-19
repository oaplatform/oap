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
import oap.util.Lists;
import oap.util.Strings;
import oap.util.function.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

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

    public String parameter( String name, String def ) {
        ensureParametersParsed();
        return Lists.headOf( params.get( name ) ).orElse( def );
    }

    @Deprecated
    public String parameterOrDefault( String name, String def ) {
        return parameter( name, def );
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
        var contentType = underlying.getFirstHeader( "Content-Type" );
        if( contentType != null && contentType.getValue().startsWith( "application/x-www-form-urlencoded" )
            && underlying instanceof HttpEntityEnclosingRequest )
            Url.parseQuery( EntityUtils.toString( ( ( HttpEntityEnclosingRequest ) underlying ).getEntity() ), this.params );

    }

    /**
     * Gets the entity's content type. This content type will be used as the value for the "Content-Type" header
     * @return the entity's content type. null if absent
     * */
    public ContentType getContentType() {
        var header = underlying.getFirstHeader( "Content-Type" );
        return header != null
            ? ContentType.parse( header.getValue() )
            : null;
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
            var contentType = underlying.getFirstHeader( "Content-Type" );
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
            headers = ArrayListMultimap.create();

            for( var h : underlying.getAllHeaders() )
                headers.put( h.getName().toLowerCase(), h.getValue() );
        }
        return headers;
    }

    public Map<String, String> getCookies() {
        if( cookies == null ) {
            cookies = new HashMap<>();

            header( "Cookie" ).ifPresent( header -> {
                for( var cookie : SPLITTER.split( header ) ) {
                    var p = StringUtils.splitByWholeSeparatorPreserveAllTokens( cookie, "=", 2 );
                    if( p.length > 1 ) cookies.put( p[0], p[1] );
                    else cookies.put( p[0], null );
                }
            } );
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

    public void skipBody() {
        body.ifPresent( Try.consume( s -> IOUtils.skip( s, Long.MAX_VALUE ) ) );
    }

    public Optional<byte[]> readBody() {
        return body.map( Try.map( ByteStreams::toByteArray ) );
    }

    public Optional<String> header( String name ) {
        ListMultimap<String, String> headers = getHeaders();
        String normalisedName = name.toLowerCase();
        return headers.containsKey( normalisedName )
            ? Lists.headOf( headers.get( normalisedName ) )
            : Optional.empty();
    }

    /**
     * use optional one
     */
    @Deprecated
    public String header2( String name ) {
        return header( name.toLowerCase() ).orElse( null );
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
            var ret = params.get( name );
            return parameters( name ).stream().findFirst();
        }

        public String parameter( String name ) {
            var ret = params.get( name );
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
            var p = params.get( name );
            if( p == null ) return false;
            return Boolean.parseBoolean( p );
        }
    }
}
