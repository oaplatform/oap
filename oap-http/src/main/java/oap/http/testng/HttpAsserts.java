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

import com.google.common.base.Throwables;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.SimpleAsyncHttpClient;
import oap.http.SimpleClient;
import oap.http.Uri;
import oap.testng.Env;
import oap.util.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpAsserts {

   public static final String HTTP_PREFIX = "http://localhost:" + Env.port();

   public static void reset() {
      SimpleAsyncHttpClient.reset();
   }


   @SafeVarargs
   public static HttpAssertion assertGet( String url, Pair<String, Object>... params ) {
      return assertRequest( new HttpGet( Uri.uri( url, params ) ) );
   }

   public static HttpAssertion assertPost( String url, String requestBody, ContentType contentType ) {
      HttpPost post = new HttpPost( url );
      post.setEntity( new StringEntity( requestBody, contentType ) );
      return assertRequest( post );
   }

   public static HttpAssertion assertPost( String url, InputStream requestBody, ContentType contentType ) {
      HttpPost post = new HttpPost( url );
      post.setEntity( new InputStreamEntity( requestBody, contentType ) );
      return assertRequest( post );
   }

   public static HttpAssertion assertPut( String url, String requestBody, ContentType contentType ) {
      HttpPut put = new HttpPut( url );
      put.setEntity( new StringEntity( requestBody, contentType ) );
      return assertRequest( put );
   }

   private static HttpAssertion assertRequest( HttpUriRequest http ) {
      try {
         return new HttpAssertion( SimpleAsyncHttpClient.execute( http ) );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      } catch( TimeoutException e ) {
         throw Throwables.propagate( e );
      }
   }

   @EqualsAndHashCode
   @ToString
   public static class HttpAssertion {
      private final SimpleClient.Response response;

      public HttpAssertion( SimpleClient.Response response ) {
         this.response = response;
      }

      public HttpAssertion isOk() {
         hasCode( HTTP_OK );
         return this;
      }

      public HttpAssertion hasCode( int code ) {
         assertThat( response.code ).isEqualTo( code );
         return this;
      }

      public HttpAssertion isJson( String json ) {
         assertString( response.contentType ).isEqualTo( ContentType.APPLICATION_JSON.toString() );
         assertJson( response.body ).isEqualTo( json );
         return this;
      }

      public HttpAssertion hasReason( String reasonPhrase ) {
         assertString( response.reasonPhrase ).isEqualTo( reasonPhrase );
         return this;
      }

      public HttpAssertion hasContentType( ContentType contentType ) {
         assertString( response.contentType ).isEqualTo( contentType.toString() );
         return this;
      }

      public HttpAssertion hasBody( String body ) {
         assertString( response.body ).isEqualTo( body );
         return this;
      }

      public HttpAssertion containsHeader( String name, String value ) {
         response.getHeader( name )
            .map( header -> assertString( header ).isEqualTo( value ) )
            .orElseThrow( () -> new AssertionError( "no header present: " + name ) );
         return this;
      }

      public HttpAssertion is( Consumer<SimpleClient.Response> condition ) {
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
