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
package oap.ws;

import lombok.extern.slf4j.Slf4j;
import oap.application.Application;
import oap.concurrent.SynchronizedThread;
import oap.http.*;
import oap.metrics.Metrics;
import oap.testng.Env;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static oap.http.testng.HttpAsserts.*;
import static org.apache.http.entity.ContentType.*;
import static org.testng.Assert.assertEquals;

@Slf4j
public class WebServicesTest {
   private final Server server = new Server( 100 );
   private final WebServices ws = new WebServices( server,
      WsConfig.CONFIGURATION.fromResource( getClass(), "ws.json" ),
      WsConfig.CONFIGURATION.fromResource( getClass(), "ws.conf" )
   );

   private SynchronizedThread listener;

   @BeforeClass
   public void startServer() {
      Application.register( "math", new MathWS() );
      Application.register( "handler", new TestHandler() );
      ws.start();
      listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
      listener.start();
   }

   @AfterClass
   public void stopServer() {
      listener.stop();
      server.stop();
      ws.stop();
      reset();
   }

   @Test
   public void invocations() {
      assertGet( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
         .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
      assertGet( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
         .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
      assertGet( HTTP_PREFIX + "/x/v/math/id?a=aaa" )
         .responded( 200, "OK", APPLICATION_JSON, "\"aaa\"" );
      assertGet( HTTP_PREFIX + "/x/v/math/req" )
         .responded( 200, "OK", APPLICATION_JSON, "\"" + HTTP_PREFIX + "/x/v/math\"" );
      assertGet( HTTP_PREFIX + "/x/v/math/sumab?a=1&b=2" )
         .responded( 200, "OK", APPLICATION_JSON, "3" );
      assertGet( HTTP_PREFIX + "/x/v/math/sumabopt?a=1&b=2" )
         .responded( 200, "OK", APPLICATION_JSON, "3" );
      assertGet( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
         .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
      assertGet( HTTP_PREFIX + "/x/v/math/sumabopt?a=1" )
         .responded( 200, "OK", APPLICATION_JSON, "1" );
      assertGet( HTTP_PREFIX + "/x/v/math/en?a=CLASS" )
         .responded( 200, "OK", APPLICATION_JSON, "\"CLASS\"" );
      assertGet( HTTP_PREFIX + "/x/v/math/sum?a=1&b=2&b=3" )
         .responded( 200, "OK", APPLICATION_JSON, "6" );
      assertGet( HTTP_PREFIX + "/x/v/math/bean?i=1&s=sss" )
         .responded( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
      assertPost( HTTP_PREFIX + "/x/v/math/bytes", "1234", APPLICATION_OCTET_STREAM )
         .responded( 200, "OK", APPLICATION_JSON, "\"1234\"" );
      assertPost( HTTP_PREFIX + "/x/v/math/string", "1234", APPLICATION_OCTET_STREAM )
         .responded( 200, "OK", APPLICATION_JSON, "\"1234\"" );
      assertPost( HTTP_PREFIX + "/x/v/math/json", "{\"i\":1,\"s\":\"sss\"}", APPLICATION_OCTET_STREAM )
         .responded( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
      assertGet( HTTP_PREFIX + "/x/v/math/code?code=204" )
         .hasCode( 204 );
      assertEquals(
         Metrics.snapshot( Metrics.name( "rest_timer" )
            .tag( "service", MathWS.class.getSimpleName() )
            .tag( "method", "bean" ) ).count,
         1 );
      assertGet( HTTP_PREFIX + "/x/h/" ).hasCode( 204 );
      assertGet( HTTP_PREFIX + "/hocon/x/v/math/x?i=1&s=2" )
         .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );

   }

   @Test
   public void testDefaultHeaders() {
      assertGet( HTTP_PREFIX + "/x/h/" )
         .containsHeader( "Access-Control-Allow-Origin", "*" );
      assertPost( HTTP_PREFIX + "/x/v/math/json", "{\"i\":1,\"s\":\"sss\"}",
         APPLICATION_OCTET_STREAM ).containsHeader( "Access-Control-Allow-Origin", "*" );
   }

   static class TestHandler implements Handler {
      @Override
      public void handle( Request request, Response response ) {
         response.respond( HttpResponse.NO_CONTENT );
      }
   }
}

