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

import com.google.common.base.Joiner;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class HttpResponse {
    public static final HttpResponse NOT_FOUND = status( HttpURLConnection.HTTP_NOT_FOUND );
    public static final HttpResponse HTTP_FORBIDDEN = status( HttpURLConnection.HTTP_FORBIDDEN );
    public static final HttpResponse NO_CONTENT = status( HttpURLConnection.HTTP_NO_CONTENT );
    public static final HttpResponse NOT_MODIFIED = status( HttpURLConnection.HTTP_NOT_MODIFIED );
    private static Map<String, Function<Object, String>> producers = Maps.of(
        __( ContentType.TEXT_PLAIN.getMimeType(), String::valueOf )
    );
    public String reasonPhrase;
    public List<Pair<String, String>> headers = new ArrayList<>();
    public int code;
    public HttpEntity contentEntity;

    public HttpResponse( int code ) {
        this.code = code;
    }

    public static HttpResponse redirect( String location ) {
        return new HttpResponse( HTTP_MOVED_TEMP ).withHeader( "Location", location );
    }

    public static HttpResponse ok( Object content, boolean raw, ContentType contentType ) {
        HttpResponse response = ok( content );
        response.contentEntity = new StringEntity( content( raw, content, contentType ), contentType );
        return response;
    }

    public static HttpResponse ok( Object content ) {
        HttpResponse response = new HttpResponse( HTTP_OK );
        response.contentEntity = new StringEntity( content( false, content, APPLICATION_JSON ), APPLICATION_JSON );
        return response;
    }

    public static HttpResponse stream( InputStream stream, ContentType contentType ) {
        HttpResponse response = new HttpResponse( HTTP_OK );
        response.contentEntity = new InputStreamEntity( stream, contentType );
        return response;
    }

    public static HttpResponse stream( Stream<String> stream, ContentType contentType ) {
        HttpResponse response = new HttpResponse( HTTP_OK );
        response.contentEntity = new HttpStreamEntity( stream, contentType );
        return response;
    }

    public static HttpResponse file( Path file, ContentType contentType ) {
        HttpResponse response = new HttpResponse( HTTP_OK );
        response.contentEntity = new FileEntity( file.toFile(), contentType );
        return response;
    }

    public static HttpResponse bytes( byte[] bytes, ContentType contentType ) {
        HttpResponse response = new HttpResponse( HTTP_OK );
        response.contentEntity = new ByteArrayEntity( bytes, contentType );
        return response;
    }

    public static HttpResponse status( int code, String reason ) {
        HttpResponse response = status( code );
        response.reasonPhrase = Strings.removeControl( reason );
        return response;
    }

    public static HttpResponse status( int code ) {
        return new HttpResponse( code );
    }

    public static void registerProducer( String mimeType, Function<Object, String> producer ) {
        producers.put( mimeType, producer );
    }

    private static String content( boolean raw, Object content, ContentType contentType ) {
        return raw ? ( String ) content :
            HttpResponse.producers.getOrDefault( contentType.getMimeType(), String::valueOf ).apply( content );
    }

    public HttpResponse withHeader( String name, String value ) {
        headers.add( __( name, value ) );
        return this;
    }

    public HttpResponse withCookie( String cookie ) {
        if( StringUtils.isNotBlank( cookie ) ) {
            headers.add( __( "Set-Cookie", cookie ) );
        }
        return this;
    }

    public HttpResponse withContent( String content, ContentType contentType ) {
        this.contentEntity = new StringEntity( content( false, content, contentType ), contentType );
        return this;
    }

    public static class CookieBuilder {
        private static final Joiner JOINER = Joiner.on( ";" ).skipNulls();
        private static final DateTimeFormatter FORMATTER =
            DateTimeFormat.forPattern( "EEE, dd-MMM-yyyy HH:mm:ss zzz" );
        private String SID;
        private String domain;
        private String expires;
        private String path;
        private List<String> customs = new ArrayList<>();

        public CookieBuilder withSID( String SID ) {
            this.SID = StringUtils.isNotBlank( SID ) ? "SID=" + SID : null;
            return this;
        }

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

        public CookieBuilder withCustomValue( String name, String value ) {
            if( StringUtils.isNoneBlank( name, value ) ) {
                this.customs.add( name + "=" + value );
            }
            return this;
        }

        public String build() {
            return JOINER.join( SID, domain, expires, path, Strings.join( ";", customs ) ).concat( ";" );
        }

    }
}
