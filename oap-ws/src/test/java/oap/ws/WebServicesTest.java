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

import oap.application.Application;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.http.Server;
import oap.io.Resources;
import oap.metrics.Metrics;
import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static oap.http.testng.HttpAsserts.*;
import static oap.http.testng.HttpAsserts.get;
import static org.apache.http.entity.ContentType.*;
import static org.testng.Assert.assertEquals;

public class WebServicesTest {
    protected final Server server = new Server( Env.port(), 100  );
    protected final WebServices ws = new WebServices( server, Lists.of(
        Resources.readString( getClass(), "ws.json" ).map( WsConfig::parse ).get() ) );
    protected final WebServices wsHoconConf = new WebServices( server, Lists.of(
        Resources.readString( getClass(), "ws.conf" ).map( WsConfig::parse ).get() ) );

    @BeforeClass
    public void startServer() {
        Application.register( "math", new MathWS() );
        Application.register( "handler", new TestHandler() );
        ws.start();
        wsHoconConf.start();
        server.start();
    }

    @AfterClass
    public void stopServer() {
        server.stop();
        ws.stop();
        wsHoconConf.stop();
        reset();
    }

    @Test
    public void testHoconWeb() {
        get( HTTP_PREFIX + "/hocon/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
    }

    @Test
    public void invocations() throws IOException {
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/id?a=aaa" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"aaa\"" );
        get( HTTP_PREFIX + "/x/v/math/req" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"" + HTTP_PREFIX + "/x/v/math\"" );
        get( HTTP_PREFIX + "/x/v/math/sumab?a=1&b=2" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "3" );
        get( HTTP_PREFIX + "/x/v/math/sumabopt?a=1&b=2" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "3" );
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/sumabopt?a=1" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "1" );
        get( HTTP_PREFIX + "/x/v/math/en?a=CLASS" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"CLASS\"" );
        get( HTTP_PREFIX + "/x/v/math/sum?a=1&b=2&b=3" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "6" );
        get( HTTP_PREFIX + "/x/v/math/bean?i=1&s=sss" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
        post( HTTP_PREFIX + "/x/v/math/bytes", "1234", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        post( HTTP_PREFIX + "/x/v/math/string", "1234", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        post( HTTP_PREFIX + "/x/v/math/json", "{\"i\":1,\"s\":\"sss\"}", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
        get( HTTP_PREFIX + "/x/v/math/code?code=204" )
            .assertResponse( 204, "No Content" );
        assertEquals(
            ws.metrics.snapshot( Metrics.name( "rest_timer" )
                .tag( "service", MathWS.class.getSimpleName() )
                .tag( "method", "bean" ) ).count,
            1 );
        get( HTTP_PREFIX + "/x/h/" ).assertResponse( 204 );
    }

    @Test
    public void testDefaultHeaders() {
        assertEquals( get( HTTP_PREFIX + "/x/h/" ).response.getHeader( "Access-Control-Allow-Origin" ).get(), "*" );
        assertEquals( post( HTTP_PREFIX + "/x/v/math/json", "{\"i\":1,\"s\":\"sss\"}",
            APPLICATION_OCTET_STREAM ).response.getHeader( "Access-Control-Allow-Origin" ).get(), "*" );
    }

    static class TestHandler implements Handler {
        @Override
        public void handle( Request request, Response response ) {
            response.respond( HttpResponse.NO_CONTENT );
        }
    }
}

