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
import oap.application.Kernel;
import oap.http.Client;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.metrics.Metrics;
import oap.util.Maps;
import oap.util.Pair;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static oap.http.testng.HttpAsserts.HTTP_URL;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

@Slf4j
public class WebServicesTest extends AbstractWebServicesTest {

    @Override
    protected void registerServices( Kernel kernel ) {
        kernel.register( "math", new MathWS() );
        kernel.register( "handler", new TestHandler() );
    }

    @Test
    public void testPath() {
        assertGet( HTTP_URL( "/x/v/math" ) )
            .responded( 200, "OK", APPLICATION_JSON, "2" );
    }

    @Test
    public void testSort() {
        assertGet( HTTP_URL( "/x/v/math/test/sort/default" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"__default__\"" );
        assertGet( HTTP_URL( "/x/v/math/test/sort/45" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"45\"" );
    }

    @Test
    public void testEqual() {
        assertGet( HTTP_URL( "/x/v/math/test/sort=3/test" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"3\"" );
    }


    @Test
    public void invocations() {
        assertGet( HTTP_URL( "/x/v/math/x?i=1&s=2" ) )
            .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        assertGet( HTTP_URL( "/x/v/math/x?i=1&s=2" ) )
            .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        assertGet( HTTP_URL( "/x/v/math/sumab?a=1&b=2" ) )
            .responded( 200, "OK", APPLICATION_JSON, "3" );
        assertGet( HTTP_URL( "/x/v/math/x?i=1&s=2" ) )
            .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        assertGet( HTTP_URL( "/x/v/math/sumabopt?a=1" ) )
            .responded( 200, "OK", APPLICATION_JSON, "1" );
        assertGet( HTTP_URL( "/x/v/math/bean?i=1&s=sss" ) )
            .responded( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
        assertPost( HTTP_URL( "/x/v/math/bytes" ), "1234", APPLICATION_OCTET_STREAM )
            .responded( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        assertPost( HTTP_URL( "/x/v/math/string" ), "1234", APPLICATION_OCTET_STREAM )
            .responded( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        assertGet( HTTP_URL( "/x/v/math/code?code=204" ) )
            .hasCode( 204 );
        assertEquals(
            Metrics.snapshot( Metrics.name( "rest_timer" )
                .tag( "service", MathWS.class.getSimpleName() )
                .tag( "method", "bean" ) ).count,
            1 );
        assertGet( HTTP_URL( "/x/h/" ) ).hasCode( 204 );
        assertGet( HTTP_URL( "/hocon/x/v/math/x?i=1&s=2" ) )
            .responded( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );

    }

    @Test
    public void testEnum() {
        assertGet( HTTP_URL( "/x/v/math/en?a=CLASS" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"CLASS\"" );
    }

    @Test
    public void testOptional() {
        assertGet( HTTP_URL( "/x/v/math/sumabopt?a=1&b=2" ) )
            .responded( 200, "OK", APPLICATION_JSON, "3" );
    }

    @Test
    public void testParameterList() {
        assertGet( HTTP_URL( "/x/v/math/sum?a=1&b=2&b=3" ) )
            .responded( 200, "OK", APPLICATION_JSON, "6" );
    }

    @Test
    public void testString() {
        assertGet( HTTP_URL( "/x/v/math/id?a=aaa" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"aaa\"" );
    }

    @Test
    public void testRequest() {
        assertGet( HTTP_URL( "/x/v/math/req" ) )
            .responded( 200, "OK", APPLICATION_JSON, "\"" + HTTP_URL( "/x/v/math\"" ) );
    }

    @Test
    public void testBean() {
        assertPost( HTTP_URL( "/x/v/math/json" ), "{\"i\":1,\"s\":\"sss\"}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
    }

    @Test
    public void testList() {
        assertPost( HTTP_URL( "/x/v/math/list" ), "[\"1str\", \"2str\"]", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "[\"1str\",\"2str\"]" );
    }

    @Test
    public void testDefaultHeaders() {
        assertGet( HTTP_URL( "/x/h/" ) )
            .containsHeader( "Access-Control-Allow-Origin", "*" );
        assertPost( HTTP_URL( "/x/v/math/json" ), "{\"i\":1,\"s\":\"sss\"}",
            APPLICATION_OCTET_STREAM ).containsHeader( "Access-Control-Allow-Origin", "*" );
    }

    @Test
    public void testShouldVerifyGZIPRequestProcessing() throws Exception {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream( byteArrayOutputStream );
        gzip.write( "{\"i\":1,\"s\":\"sss\"}".getBytes( "UTF-8" ) );
        gzip.close();

        final Client.Response response = Client
            .custom()
            .build()
            .post( HTTP_URL( "/x/v/math/json" ),
                new ByteArrayInputStream( byteArrayOutputStream.toByteArray() ),
                APPLICATION_JSON, Maps.of( Pair.__( "Content-Encoding", "gzip" ) ) );

        assertThat( response.code ).isEqualTo( 200 );
        assertThat( response.contentString() ).isEqualTo( "{\"i\":1,\"s\":\"sss\"}" );
    }

    static class TestHandler implements Handler {
        @Override
        public void handle( Request request, Response response ) {
            response.respond( HttpResponse.NO_CONTENT );
        }
    }

}

