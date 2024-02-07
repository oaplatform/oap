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

package oap.ws.interceptor;

import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.ws.InvocationContext;
import oap.ws.Response;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;

public class InterceptorTest extends Fixtures {
    public InterceptorTest() {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void shouldNotAllowRequestWhenErrorInterceptor() {
        assertGet( httpUrl( "/interceptor/text?value=error" ) )
            .hasCode( FORBIDDEN )
            .hasReason( "caused by interceptor" );
    }

    @SuppressWarnings( "unused" )
    private static class TestWS {
        public String text( String value ) {
            return "ok";
        }
    }

    private static class PassInterceptor implements Interceptor {
        @Override
        public Optional<Response> before( InvocationContext context ) {
            return Optional.empty();
        }
    }

    private static class ErrorInterceptor implements Interceptor {
        @Override
        public Optional<Response> before( InvocationContext context ) {
            return "error".equals( context.exchange.getStringParameter( "value" ) )
                ? Optional.of( new Response( FORBIDDEN, "caused by interceptor" ) )
                : Optional.empty();
        }
    }
}
