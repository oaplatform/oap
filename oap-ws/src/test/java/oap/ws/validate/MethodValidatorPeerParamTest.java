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
import oap.http.cors.GenericCorsPolicy;
import oap.http.PlainHttpListener;
import oap.http.Protocol;
import oap.http.Server;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.ws.SessionManager;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class MethodValidatorPeerParamTest {
   private static final SessionManager SESSION_MANAGER = new SessionManager( 10, null, "/" );

   private final Server server = new Server( 100 );
   private final WebServices ws = new WebServices( server, SESSION_MANAGER, GenericCorsPolicy.DEFAULT );

   private SynchronizedThread listener;

   @BeforeClass
   public void startServer() {
      Metrics.resetAll();
      server.start();
      ws.bind( "test", GenericCorsPolicy.DEFAULT, new TestWS(), false, SESSION_MANAGER,
         Collections.emptyList(), Protocol.HTTP );

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
   public void validationDefault() {
      assertPost( HTTP_PREFIX + "/test/run/validation/default?q=1", "test", TEXT_PLAIN )
         .responded( 200, "OK", APPLICATION_JSON, "\"1test\"" );
   }

   @Test
   public void validationOk() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok?q=1", "test", TEXT_PLAIN )
         .responded( 200, "OK", APPLICATION_JSON, "\"1test\"" );
   }

   @Test
   public void validationOkList() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok?q=1&ql=_11&ql=_12", "test", TEXT_PLAIN )
         .responded( 200, "OK", APPLICATION_JSON, "\"1_11/_12test\"" );
   }

   @Test
   public void validationOkOptional() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok?q=1&q2=2", "test", TEXT_PLAIN )
         .responded( 200, "OK", APPLICATION_JSON, "\"12test\"" );
   }

   @Test
   public void validationFail() {
      assertPost( HTTP_PREFIX + "/test/run/validation/fail?q=1", "test", TEXT_PLAIN )
         .responded( 400, "validation failed", TEXT_PLAIN, "error:1\nerror:test" );
   }

   @Test
   public void validationRequiredFailed() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok", "test", TEXT_PLAIN )
         .responded( 400, "q is required", TEXT_PLAIN, "q is required" );
   }

   @Test
   public void validationTypeFailed() {
      assertPost( HTTP_PREFIX + "/test/run/validation/ok?q=test", "test", TEXT_PLAIN )
         .responded( 400, "cannot cast test to int", TEXT_PLAIN, "cannot cast test to int" );
   }

   public static class TestWS {

      @WsMethod( path = "/run/validation/default", method = POST )
      public String validationDefault(
         @WsParam( from = QUERY ) int q,
         @WsParam( from = BODY ) String body
      ) {
         return q + body;
      }

      @WsMethod( path = "/run/validation/ok", method = POST )
      public String validationOk(
         @WsParam( from = QUERY ) @WsValidate( "validateOkInt" ) int q,
         @WsParam( from = QUERY ) @WsValidate( "validateOkOptString" ) Optional<String> q2,
         @WsParam( from = QUERY ) @WsValidate( "validateOkListString" ) List<String> ql,
         @WsParam( from = BODY ) @WsValidate( "validateOkString" ) String body
      ) {
         return q + q2.orElse( "" ) + String.join( "/", ql ) + body;
      }

      @WsMethod( path = "/run/validation/fail", method = POST )
      public String validationFail(
         @WsParam( from = QUERY ) @WsValidate( "validateFailInt" ) int q,
         @WsParam( from = BODY ) @WsValidate( "validateFailString" ) String body
      ) {
         return q + body;
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateOkInt( int value ) {
         return empty();
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateOkOptString( Optional<String> value ) {
         return empty();
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateOkListString( List<String> value ) {
         return empty();
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateOkString( String value ) {
         return empty();
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateFailInt( int value ) {
         return error( "error:" + value );
      }

      @SuppressWarnings( "unused" )
      public ValidationErrors validateFailString( String value ) {
         return error( "error:" + value );
      }
   }
}
