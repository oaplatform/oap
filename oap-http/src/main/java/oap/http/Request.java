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
import com.google.common.collect.ListMultimap;
import com.google.common.io.ByteStreams;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Try;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static oap.util.Pair.__;

public class Request {
    private static final Splitter SPLITTER = Splitter.on( ";" ).trimResults().omitEmptyStrings();
    public final String requestLine;
    public final HttpMethod httpMethod;
    public final String baseUrl;
    public final Context context;
    public final Optional<InputStream> body;
    public final String uri;
    public final ListMultimap<String, String> headers;
    public final ListMultimap<String, String> params;
    public final String ua;
    public final String referrer;
    public final String ip;
    public final Map<String, String> cookies;

    public Request( HttpRequest req, Context context ) {
        this.headers = Stream.of( req.getAllHeaders() )
            .map( h -> __( h.getName().toLowerCase(), h.getValue() ) )
            .collect( Maps.Collectors.toListMultimap() );
        this.baseUrl = context.protocol.toLowerCase() + "://" + header( "Host" ).orElse( "localhost" );
        this.uri = req.getRequestLine().getUri();
        this.requestLine = Strings.substringBefore( req.getRequestLine().getUri(), "?" ).substring(
            context.location.length() );
        this.httpMethod = HttpMethod.valueOf( req.getRequestLine().getMethod().toUpperCase() );
        this.context = context;
        this.body = content( req ); // Headers have to be constructed at this point
        this.params = params( req );
        this.ua = header( "User-Agent" ).orElse( null );
        this.referrer = header( "Referrer" ).orElse( null );
        this.ip = header( "X-Forwarded-For" ).orElse( context.remoteAddress.getHostAddress() );
        this.cookies = header( "Cookie" )
            .map( cookie -> Stream.of( SPLITTER.split( cookie ).iterator() )
                .map( s -> Strings.split( s, "=" ) )
                .collect( Maps.Collectors.<String, String>toMap() ) )
            .orElse( Maps.empty() );
    }

    public boolean isRequestGzipped() {
        final List<String> headers = headers( "Content-Encoding" );
        if( headers != null ) {
            for( final String header : headers ) {
                if( header.contains( "gzip" ) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private Optional<InputStream> content( HttpRequest req ) {
        try {
            if( req instanceof HttpEntityEnclosingRequest ) {
                final HttpEntityEnclosingRequest enclosingRequest = ( HttpEntityEnclosingRequest ) req;

                final InputStream content = enclosingRequest.getEntity().getContent();

                return isRequestGzipped()
                    ? Optional.of( new GZIPInputStream( content ) )
                    : Optional.of( content );
            } else {
                return Optional.empty();
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private static ListMultimap<String, String> params( HttpRequest req ) {
        ListMultimap<String, String> query = Url.parseQuery(
            Strings.substringAfter( req.getRequestLine().getUri(), "?" ) );
        if( req.getFirstHeader( "Content-Type" ) != null
            && req.getFirstHeader( "Content-Type" ).getValue().startsWith( "application/x-www-form-urlencoded" )
            && req instanceof HttpEntityEnclosingRequest )
            try {
                return Maps.add( query, Url.parseQuery(
                    EntityUtils.toString( ( ( HttpEntityEnclosingRequest ) req ).getEntity() ) ) );
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        else return query;
    }

    public Optional<String> parameter( String name ) {
        return parameters( name ).stream().findFirst();
    }

    public List<String> parameters( String name ) {
        return params.containsKey( name ) ? params.get( name ) : Collections.emptyList();

    }

    public Optional<byte[]> readBody() {
        return body.map( Try.mapOrThrow( ByteStreams::toByteArray, HttpException.class ) );
    }

    public Optional<String> header( String name ) {
        return headers.containsKey( name.toLowerCase() ) ? Lists.headOpt( headers.get( name.toLowerCase() ) )
            : Optional.empty();
    }

    public List<String> headers( String name ) {
        return headers.containsKey( name.toLowerCase() ) ? headers.get( name.toLowerCase() ) : Lists.empty();
    }

    public Optional<String> cookie( String name ) {
        return Optional.ofNullable( cookies.get( name ) );
    }

    @Deprecated
    /**
     * @see #cookie(String)
     * */
    public Map<String, String> cookies() {
        return cookies;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper( this )
            .add( "baseUrl", baseUrl )
            .add( "requestLine", requestLine )
            .add( "method", httpMethod )
            .add( "params", params )
            .omitNullValues()
            .toString();
    }

    public String toDetailedString() {
        return MoreObjects.toStringHelper( this )
            .add( "baseUrl", this.baseUrl )
            .add( "method", this.httpMethod )
            .add( "ip", this.ip )
            .add( "ua", this.ua )
            .add( "uri", this.uri )
            .add( "params", this.params )
            .add( "headers", this.headers )
            .add( "cookies", this.cookies )
            .omitNullValues()
            .toString();
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS
    }

}
