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
package oap.ws.testng;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.io.Closeables;
import oap.json.testng.JsonAsserts;
import oap.testng.Env;
import oap.util.Pair;
import oap.util.Strings;
import oap.ws.Uri;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.testng.Assert.assertEquals;

//todo implement it via oap.ws.apache.SimpleHttpClient
public class HttpAsserts {

    public static final String HTTP_PREFIX = "http://localhost:" + Env.port();

    private static CloseableHttpClient client;

    public synchronized static void reset() {
        Closeables.close( client );
        client = null;
    }

    private static CloseableHttpClient client() {
        if( client == null )
            synchronized( HttpAsserts.class ) {
                if( client == null ) {

                    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
                    cm.setDefaultMaxPerRoute( 100 );
                    cm.setMaxTotal( 200 );

                    client = HttpClients
                        .custom()
                        .setMaxConnPerRoute( 100 )
                        .setMaxConnTotal( 200 )
                        .setConnectionManager( cm )
                        .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
                        .disableRedirectHandling()
                        .setRetryHandler( new DefaultHttpRequestRetryHandler( 2, true ) )
                        .setServiceUnavailableRetryStrategy( new DefaultServiceUnavailableRetryStrategy( 2, 100 ) )
                        .build();
                }
            }
        return client;
    }

    @SafeVarargs
    public static Response get( String url, Pair<String, Object>... params ) {
        return invoke( new HttpGet( Uri.uri( url, params ) ) );
    }

    public static Response post( String url, String requestBody, ContentType contentType ) {
        HttpPost post = new HttpPost( url );
        post.setEntity( new StringEntity( requestBody, contentType ) );
        return invoke( post );
    }

    public static Response post( String url, InputStream requestBody, ContentType contentType ) {
        HttpPost post = new HttpPost( url );
        post.setEntity( new InputStreamEntity( requestBody, contentType ) );
        return invoke( post );
    }

    public static Response put( String url, String requestBody, ContentType contentType ) {
        HttpPut put = new HttpPut( url );
        put.setEntity( new StringEntity( requestBody, contentType ) );
        return invoke( put );
    }

    private static Response invoke( HttpUriRequest http ) {
        try( CloseableHttpResponse response = client().execute( http ) ) {
            if( response.getEntity() != null ) {
                HttpEntity entity = response.getEntity();
                try( InputStream is = entity.getContent() ) {
                    return new Response(
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(),
                        entity.getContentType() != null ?
                            ContentType.parse( entity.getContentType().getValue() ) :
                            null,
                        Strings.readString( is ) );
                }
            } else return new Response(
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase()
            );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @EqualsAndHashCode
    @ToString
    public static class Response {
        public final int code;
        public final String body;
        public final String contentType;
        public final String reasonPhrase;

        public Response( int code, String reasonPhrase, ContentType contentType, String body ) {
            this.code = code;
            this.reasonPhrase = reasonPhrase;
            this.contentType = contentType != null ? contentType.toString() : null;
            this.body = body;
        }

        public Response( int code, String reasonPhrase ) {
            this( code, reasonPhrase, null, null );
        }

        public void assertOk() {
            assertResponse( HTTP_OK );
        }

        public void assertResponse( int code ) {
            assertEquals( this.code, code, "body: " + this.body );
        }

        public void assertResponse( int code, String reasonPhrase, ContentType contentType, String body ) {
            assertEquals( this.reasonPhrase, reasonPhrase );
            assertEquals( this.code, code );
            assertEquals( this.contentType, contentType != null ? contentType.toString() : null );
            if( Objects.equals( ContentType.APPLICATION_JSON.toString(), this.contentType ) )
                JsonAsserts.assertEquals( this.body, body );
            else
                assertEquals( this.body, body );

        }

        public void assertResponse( int code, String reasonPhrase ) {
            assertResponse( code, reasonPhrase, null, null );
        }
    }
}
