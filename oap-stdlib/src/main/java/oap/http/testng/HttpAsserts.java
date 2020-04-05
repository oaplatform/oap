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
import lombok.extern.slf4j.Slf4j;
import oap.http.Client;
import oap.http.Cookie;
import oap.json.testng.JsonAsserts;
import oap.testng.Env;
import oap.util.BiStream;
import oap.util.Pair;
import oap.util.Stream;
import org.apache.http.entity.ContentType;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.testng.HttpAsserts.HttpAssertion.assertHttpResponse;
import static oap.http.testng.HttpAsserts.JsonHttpAssertion.assertJsonResponse;
import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class HttpAsserts {

    private static Client client = Client.custom()
        .onError( ( c, e ) -> log.error( e.getMessage() ) )
        .build();

    @Deprecated
    public static String HTTP_PREFIX() {
        return httpPrefix();
    }

    public static String httpPrefix() {
        return "http://localhost:" + Env.port();
    }

    @Deprecated
    public static String HTTP_URL( String suffix ) {
        return httpUrl( suffix );
    }

    public static String httpUrl( String suffix ) {
        return httpPrefix() + ( suffix.startsWith( "/" ) ? suffix : "/" + suffix );
    }

    public static void reset() {
        client.reset();
    }


    @SafeVarargs
    public static HttpAssertion assertGet( String uri, Pair<String, Object>... params ) {
        return new HttpAssertion( client.get( uri, params ) );
    }

    public static HttpAssertion assertGet( String uri, Map<String, Object> params, Map<String, Object> headers ) {
        return assertHttpResponse( client.get( uri, params, headers ) );
    }

    public static HttpAssertion assertPost( String uri, String content, Map<String, Object> headers ) {
        return assertPost( uri, content, APPLICATION_JSON, headers );
    }

    public static HttpAssertion assertPost( String uri, String content ) {
        return assertPost( uri, content, Map.of() );
    }

    public static HttpAssertion assertPost( String uri, String content, ContentType contentType, Map<String, Object> headers ) {
        return assertHttpResponse( client.post( uri, content, contentType, headers ) );
    }

    public static HttpAssertion assertPost( String uri, String content, ContentType contentType ) {
        return assertPost( uri, content, contentType, Map.of() );
    }

    public static HttpAssertion assertPost( String uri, InputStream content, ContentType contentType ) {
        return assertHttpResponse( client.post( uri, content, contentType ) );
    }

//    public static HttpAssertion assertUploadFile( String uri, String prefix, Path path ) {
//        return new HttpAssertion( client.uploadFile( uri, prefix, path ) );
//    }

    public static HttpAssertion assertPut( String uri, String content, ContentType contentType ) {
        return assertHttpResponse( client.put( uri, content, contentType ) );
    }

    public static HttpAssertion assertDelete( String uri ) {
        return assertHttpResponse( client.delete( uri ) );
    }

    @EqualsAndHashCode
    @ToString
    public static class HttpAssertion {
        private final Client.Response response;

        private HttpAssertion( Client.Response response ) {
            this.response = response;
        }

        public static HttpAssertion assertHttpResponse( Client.Response response ) {
            return new HttpAssertion( response );
        }

        public HttpAssertion isOk() {
            hasCode( HTTP_OK );
            return this;
        }

        public HttpAssertion hasCode( int code ) {
            assertThat( response.code )
                .as( "check http code (code = %s, reason = %s, body = %s)", response.code, response.reasonPhrase, response.contentString() )
                .isEqualTo( code );
            return this;
        }

        @Deprecated
        public JsonAsserts.JsonAssertion isJson() {
            assertJsonResponse( response );
            return assertJson( response.contentString() );
        }

        public HttpAssertion isJson( String json ) {
            isJson().isStructurallyEqualTo( json );
            return this;
        }

        public HttpAssertion hasReason( String reasonPhrase ) {
            assertString( response.reasonPhrase ).isEqualTo( reasonPhrase );
            return this;
        }

        public HttpAssertion hasContentType( ContentType contentType ) {
            assertString( response.contentType.toString() ).isEqualTo( contentType.toString() );
            return this;
        }

        public HttpAssertion hasBody( String body ) {
            assertString( response.contentString() ).isEqualTo( body );
            return this;
        }

        public HttpAssertion containsHeader( String name, String value ) {
            assertString( response.header( name ).orElse( null ) ).isEqualTo( value );
            return this;
        }

        public HttpAssertion containsCookie( String name, Consumer<Cookie> assertion ) {
            assertThat( Stream.of( cookies() ).filter( c -> c.name.equalsIgnoreCase( name ) ).findAny() )
                .isNotEmpty()
                .withFailMessage( "no such cookie: " + name )
                .get()
                .satisfies( assertion );
            return this;
        }

        public HttpAssertion containsCookie( Cookie cookie ) {
            assertThat( cookies() ).contains( cookie );
            return this;
        }

        public HttpAssertion containsCookie( String cookie ) {
            return containsCookie( Cookie.parse( cookie ) );
        }

        protected List<Cookie> cookies() {
            return BiStream.of( response.headers )
                .filter( ( name, value ) -> "Set-Cookie".equalsIgnoreCase( name ) )
                .mapToObj( ( name, value ) -> Cookie.parse( value ) )
                .toList();
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

        public HttpAssertion respondedJson( int code, String reasonPhrase, String body ) {
            assertJsonResponse( response )
                .isEqualTo( code, reasonPhrase, body );
            return this;
        }

        public HttpAssertion respondedJson( String json ) {
            return this.respondedJson( HTTP_OK, "OK", json );
        }

        public HttpAssertion respondedJson( Class<?> contextClass, String resource ) {
            return this.respondedJson( contentOfTestResource( contextClass, resource ) );
        }

        public HttpAssertion respondedJson( Class<?> contextClass, String resource, Map<String, Object> substitutions ) {
            return this.respondedJson( contentOfTestResource( contextClass, resource, substitutions ) );
        }

        public HttpAssertion satisfies( Consumer<Client.Response> assertion ) {
            assertion.accept( response );
            return this;
        }
    }

    public static class CookieHttpAssertion {
        private Cookie cookie;

        private CookieHttpAssertion( Cookie cookie ) {
            this.cookie = cookie;
        }

        public static CookieHttpAssertion assertCookie( Cookie cookie ) {
            return new CookieHttpAssertion( cookie );
        }

        public CookieHttpAssertion hasValue( String value ) {
            assertString( cookie.value ).isEqualTo( value );
            return this;
        }

        public CookieHttpAssertion hasValue( Object value ) {
            return hasValue( String.valueOf( value ) );
        }

        public CookieHttpAssertion hasDomain( String domain ) {
            assertString( cookie.domain ).isEqualTo( domain );
            return this;
        }

        public CookieHttpAssertion expiresAt( DateTime expiration ) {
            assertThat( cookie.expires ).isEqualTo( expiration );
            return this;
        }

        public CookieHttpAssertion expiresAfter( DateTime expiration ) {
            assertThat( cookie.expires ).isGreaterThanOrEqualTo( expiration );
            return this;
        }

        public CookieHttpAssertion hasPath( String path ) {
            assertString( cookie.path ).isEqualTo( path );
            return this;
        }

        public CookieHttpAssertion hasNotMaxAge() {
            return hasMaxAge( Cookie.NO_MAX_AGE );
        }

        public CookieHttpAssertion hasMaxAge( long maxAge ) {
            assertThat( cookie.maxAge ).isEqualTo( maxAge );
            return this;
        }

        public CookieHttpAssertion hasSameSite( Cookie.SameSite sameSite ) {
            assertThat( cookie.sameSite ).isEqualTo( sameSite );
            return this;
        }

        public CookieHttpAssertion isSecure() {
            assertThat( cookie.secure ).isTrue();
            return this;
        }

        public CookieHttpAssertion isNotSecure() {
            assertThat( cookie.secure ).isFalse();
            return this;
        }

        public CookieHttpAssertion isHttpOnly() {
            assertThat( cookie.httpOnly ).isTrue();
            return this;
        }

        public CookieHttpAssertion isNotHttpOnly() {
            assertThat( cookie.httpOnly ).isFalse();
            return this;
        }

    }

    public static class JsonHttpAssertion {
        private Client.Response response;

        private JsonHttpAssertion( Client.Response response ) {
            this.response = response;
        }

        public static JsonHttpAssertion assertJsonResponse( Client.Response response ) {
            assertHttpResponse( response )
                .hasContentType( APPLICATION_JSON );
            return new JsonHttpAssertion( response );
        }

        public JsonHttpAssertion isEqualTo( int code, String reasonPhrase, String body ) {
            assertHttpResponse( response )
                .hasCode( code )
                .hasReason( reasonPhrase );
            isJson().isStructurallyEqualTo( body );
            return this;
        }

        public JsonHttpAssertion isEqualTo( String json ) {
            return this.isEqualTo( HTTP_OK, "OK", json );
        }

        public JsonHttpAssertion isEqualTo( Class<?> contextClass, String resource ) {
            return this.isEqualTo( contentOfTestResource( contextClass, resource ) );
        }

        public JsonAsserts.JsonAssertion isJson() {
            return assertJson( response.contentString() );
        }
    }
}
