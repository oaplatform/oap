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
import oap.http.*;
import oap.http.testng.HttpAsserts;
import oap.metrics.Metrics;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import oap.ws.WebServices;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.ws.WsParam.From.BODY;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class ValidatePeerMethodTest extends AbstractTest {
    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, null );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Metrics.resetAll();
        server.start();
        ws.bind( "test", Cors.DEFAULT, new TestWS(), false, null, Protocol.HTTP );

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
            .responded( 400, "validation failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "error:test" );
    }

    public static class TestWS {

        @WsMethod( path = "/run/validation/default", method = POST )
        public Object validationDefault( @WsParam( from = BODY ) String request ) {
            return HttpResponse.ok( request, true, TEXT_PLAIN );
        }

        @WsMethod( path = "/run/validation/ok", method = POST )
        @Validate( "validateOk" )
        public Object validationOk( @WsParam( from = BODY ) String request ) {
            return HttpResponse.ok( request, true, TEXT_PLAIN );
        }

        @WsMethod( path = "/run/validation/fail", method = POST )
        @Validate( "validateFail" )
        public Object validationFail( @WsParam( from = BODY ) String request ) {
            return HttpResponse.ok( request, true, TEXT_PLAIN );
        }

        @SuppressWarnings( "unused" )
        public List<String> validateOk( String request ) {
            return Lists.empty();
        }

        @SuppressWarnings( "unused" )
        public List<String> validateFail( String request ) {
            return Lists.of( "error:" + request );
        }
    }
}
