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
import lombok.ToString;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static oap.util.Pair.__;

public class Request {
    private static final Splitter SPLITTER = Splitter.on( ";" ).trimResults().omitEmptyStrings();
    public final HttpRequest req;
    public final Context context;
    public final Optional<InputStream> body;
    public final String uri;
    public final String ua;
    public final String referrer;
    public final String ip;
    private ListParams listParams;
    private UniqueParams uniqueParams;
    private ListMultimap<String, String> _headers;
    private Map<String, String> _cookies;
    private String _requestLine;

    public Request( HttpRequest req, Context context ) {
        this.req = req;
        this.context = context;
        this.uri = req.getRequestLine().getUri();
        this.body = content( req ); // Headers have to be constructed at this point
        this.ua = header( "User-Agent" ).orElse( null );
        this.referrer = header( "Referrer" ).orElse( null );
        this.ip = header( "X-Forwarded-For" ).orElse( context.remoteAddress.getHostAddress() );
    }

    public ListParams getListParams() {
        if( listParams == null ) {
            listParams = new ListParams();

            Url.parseQuery( Strings.substringAfter( uri, "?" ), listParams.params );
            var contentType = req.getFirstHeader( "Content-Type" );
            if( contentType != null && contentType.getValue().startsWith( "application/x-www-form-urlencoded" )
                && req instanceof HttpEntityEnclosingRequest )
                try {
                    Url.parseQuery( EntityUtils.toString( ( ( HttpEntityEnclosingRequest ) req ).getEntity() ), listParams.params );
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
        }

        return listParams;
    }

    /**
     * @apiNote "application/x-www-form-urlencoded" not supported
     */
    public UniqueParams getUniqueParams() {
        if( uniqueParams == null ) {
            uniqueParams = new UniqueParams();
            Url.parseQuery( Strings.substringAfter( uri, "?" ), uniqueParams.params );
        }

        return uniqueParams;
    }

    public ListMultimap<String, String> getHeaders() {
        if( _headers == null ) {
            _headers = Stream.of( req.getAllHeaders() )
                .map( h -> __( h.getName().toLowerCase(), h.getValue() ) )
                .collect( Maps.Collectors.toListMultimap() );
        }
        return _headers;
    }

    public Map<String, String> getCookies() {
        if( _cookies == null ) {
            _cookies = header( "Cookie" )
                .map( cookie -> Stream.of( SPLITTER.split( cookie ).iterator() )
                    .map( s -> Strings.split( s, "=" ) )
                    .collect( Maps.Collectors.<String, String>toMap() ) )
                .orElse( Map.of() );
        }
        return _cookies;
    }

    public HttpMethod getHttpMethod() {
        return HttpMethod.valueOf( req.getRequestLine().getMethod().toUpperCase() );
    }

    public String getRequestLine() {
        if( _requestLine == null ) {
            _requestLine = Strings.substringBefore( uri, "?" ).substring( context.location.length() );
        }
        return _requestLine;
    }

    public String getBaseUrl() {
        return context.protocol.name().toLowerCase() + "://" + header( "Host" ).orElse( "localhost" );
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

    public boolean isGzipSupport() {
        final List<String> headers = headers( "Accept-encoding" );
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
            .add( "params", getListParams() )
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
            .add( "params", this.getListParams() )
            .add( "headers", this.getHeaders() )
            .add( "cookies", getCookies() )
            .omitNullValues()
            .toString();
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH
    }

    @ToString
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

    public static class UniqueParams {
        public final HashMap<String, String> params = new HashMap<>();

        public Optional<String> parameterOpt( String name ) {
            return Optional.of( params.get( name ) );
        }

        public String parameter( String name ) {
            return params.get( name );
        }
    }
}
