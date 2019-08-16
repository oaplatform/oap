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

import oap.json.Binder;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class HttpResponse {
    public static final HttpResponse NOT_FOUND = status( HTTP_NOT_FOUND ).response();
    public static final HttpResponse FORBIDDEN = status( HTTP_FORBIDDEN ).response();
    public static final HttpResponse NO_CONTENT = status( HTTP_NO_CONTENT ).response();
    public static final HttpResponse NOT_MODIFIED = status( HTTP_NOT_MODIFIED ).response();
    private static Map<String, Function<Object, String>> producers = Maps.of(
        __( TEXT_PLAIN.getMimeType(), String::valueOf ),
        __( APPLICATION_JSON.getMimeType(), Binder.json::marshal )
    );
    public final int code;
    public final String reason;
    public final List<Pair<String, String>> headers;
    public final List<Pair<String, String>> cookies;
    public final Map<String, Object> session;
    public final HttpEntity contentEntity;

    private HttpResponse( int code, String reason, List<Pair<String, String>> cookies, List<Pair<String, String>> headers, Map<String, Object> session, HttpEntity contentEntity ) {
        this.code = code;
        this.reason = reason;
        this.cookies = cookies;
        this.headers = headers;
        this.session = session;
        this.contentEntity = contentEntity;
    }

    public static Builder redirect( String location ) {
        return status( HTTP_MOVED_TEMP )
            .withHeader( "Location", location );
    }

    public static Builder redirect( URI location ) {
        return redirect( location.toString() );
    }

    public static Builder ok( Object content, boolean raw, ContentType contentType ) {
        return ok( content )
            .withEntity( new StringEntity( content( raw, content, contentType ), contentType ) );
    }

    public static Builder ok( Object content ) {
        return status( HTTP_OK )
            .withEntity( new StringEntity( content( false, content, APPLICATION_JSON ), APPLICATION_JSON ) );
    }

    public static Builder ok( byte[] content, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new ByteArrayEntity( content, contentType ) );
    }

    public static Builder stream( InputStream stream, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new InputStreamEntity( stream, contentType ) );
    }

    public static Builder stream( Stream<String> stream, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new HttpStreamEntity( stream, contentType ) );
    }

    public static Builder stream( Stream<?> stream, boolean raw, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new HttpStreamEntity( stream.map( v -> content( raw, v, contentType ) ), contentType ) );
    }

    public static Builder outputStream( Consumer<OutputStream> consumer, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new HttpOutputStreamEntity( consumer, contentType ) );
    }

    public static Builder gzipOutputStream( Consumer<OutputStream> consumer, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new HttpGzipOutputStreamEntity( consumer, contentType ) );
    }

    public static Builder file( Path file, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new FileEntity( file.toFile(), contentType ) );
    }

    public static Builder bytes( byte[] bytes, ContentType contentType ) {
        return status( HTTP_OK )
            .withEntity( new ByteArrayEntity( bytes, contentType ) );
    }

    public static Builder status( int code, String reason ) {
        return status( code )
            .withReason( Strings.removeControl( reason ) );
    }

    public static Builder status( int code, String reason, Object content ) {
        return status( code, reason )
            .withEntity( new StringEntity( content( false, content, APPLICATION_JSON ), APPLICATION_JSON ) );
    }

    public static Builder status( int code ) {
        return new Builder( code );
    }

    public Builder modify() {
        Builder builder = status( code, reason )
            .withEntity( contentEntity );
        builder.session.putAll( session );
        builder.cookies.addAll( cookies );
        builder.headers.addAll( headers );
        return builder;

    }

    public static void registerProducer( String mimeType, Function<Object, String> producer ) {
        producers.put( mimeType, producer );
    }

    private static String content( boolean raw, Object content, ContentType contentType ) {
        return raw ? ( String ) content
            : HttpResponse.producers.getOrDefault( contentType.getMimeType(), String::valueOf ).apply( content );
    }

    /**
     * @todo what is it for?
     */
    public String getFirstHeader( String name ) {
        return headers.stream().filter( p -> p._1.equals( name ) ).findFirst().map( p -> p._2 ).orElse( null );
    }

    @Override
    public String toString() {
        return "HttpResponse{" + "reason='" + reason + '\'' + ", headers=" + headers + ", code=" + code + '}';
    }

    public static class Builder {
        public String reason;
        public List<Pair<String, String>> headers = new ArrayList<>();
        public List<Pair<String, String>> cookies = new ArrayList<>();
        public Map<String, Object> session = new HashMap<>();
        public int code;
        public HttpEntity contentEntity;

        private Builder( int code ) {
            this.code = code;
        }

        public Builder withHeader( String name, String value ) {
            headers.add( __( name, value ) );
            return this;
        }

        public Builder withCookie( String cookie ) {
            if( StringUtils.isNotBlank( cookie ) ) cookies.add( __( "Set-Cookie", cookie ) );
            return this;
        }

        public Builder withContent( String content, ContentType contentType ) {
            this.contentEntity = new StringEntity( content( true, content, contentType ), contentType );
            return this;
        }

        public Builder withSessionValue( String key, Object value ) {
            session.put( key, value );
            return this;
        }

        /**
         * this method is package private not to expose apache http entity outside the framework.
         */
        Builder withEntity( HttpEntity entity ) {
            this.contentEntity = entity;
            return this;
        }

        public Builder withReason( String reason ) {
            this.reason = reason;
            return this;
        }

        public HttpResponse response() {
            return new HttpResponse( this.code, this.reason, this.cookies, this.headers, this.session, this.contentEntity );
        }
    }

    public static class CookieBuilder {
        private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern( "EEE, dd-MMM-yyyy HH:mm:ss zzz" );
        private String domain;
        private String expires;
        private String path;
        private List<String> values = new ArrayList<>();

        public CookieBuilder withDomain( String domain ) {
            this.domain = StringUtils.isNotBlank( domain ) ? "domain=" + domain : null;
            return this;
        }

        public CookieBuilder withExpires( DateTime expires ) {
            this.expires = expires != null ? "expires=" + FORMATTER.print( expires ) : null;
            return this;
        }

        public CookieBuilder withPath( String path ) {
            this.path = StringUtils.isNotBlank( path ) ? "path=" + path : null;
            return this;
        }

        public CookieBuilder withValue( String name, String value ) {
            if( StringUtils.isNoneBlank( name, value ) ) this.values.add( name + "=" + value );
            return this;
        }

        public CookieBuilder httpOnly() {
            this.values.add( "HttpOnly" );
            return this;
        }

        public String build() {
            return Strings.join( "; ", Lists.of( Strings.join( "; ", values ), domain, expires, path ) );
        }
    }
}
