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
package oap.http.testng;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.SimpleHttpClient;
import oap.http.Uri;
import oap.io.Resources;
import oap.json.testng.JsonAsserts;
import oap.testng.Env;
import oap.util.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.testng.Assert.assertEquals;

public class HttpAsserts {

    public static final String HTTP_PREFIX = "http://localhost:" + Env.port();

    public static void reset() {
        SimpleHttpClient.reset();
    }


    @SafeVarargs
    public static ResponseAssert get( String url, Pair<String, Object>... params ) {
        return invoke( new HttpGet( Uri.uri( url, params ) ) );
    }

    public static ResponseAssert post( String url, String requestBody, ContentType contentType ) {
        HttpPost post = new HttpPost( url );
        post.setEntity( new StringEntity( requestBody, contentType ) );
        return invoke( post );
    }

    public static ResponseAssert post( String url, InputStream requestBody, ContentType contentType ) {
        HttpPost post = new HttpPost( url );
        post.setEntity( new InputStreamEntity( requestBody, contentType ) );
        return invoke( post );
    }

    public static ResponseAssert put( String url, String requestBody, ContentType contentType ) {
        HttpPut put = new HttpPut( url );
        put.setEntity( new StringEntity( requestBody, contentType ) );
        return invoke( put );
    }

    private static ResponseAssert invoke( HttpUriRequest http ) {
        return new ResponseAssert( SimpleHttpClient.execute( http ) );
    }

    @EqualsAndHashCode
    @ToString
    public static class ResponseAssert {
        public final SimpleHttpClient.Response response;

        public ResponseAssert( SimpleHttpClient.Response response ) {
            this.response = response;
        }

        public void assertOk() {
            assertResponse( HTTP_OK );
        }

        public void assertResponse( int code ) {
            assertEquals( response.code, code, "body: " + response.body );
        }

        public void assertResponse( int code, String reasonPhrase, ContentType contentType, String body ) {
            assertEquals( response.reasonPhrase, reasonPhrase );
            assertEquals( response.code, code );
            assertEquals( response.contentType, contentType != null ? contentType.toString() : null );
            if( Objects.equals( ContentType.APPLICATION_JSON.toString(), response.contentType ) )
                JsonAsserts.assertEquals( response.body, body );
            else
                assertEquals( response.body, body );
        }

        public void assertResponse( int code, String reasonPhrase, ContentType contentType, Class<?> context,
            String resourcePath ) {
            assertResponse( code, reasonPhrase, contentType,
                Resources.readString( context, context.getSimpleName() + "/" + resourcePath ).get() );
        }

        public void assertResponse( int code, String reasonPhrase ) {
            assertResponse( code, reasonPhrase, null, null );
        }

        public void assertContent( int code, Consumer<String> assertor ) {
            assertEquals( response.code, code );
            assertor.accept( response.body );
        }
    }
}
