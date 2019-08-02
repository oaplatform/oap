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

import oap.testng.Fixtures;
import oap.ws.testng.WsFixture;
import org.testng.annotations.Test;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;

public class WebServiceLocalTest extends Fixtures {
    {
        fixture( new WsFixture( getClass(),
            ( ws, kernel ) -> kernel.register( "test", new TestWS() ),
            "ws-local.conf" ) );
    }

    @Test
    public void shouldAllowRequestWhenEmptyInterceptor() {
        assertGet( httpUrl( "/test/text?value=empty" ) ).isOk().hasBody( "ok" );
    }

    @SuppressWarnings( "unused" )
    private static class TestWS {

        @WsMethod( path = "/text", method = GET, produces = "text/plain" )
        public String text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return "ok";
        }
    }
}
