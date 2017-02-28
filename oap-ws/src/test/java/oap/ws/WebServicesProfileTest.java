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
import oap.http.HttpResponse;
import oap.util.Lists;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static oap.http.Request.HttpMethod.GET;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertGet;

public class WebServicesProfileTest extends AbstractWebServicesTest {

    @BeforeClass
    @Override
    public void startServer() {
        Application.register( "no-profile", new TestWS() );
        Application.register( "with-profile", new TestWS() );
        Application.register( "new-profile", new TestWS() );

        Application.registerProfiles( Lists.of( "test-profile" ) );

        super.startServer();
    }

    @Override
    protected List<String> getConfig() {
        return Collections.singletonList( "ws-profile.conf" );
    }

    @Test
    public void testShouldStartWebServiceIfProfileIsNotConfiguredForServiceAndWS() {
        assertGet( HTTP_PREFIX + "/test-no-profile/text?value=empty" ).isOk().hasBody( "\"" + "ok" + "\"" );
    }

    @Test
    public void testShouldStartWebServiceIfProfileIsConfiguredForServiceAndWS() {
        assertGet( HTTP_PREFIX + "/test-with-profile/text?value=empty" ).isOk().hasBody( "\"" + "ok" + "\"" );
    }

    @Test
    public void testShouldNotStartWebServiceIfProfileIsConfiguredForServiceAndNotWS() {
        assertGet( HTTP_PREFIX + "/new-profile/text?value=empty" ).hasCode( 501 );

    }

    private static class TestWS {

        @WsMethod( path = "/text", method = GET )
        public HttpResponse text( @WsParam( from = WsParam.From.QUERY ) String value ) {
            return HttpResponse.ok( "ok" );
        }
    }
}
