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
package oap.ws.validate;

import oap.concurrent.SynchronizedThread;
import oap.http.HttpResponse;
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.ws.WsParam.From.BODY;

public class MethodValidatorPeerMethodTest extends AbstractTest {
   private static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );

   private final Server server = new Server( 100 );
   private final WebServices ws = new WebServices( server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );

   private SynchronizedThread listener;

   @BeforeClass
   public void startServer() {
      Metrics.resetAll();
      server.start();
      ws.bind( "test", GenericCorsPolicy.DEFAULT, new TestWS(), false, SESSION_MANAGER, Collections.emptyList(), Protocol.HTTP );

      PlainHttpListener http = new PlainHttpListener( server, Env.port() );
      listener = new SynchronizedThread( http );
      listener.start();
   }

   @AfterClass
   public void stopServer() {
      listener.stop();
      server.stop();
      server.unbind( "test" );

      HttpAsserts.reset();
      Metrics.resetAll();
   }

   @Test
   public void validationDefault() throws InterruptedException {
      assertPost( HTTP_PREFIX + "/test/run/validation/default", "test", TEXT_PLAIN )
         .responded( 200, "OK", TEXT_PLAIN, "test" );
   }

   @Test
   public void validationOk() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok", "test", TEXT_PLAIN )
         .responded( 200, "OK", TEXT_PLAIN, "test" );
   }

   @Test
   public void validationFail() {
      assertPost( HTTP_PREFIX + "/test/run/validation/fail", "test", TEXT_PLAIN )
         .responded( 400, "validation failed", TEXT_PLAIN, "error1\nerror2" );
   }

   @Test
   public void validationFailCode() {
      assertPost( HTTP_PREFIX + "/test/run/validation/fail-code", "test", TEXT_PLAIN )
         .responded( 403, "denied", TEXT_PLAIN, "denied" );
   }

   @Test
   public void validationMethods() {
      assertGet( HTTP_PREFIX + "/test/run/validation/methods?a=a&b=5&c=c" )
         .responded( 400, "validation failed", TEXT_PLAIN, "a\na5\n5a" );
   }

   public static class TestWS {

      @WsMethod( path = "/run/validation/default", method = POST )
      public Object validationDefault( @WsParam( from = BODY ) String request ) {
         return HttpResponse.ok( request, true, TEXT_PLAIN );
      }

      @WsMethod( path = "/run/validation/ok", method = POST, produces = "text/plain" )
      @Validate( "validateOk" )
      public String validationOk( @WsParam( from = BODY ) String request ) {
         return request;
      }

      @WsMethod( path = "/run/validation/fail", method = POST )
      @Validate( "validateFail" )
      public Object validationFail( @WsParam( from = BODY ) String request ) {
         return null;
      }

      @WsMethod( path = "/run/validation/fail-code", method = POST )
      @Validate( "validateFailCode" )
      public Object validationFailCode( @WsParam( from = BODY ) String request ) {
         return null;
      }

      @WsMethod( path = "/run/validation/methods", method = GET )
      @Validate( { "validateA", "validateAB", "validateBA" } )
      public String validationMethods( String a, int b, String c ) {
         return a + b + c;
      }


      @SuppressWarnings( "unused" )
      public ValidationErrors validateA( String a ) {
         return ValidationErrors.create( a );
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateAB( String a, int b ) {
         return ValidationErrors.create( a + b );
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateBA( int b, String a ) {
         return ValidationErrors.create( b + a );
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateOk( String request ) {
         return ValidationErrors.empty();
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateFail( String request ) {
         return ValidationErrors.create( Lists.of( "error1", "error2" ) );
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateFailCode( String request ) {
         return ValidationErrors.create( 403, "denied" );
      }
   }
}
