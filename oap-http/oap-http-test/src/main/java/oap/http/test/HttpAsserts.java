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
package oap.http.test;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.slf4j.Slf4j;
import oap.http.Cookie;
import oap.http.Response;
import oap.http.client.JettyRequestExtensions;
import oap.http.client.OapHttpClient;
import oap.http.test.cookies.MockHttpCookieStorage;
import oap.json.JsonException;
import oap.json.testng.JsonAsserts;
import oap.testng.Asserts;
import oap.util.BiStream;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Stream;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.InputStreamRequestContent;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.joda.time.DateTime;
import org.testng.internal.collections.Ints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Http.ContentType.APPLICATION_JSON;
import static oap.http.Http.ContentType.APPLICATION_OCTET_STREAM;
import static oap.http.test.HttpAsserts.HttpAssertion.assertHttpResponse;
import static oap.http.test.HttpAsserts.JsonHttpAssertion.assertJsonResponse;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

@ExtensionMethod( JettyRequestExtensions.class )
@Slf4j
@SuppressWarnings( "unused" )
public class HttpAsserts {
    public static final HttpClient TEST_HTTP_CLIENT = OapHttpClient.customHttpClient().cookieStore( new MockHttpCookieStorage() ).build();

    public static String httpPrefix( int port ) {
        return "http://localhost:" + port;
    }

    public static String httpUrl( int port, String suffix ) {
        return httpPrefix( port ) + ( suffix.startsWith( "/" ) ? suffix : "/" + suffix );
    }

    @SafeVarargs
    public static HttpAssertion assertGet( String uri, Pair<String, Object>... params ) throws UncheckedIOException {
        return assertGet( uri, Maps.of( params ), Map.of() );
    }

    public static HttpAssertion assertGet( String uri, Map<String, Object> params, Map<String, Object> headers ) throws UncheckedIOException {
        return assertGet( TEST_HTTP_CLIENT, uri, params, headers );
    }

    public static HttpAssertion assertGet( org.eclipse.jetty.client.HttpClient client, String uri, Map<String, Object> params, Map<String, Object> headers ) throws UncheckedIOException {
        return getResponseAsHttpAssertion( client
            .newRequest( uri )
            .method( HttpMethod.GET )
            .addParams( params )
            .addHeaders( headers ) );
    }

    public static HttpAssertion assertPost( String uri, InputStream content, @Nullable String contentType, Map<String, Object> headers ) {
        return assertPost( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPost( org.eclipse.jetty.client.HttpClient httpClient, String uri, InputStream content, @Nullable String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.POST )
            .addHeaders( headers )
            .body( new InputStreamRequestContent( contentType, content, null ) ) );
    }

    public static HttpAssertion assertPost( String uri, InputStream content, @Nullable String contentType ) {
        return assertPost( uri, content, contentType, Maps.of() );
    }

    public static HttpAssertion assertPost( String uri, byte[] content, @Nullable String contentType, Map<String, Object> headers ) {
        return assertPost( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPost( org.eclipse.jetty.client.HttpClient httpClient, String uri, byte[] content, @Nullable String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.POST )
            .addHeaders( headers )
            .body( new BytesRequestContent( contentType, content ) ) );
    }

    public static HttpAssertion assertPost( String uri, String content, @Nullable String contentType, Map<String, Object> headers ) {
        return assertPost( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPost( org.eclipse.jetty.client.HttpClient httpClient, String uri, String content, @Nullable String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.POST )
            .addHeaders( headers )
            .body( new StringRequestContent( contentType, content ) ) );
    }

    public static HttpAssertion assertPost( String uri, String content ) {
        return assertPost( uri, content, null, Maps.of() );
    }

    public static HttpAssertion assertPost( String uri, String content, Map<String, Object> headers ) {
        return assertPost( uri, content, null, headers );
    }

    public static HttpAssertion assertPost( String uri, String content, String contentType ) {
        return assertPost( uri, content, contentType, Maps.of() );
    }

    @SneakyThrows
    private static @Nonnull HttpAssertion getResponseAsHttpAssertion( org.eclipse.jetty.client.Request request ) {
        ContentResponse contentResponse = request
            .timeout( 10, TimeUnit.SECONDS )
            .send();

        String mediaType = contentResponse.getMediaType();

        ArrayList<Pair<String, String>> headers = new ArrayList<>();
        HttpFields responseHeaders = contentResponse.getHeaders();
        responseHeaders.forEach( field -> {
            HttpHeader header = field.getHeader();
            headers.add( __( field.getName(), field.getValue() ) );
        } );

        return new HttpAssertion( new Response(
            request.getURI().toString(),
            contentResponse.getStatus(), contentResponse.getReason(), headers, mediaType != null ? mediaType : APPLICATION_OCTET_STREAM, new ByteArrayInputStream( contentResponse.getContent() ) ) );
    }

    public static HttpAssertion assertPut( String uri, String content, String contentType ) {
        return assertPut( uri, content, contentType, Maps.of() );
    }

    public static HttpAssertion assertPut( String uri, String content, String contentType, Map<String, Object> headers ) {
        return assertPut( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPut( org.eclipse.jetty.client.HttpClient httpClient, String uri, String content, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PUT )
            .addHeaders( headers )
            .body( new StringRequestContent( contentType, content ) )
        );
    }

    public static HttpAssertion assertPut( String uri, byte[] content, String contentType ) {
        return assertPut( uri, content, contentType, Maps.of() );
    }

    public static HttpAssertion assertPut( String uri, byte[] content, String contentType, Map<String, Object> headers ) {
        return assertPut( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPut( org.eclipse.jetty.client.HttpClient httpClient, String uri, byte[] content, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PUT )
            .addHeaders( headers )
            .body( new BytesRequestContent( contentType, content ) )
        );
    }

    public static HttpAssertion assertPut( String uri, InputStream is, String contentType ) {
        return assertPut( uri, is, contentType, Maps.of() );
    }

    public static HttpAssertion assertPut( String uri, InputStream is, String contentType, Map<String, Object> headers ) {
        return assertPut( TEST_HTTP_CLIENT, uri, is, contentType, headers );
    }

    public static HttpAssertion assertPut( org.eclipse.jetty.client.HttpClient httpClient, String uri, InputStream is, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PUT )
            .addHeaders( headers )
            .body( new InputStreamRequestContent( contentType, is, null ) )
        );
    }

    public static HttpAssertion assertPatch( String uri, byte[] content, String contentType ) {
        return assertPatch( uri, content, contentType, Maps.of() );
    }

    public static HttpAssertion assertPatch( String uri, byte[] content, String contentType, Map<String, Object> headers ) {
        return assertPatch( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPatch( org.eclipse.jetty.client.HttpClient httpClient, String uri, byte[] content, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PATCH )
            .addHeaders( headers )
            .body( new BytesRequestContent( contentType, content ) )
        );
    }

    public static HttpAssertion assertPatch( String uri, String content, String contentType ) {
        return assertPatch( uri, content, contentType, Maps.of() );
    }

    public static HttpAssertion assertPatch( String uri, String content, String contentType, Map<String, Object> headers ) {
        return assertPatch( TEST_HTTP_CLIENT, uri, content, contentType, headers );
    }

    public static HttpAssertion assertPatch( org.eclipse.jetty.client.HttpClient httpClient, String uri, String content, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PATCH )
            .addHeaders( headers )
            .body( new StringRequestContent( contentType, content ) )
        );
    }

    public static HttpAssertion assertPatch( String uri, InputStream is, String contentType ) {
        return assertPatch( uri, is, contentType, Maps.of() );
    }


    public static HttpAssertion assertPatch( String uri, InputStream is, String contentType, Map<String, Object> headers ) {
        return assertPatch( TEST_HTTP_CLIENT, uri, is, contentType, headers );
    }

    public static HttpAssertion assertPatch( org.eclipse.jetty.client.HttpClient httpClient, String uri, InputStream is, String contentType, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.PATCH )
            .addHeaders( headers )
            .body( new InputStreamRequestContent( contentType, is, null ) )
        );
    }

    public static HttpAssertion assertDelete( String uri, Map<String, Object> headers ) {
        return assertDelete( TEST_HTTP_CLIENT, uri, headers );
    }

    public static HttpAssertion assertDelete( org.eclipse.jetty.client.HttpClient httpClient, String uri, Map<String, Object> headers ) {
        return getResponseAsHttpAssertion( httpClient
            .newRequest( uri )
            .method( HttpMethod.DELETE )
            .addHeaders( headers )
        );
    }

    public static HttpAssertion assertDelete( String uri ) {
        return assertDelete( uri, Map.of() );
    }

    @EqualsAndHashCode
    @ToString
    public static final class HttpAssertion {
        private final Response response;

        private HttpAssertion( Response response ) {
            this.response = response;
        }

        public static HttpAssertion assertHttpResponse( Response response ) {
            return new HttpAssertion( response );
        }

        public HttpAssertion isOk() {
            hasCode( HTTP_OK );
            return this;
        }

        public HttpAssertion hasCode( int code ) {
            assertThat( response.code )
                .as( "check http code (code = %s, reason = %s, body = %s)", response.code, response.reasonPhrase, response.contentString() )
                .isIn( code );
            return this;
        }

        public HttpAssertion codeIsIn( int... code ) {
            assertThat( response.code )
                .as( "check http code (code = %s, reason = %s, body = %s)", response.code, response.reasonPhrase, response.contentString() )
                .isIn( Ints.asList( code ) );
            return this;
        }

        @Deprecated
        public JsonAsserts.JsonAssertion isJson() {
            assertJsonResponse( response );
            return JsonAsserts.assertJson( response.contentString() );
        }

        @Deprecated
        public HttpAssertion isJson( String json ) {
            assertJsonResponse( response )
                .assertJson()
                .isEqualTo( json );
            return this;
        }

        public HttpAssertion hasReason( String reasonPhrase ) {
            assertString( response.reasonPhrase ).isEqualTo( reasonPhrase );
            return this;
        }

        public HttpAssertion reasonContains( String values ) {
            assertString( response.reasonPhrase ).contains( values );
            return this;
        }

        public HttpAssertion reasonContainsPattern( Pattern pattern ) {
            assertString( response.reasonPhrase ).containsPattern( pattern );
            return this;
        }

        public HttpAssertion reasonContainsPattern( String pattern ) {
            assertString( response.reasonPhrase ).containsPattern( pattern );
            return this;
        }

        public HttpAssertion hasContentType( String contentType ) {
            assertString( response.contentType ).isEqualTo( contentType );
            return this;
        }

        public HttpAssertion hasBody( String body ) {
            assertString( response.contentString() ).isEqualTo( body );
            return this;
        }

        public HttpAssertion bodyContains( String values ) {
            assertString( response.contentString() ).contains( values );
            return this;
        }

        public HttpAssertion bodyContainsPattern( Pattern pattern ) {
            assertString( response.contentString() ).containsPattern( pattern );
            return this;
        }

        public HttpAssertion bodyContainsPattern( String pattern ) {
            assertString( response.contentString() ).containsPattern( pattern );
            return this;
        }

        public HttpAssertion hasHeadersSize( int size ) {
            assertThat( response.headers ).hasSize( size );

            return this;
        }

        public HttpAssertion containsHeader( String name, String value ) {
            containsHeader( name );
            assertString( response.header( name ).orElse( null ) ).isEqualTo( value );
            return this;
        }

        public HttpAssertion containsHeader( String name ) {
            assertThat( response.getHeaders() ).containsKey( name );
            return this;
        }

        public HttpAssertion doesNotContainHeader( String name ) {
            assertThat( response.getHeaders() ).doesNotContainKey( name );
            return this;
        }

        public HttpAssertion containsCookie( String name, Consumer<Cookie> assertion ) {
            Optional<Cookie> cookie = Stream.of( getCookies() ).filter( c -> c.getName().equalsIgnoreCase( name ) ).findAny();
            Assertions.assertThat( cookie )
                .isNotEmpty()
                .withFailMessage( "no such cookie: " + name )
                .get()
                .satisfiesAnyOf( new Consumer[] { assertion } );
            return this;
        }

        public HttpAssertion containsCookie( Cookie cookie ) {
            Assertions.assertThat( getCookies() ).contains( cookie );
            return this;
        }

        public HttpAssertion containsCookie( String cookie ) {
            return containsCookie( Cookie.parseSetCookieHeader( cookie ) );
        }

        public HttpAssertion cookies( Consumer<CookiesHttpAssertion> cons ) {
//            HttpUrl httpUrl = HttpUrl.parse( response.url );
//            assertThat( httpUrl ).isNotNull();
//            List<HttpCookie> cookies = cookieManager.getCookieStore().get( httpUrl.uri() );

//            cons.accept( new CookiesHttpAssertion( cookies ) );
            return this;
        }

        private List<Cookie> getCookies() {
            return BiStream.of( response.headers )
                .filter( ( name, value ) -> "Set-Cookie".equalsIgnoreCase( name ) )
                .mapToObj( ( name, value ) -> Cookie.parseSetCookieHeader( value ) )
                .toList();
        }

        public HttpAssertion is( Consumer<Response> condition ) {
            condition.accept( response );
            return this;
        }

        public HttpAssertion responded( int code, String reasonPhrase, String contentType, String body ) {
            return this.hasCode( code )
                .hasReason( reasonPhrase )
                .hasContentType( contentType )
                .hasBody( body );
        }

        public HttpAssertion respondedJson( int code, String reasonPhrase, String body ) {
            return respondedJson( code, reasonPhrase, body, Map.of() );
        }

        public HttpAssertion respondedJson( int code, String reasonPhrase, String body, Map<String, Object> substitutions ) {
            assertJsonResponse( response )
                .isEqualTo( code, reasonPhrase, body, substitutions )
                .hasJsonContentType();
            return this;
        }

        public HttpAssertion respondedJson( String json ) {
            return respondedJson( json, Map.of() );
        }

        public HttpAssertion respondedJson( String json, Map<String, Object> substitutions ) {
            return respondedJson( HTTP_OK, "OK", json, substitutions );
        }

        public HttpAssertion respondedJson( Class<?> contextClass, String resource ) {
            return respondedJson( contentOfTestResource( contextClass, resource, ofString() ) );
        }

        public HttpAssertion respondedJson( Class<?> contextClass, String resource, Map<String, Object> substitutions ) {
            return respondedJson( contentOfTestResource( contextClass, resource, ofString() ), substitutions );
        }

        public HttpAssertion satisfies( Consumer<Response> assertion ) {
            assertion.accept( response );
            return this;
        }

        public <T> T unmarshal( Class<T> clazz ) {
            try {
                Optional<T> unmarshal = response.unmarshal( clazz );
                assertThat( unmarshal ).isPresent();
                return unmarshal.get();
            } catch( JsonException e ) {
                throw Assertions.<AssertionError>fail( e.getMessage(), e );
            }
        }

        public Asserts.StringAssertion body() {
            return assertString( response.contentString() );
        }

        public Asserts.StringAssertion body( Function<byte[], String> conv ) {
            return assertString( conv.apply( response.content() ) );
        }
    }

    public static final class CookiesHttpAssertion {
        private final List<Cookie> cookies;

        @SneakyThrows
        public CookiesHttpAssertion( List<HttpCookie> cookies ) {
            this.cookies = Lists.map( cookies, c -> Cookie.builder( c.getName(), c.getValue() )
                .withPath( c.getPath() )
                .withDomain( c.getDomain() )
                .withMaxAge( c.getMaxAge() < 0 ? null : ( int ) c.getMaxAge() )
//                .withExpires( c.getMaxAge() <= 0 ? null : new Date( ( whenCreatedFieldGet( c ) + c.getMaxAge() ) * 1000L ) )
                .withDiscard( c.getDiscard() )
                .withSecure( c.getSecure() )
                .withHttpOnly( c.isHttpOnly() )
                .withVersion( c.getVersion() )
                .withComment( c.getComment() )
                .build() );
        }

        public CookieHttpAssertion cookie( String name ) {
            Optional<Cookie> cookie = Stream.of( cookies ).filter( c -> c.getName().equalsIgnoreCase( name ) ).findAny();

            assertThat( cookie ).isPresent();
            return CookieHttpAssertion.assertCookie( cookie.get() );
        }
    }

    public static final class CookieHttpAssertion {
        private final Cookie cookie;

        private CookieHttpAssertion( Cookie cookie ) {
            this.cookie = cookie;
        }

        public static CookieHttpAssertion assertCookie( Cookie cookie ) {
            return new CookieHttpAssertion( cookie );
        }

        public CookieHttpAssertion hasValue( String value ) {
            assertString( cookie.getValue() ).isEqualTo( value );
            return this;
        }

        public CookieHttpAssertion hasValue( Object value ) {
            return hasValue( String.valueOf( value ) );
        }

        public CookieHttpAssertion hasDomain( String domain ) {
            assertString( cookie.getDomain() ).isEqualTo( domain );
            return this;
        }

        public CookieHttpAssertion expiresAt( DateTime expiration ) {
            assertThat( cookie.getExpires() ).isEqualTo( expiration.toDate() );
            return this;
        }

        public CookieHttpAssertion expiresAfter( DateTime expiration ) {
            assertThat( cookie.getExpires() ).isAfterOrEqualTo( expiration.toDate() );
            return this;
        }

        public CookieHttpAssertion hasPath( String path ) {
            assertString( cookie.getPath() ).isEqualTo( path );
            return this;
        }

        public AbstractIntegerAssert<?> maxAge() {
            return assertThat( cookie.getMaxAge() );
        }

        public CookieHttpAssertion hasNotMaxAge() {
            return hasMaxAge( -1 );
        }

        public CookieHttpAssertion hasMaxAge( int maxAge ) {
            assertThat( cookie.getMaxAge() ).isEqualTo( maxAge );
            return this;
        }

        public CookieHttpAssertion isSecure() {
            assertThat( cookie.isSecure() ).isTrue();
            return this;
        }

        public CookieHttpAssertion isNotSecure() {
            assertThat( cookie.isSecure() ).isFalse();
            return this;
        }

        public CookieHttpAssertion isHttpOnly() {
            assertThat( cookie.isHttpOnly() ).isTrue();
            return this;
        }

        public CookieHttpAssertion isNotHttpOnly() {
            assertThat( cookie.isHttpOnly() ).isFalse();
            return this;
        }

    }

    public static final class JsonHttpAssertion {
        private final Response response;

        private JsonHttpAssertion( Response response ) {
            this.response = response;
        }

        public static JsonHttpAssertion assertJsonResponse( Response response ) {
            return new JsonHttpAssertion( response );
        }

        public JsonHttpAssertion isEqualTo( int code, String reasonPhrase, String body ) {
            return isEqualTo( code, reasonPhrase, body, Map.of() );
        }

        public JsonHttpAssertion isEqualTo( int code, String reasonPhrase, String body, Map<String, Object> substitutions ) {
            assertHttpResponse( response )
                .hasCode( code )
                .hasReason( reasonPhrase );
            assertJson().isEqualTo( body, substitutions );
            return this;
        }

        public JsonHttpAssertion isEqualTo( String json ) {
            return this.isEqualTo( HTTP_OK, "OK", json );
        }

        public JsonHttpAssertion isEqualTo( Class<?> contextClass, String resource ) {
            return this.isEqualTo( contentOfTestResource( contextClass, resource, ofString() ) );
        }

        public JsonAsserts.JsonAssertion assertJson() {
            return JsonAsserts.assertJson( response.contentString() );
        }

        public JsonHttpAssertion hasJsonContentType() {
            assertHttpResponse( response ).hasContentType( APPLICATION_JSON );
            return this;
        }
    }
}
