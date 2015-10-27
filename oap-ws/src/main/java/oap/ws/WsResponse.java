/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import oap.json.Binder;
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

public class WsResponse {

    public static final WsResponse NOT_FOUND = status( HttpURLConnection.HTTP_NOT_FOUND );
    public static final WsResponse NO_CONTENT = status( HttpURLConnection.HTTP_NO_CONTENT );
    public static final WsResponse NOT_MODIFIED = status( HttpURLConnection.HTTP_NOT_MODIFIED );
    public int code;
    public String reasonPhrase;
    public Object content;
    private InputStream streamContent;
    boolean raw;
    public List<Pair<String, String>> headers = new ArrayList<>();
    public ContentType contentType;

    private WsResponse( int code ) {
        this.code = code;
    }

    public WsResponse withHeader( String name, String value ) {
        headers.add( __( name, value ) );
        return this;
    }

    public static WsResponse redirect( String location ) {
        return new WsResponse( HttpURLConnection.HTTP_MOVED_TEMP ).withHeader( "Location", location );
    }

    public static WsResponse ok( Object content, boolean raw, ContentType contentType ) {
        WsResponse response = ok( content );
        response.raw = raw;
        response.contentType = contentType;
        return response;
    }

    public static WsResponse ok( Object content ) {
        WsResponse response = new WsResponse( HttpURLConnection.HTTP_OK );
        response.content = content;
        response.contentType = ContentType.APPLICATION_JSON;
        return response;
    }

    public static WsResponse stream( InputStream stream, ContentType contentType ) {
        WsResponse response = new WsResponse( HttpURLConnection.HTTP_OK );
        response.streamContent = stream;
        response.contentType = contentType;
        return response;
    }

    public static WsResponse status( int code, String reason ) {
        WsResponse response = status( code );
        response.reasonPhrase = Strings.removeControl( reason );
        return response;
    }

    public static WsResponse status( int code ) {
        WsResponse response = new WsResponse( code );
        response.contentType = TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 );
        return response;
    }

    public WsResponse withContent( String content, ContentType contentType ) {
        this.content = content;
        this.contentType = contentType;
        return this;
    }

    public String content() {
        return raw ? (String) content :
            producers.getOrDefault( contentType.getMimeType(), String::valueOf ).apply( content );
    }

    private static Map<String, Function<Object, String>> producers = Maps.of(
        __( ContentType.TEXT_PLAIN.getMimeType(), String::valueOf ),
        __( ContentType.APPLICATION_JSON.getMimeType(), Binder::marshal )
    );

    public boolean hasContent() {
        return content != null;
    }

    public boolean hasStreamContent() {
        return streamContent != null;
    }

    public InputStream stream() {
        return streamContent;
    }
}
