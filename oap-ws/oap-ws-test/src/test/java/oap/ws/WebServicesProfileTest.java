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

import oap.application.testng.KernelFixture;
import oap.http.Http;
import oap.http.testng.HttpAsserts;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.io.Resources.urlOrThrow;

public class WebServicesProfileTest extends Fixtures {

    public WebServicesProfileTest() {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void shouldStartWebServiceIfProfileIsNotConfiguredForServiceAndWS() {
        assertGet( HttpAsserts.httpUrl( "/test-no-profile/text?value=empty" ) ).isOk().hasBody( "ok" );
    }

    @Test
    public void shouldNotStartWebServiceIfProfileIsConfiguredForServiceAndNotWS() {
        assertGet( HttpAsserts.httpUrl( "/new-profile/text?value=empty" ) ).hasCode( Http.StatusCode.NO_CONTENT );
    }


    public static class TestWS {
        @WsMethod( path = "/text", method = GET, produces = "text/plain" )
        public String text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return "ok";
        }
    }
}
