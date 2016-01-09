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

import oap.util.Maps;
import oap.util.Pair;
import oap.util.Strings;
import org.apache.http.entity.ContentType;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static oap.util.Pair.__;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class HttpResponse {
    public static final HttpResponse NOT_FOUND = status( HttpURLConnection.HTTP_NOT_FOUND );
    public static final HttpResponse HTTP_FORBIDDEN = status( HttpURLConnection.HTTP_FORBIDDEN );
    public static final HttpResponse NO_CONTENT = status( HttpURLConnection.HTTP_NO_CONTENT );
    public static final HttpResponse NOT_MODIFIED = status( HttpURLConnection.HTTP_NOT_MODIFIED );
    public String reasonPhrase;
    public Object content;
    public List<Pair<String, String>> headers = new ArrayList<>();
    public ContentType contentType;
    public int code;
    boolean raw;
    private InputStream streamContent;

    public HttpResponse( int code ) {
        this.code = code;
    }

    public static HttpResponse redirect( String location ) {
        return new HttpResponse( HttpURLConnection.HTTP_MOVED_TEMP ).withHeader( "Location", location );
    }

    public static HttpResponse ok( Object content, boolean raw, ContentType contentType ) {
        HttpResponse response = ok( content );
        response.raw = raw;
        response.contentType = contentType;
        return response;
    }

    public static HttpResponse ok( Object content ) {
        HttpResponse response = new HttpResponse( HttpURLConnection.HTTP_OK );
        response.content = content;
        response.contentType = ContentType.APPLICATION_JSON;
        return response;
    }

    public static HttpResponse stream( InputStream stream, ContentType contentType ) {
        HttpResponse response = new HttpResponse( HttpURLConnection.HTTP_OK );
        response.streamContent = stream;
        response.contentType = contentType;
        return response;
    }

    public static HttpResponse status( int code, String reason ) {
        HttpResponse response = status( code );
        response.reasonPhrase = Strings.removeControl( reason );
        return response;
    }

    public static HttpResponse status( int code ) {
        HttpResponse response = new HttpResponse( code );
        response.contentType = TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 );
        return response;
    }

    public HttpResponse withHeader( String name, String value ) {
        headers.add( __( name, value ) );
        return this;
    }

    public HttpResponse withContent( String content, ContentType contentType ) {
        this.content = content;
        this.contentType = contentType;
        return this;
    }

    public String content() {
        return raw ? (String) content :
            HttpResponse.producers.getOrDefault( contentType.getMimeType(), String::valueOf ).apply( content );
    }

    public boolean hasContent() {
        return content != null;
    }

    public boolean hasStreamContent() {
        return streamContent != null;
    }

    public InputStream stream() {
        return streamContent;
    }

    private static Map<String, Function<Object, String>> producers = Maps.of(
        __( ContentType.TEXT_PLAIN.getMimeType(), String::valueOf )
    );

    public static void registerProducer(String mimeType, Function<Object, String> producer) {
        producers.put( mimeType, producer );
    }

}
