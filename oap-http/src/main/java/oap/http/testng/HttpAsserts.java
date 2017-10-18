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
import oap.http.Client;
import oap.json.testng.JsonAsserts;
import oap.testng.Env;
import oap.util.Pair;
import org.apache.http.entity.ContentType;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpAsserts {

    private static Client client = Client.custom()
        .onError( ( c, e ) -> System.err.println( e.getMessage() ) )
        .build();

    public static String HTTP_PREFIX() { return "http://localhost:" + Env.port(); }

    public static String HTTP_URL( String suffix ) {
        return "http://localhost:" + Env.port() + ( suffix.startsWith( "/" ) ? suffix : "/" + suffix );
    }

    public static void reset() {
        client.reset();
    }


    @SafeVarargs
    public static HttpAssertion assertGet( String uri, Pair<String, Object>... params ) {
        return new HttpAssertion( client.get( uri, params ) );
    }

    public static HttpAssertion assertGet( String uri, Map<String, Object> params, Map<String, Object> headers ) {
        return new HttpAssertion( client.get( uri, params, headers ) );
    }

    public static HttpAssertion assertPost( String uri, String content, ContentType contentType ) {
        return new HttpAssertion( client.post( uri, content, contentType ) );
    }

    public static HttpAssertion assertPost( String uri, InputStream content, ContentType contentType ) {
        return new HttpAssertion( client.post( uri, content, contentType ) );
    }

    public static HttpAssertion assertUploadFile( String uri, String prefix, Path path ) {
        return new HttpAssertion( client.uploadFile( uri, prefix, path ) );
    }

    public static HttpAssertion assertPut( String uri, String content, ContentType contentType ) {
        return new HttpAssertion( client.put( uri, content, contentType ) );
    }

    public static HttpAssertion assertDelete( String uri ) {
        return new HttpAssertion( client.delete( uri ) );
    }

    @EqualsAndHashCode
    @ToString
    public static class HttpAssertion {
        private final Client.Response response;

        public HttpAssertion( Client.Response response ) {
            this.response = response;
        }

        public HttpAssertion isOk() {
            hasCode( HTTP_OK );
            return this;
        }

        public HttpAssertion hasCode( int code ) {
            assertThat( response.code )
                .as( "check http code (code = {}, reasonPhrase = {}, body = {})", response.code, response.reasonPhrase, response.contentString )
                .isEqualTo( code );
            return this;
        }

        public JsonAsserts.JsonAssertion isJson() {
            hasContentType( ContentType.APPLICATION_JSON );
            return assertJson( response.contentString.orElse( null ) );
        }

        public HttpAssertion isJson( String json ) {
            isJson().isEqualTo( json );
            return this;
        }

        public HttpAssertion hasReason( String reasonPhrase ) {
            assertString( response.reasonPhrase ).isEqualTo( reasonPhrase );
            return this;
        }

        public HttpAssertion hasContentType( ContentType contentType ) {
            assertString( response.contentType
                .map( ContentType::toString )
                .orElse( null ) )
                .isEqualTo( contentType.toString() );
            return this;
        }

        public HttpAssertion hasBody( String body ) {
            assertString( response.contentString.orElse( null ) ).isEqualTo( body );
            return this;
        }

        public HttpAssertion containsHeader( String name, String value ) {
            assertString( response.headers.getOrDefault( name, null ) ).isEqualTo( value );
            return this;
        }

        public HttpAssertion is( Consumer<Client.Response> condition ) {
            condition.accept( response );
            return this;
        }

        public HttpAssertion responded( int code, String reasonPhrase, ContentType contentType, String body ) {
            return this.hasCode( code )
                .hasReason( reasonPhrase )
                .hasContentType( contentType )
                .hasBody( body );
        }
    }
}
