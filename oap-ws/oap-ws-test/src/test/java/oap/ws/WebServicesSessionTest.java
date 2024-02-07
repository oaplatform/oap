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
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.WsParam.From.SESSION;

public class WebServicesSessionTest extends Fixtures {
    public WebServicesSessionTest() {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void sessionViaResponse() {
        assertGet( httpUrl( "/session/put" ), Map.of( "value", "vvv" ), Map.of() )
            .hasCode( Http.StatusCode.NO_CONTENT );
        assertGet( httpUrl( "/session/get" ) )
            .isOk()
            .hasBody( "vvv" );
    }

    @Test
    public void sessionDirectly() {
        assertGet( httpUrl( "/session/putDirectly" ), Map.of( "value", "vvv" ), Map.of() )
            .hasCode( Http.StatusCode.NO_CONTENT );
        assertGet( httpUrl( "/session/get" ) )
            .isOk()
            .hasBody( "vvv" );
    }

    @Test
    public void respondHtmlContentType() {
        assertGet( httpUrl( "/session/putDirectly" ), Map.of( "value", "vvv" ), Map.of() )
            .hasCode( Http.StatusCode.NO_CONTENT );
        assertGet( httpUrl( "/session/html" ) )
            .isOk()
            .hasBody( "vvv" )
            .hasContentType( Http.ContentType.TEXT_HTML );
    }

    @SuppressWarnings( "unused" )
    private static class TestWS {

        public static final String IN_SESSION = "inSession";

        public Response put( String value, Session session ) {
            session.set( IN_SESSION, value );
            return Response.noContent();
        }

        public void putDirectly( String value, Session session ) {
            session.set( IN_SESSION, value );
        }

        @WsMethod( path = "/get", method = GET, produces = "text/plain" )
        public String get( @WsParam( from = SESSION ) String inSession ) {
            return inSession;
        }

        @WsMethod( path = "/html", method = GET, produces = "text/html" )
        public String getHtml( @WsParam( from = SESSION ) String inSession ) {
            return inSession;
        }
    }
}
