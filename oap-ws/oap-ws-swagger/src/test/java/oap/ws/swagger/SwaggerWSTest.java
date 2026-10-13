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

package oap.ws.swagger;

import oap.application.testng.KernelFixture;
import oap.template.TemplateEngine;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.http.Http.ContentType.TEXT_HTML;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.http.test.HttpAsserts.assertGet;
import static oap.io.Resources.urlOrThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SwaggerWSTest extends Fixtures {
    private final KernelFixture kernel;

    public SwaggerWSTest() {
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        kernel = fixture( new KernelFixture( testDirectoryFixture, urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void testRender() {
        assertGet( kernel.httpUrl( "/swagger" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "swagger-ui-dist@5.33.2" )
            .bodyContains( "{name: \"api\", url: \"/system/openapi\"}" )
            .bodyDoesNotContain( "login-box" );
    }

    @Test
    public void testRenderWithLoginGet() {
        SwaggerWS swaggerWS = kernel.service( "oap-ws-swagger", SwaggerWS.class );
        swaggerWS.loginMethod = GET;
        swaggerWS.urlTemplate = "/login?u={username}&p={password}";

        assertGet( kernel.httpUrl( "/swagger" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "login-box" )
            .bodyContains( "\"/login?u={username}&p={password}\"" );
    }

    @Test
    public void testRenderWithLoginPost() {
        SwaggerWS swaggerWS = kernel.service( "oap-ws-swagger", SwaggerWS.class );
        swaggerWS.loginMethod = POST;
        swaggerWS.urlTemplate = "/login";
        swaggerWS.requestTemplate = Optional.of( "{\"username\":\"{username}\",\"password\":\"{password}\"}" );

        assertGet( kernel.httpUrl( "/swagger" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "login-box" )
            .bodyContains( "{\\\"username\\\":\\\"{username}\\\",\\\"password\\\":\\\"{password}\\\"}" );
    }

    @Test
    public void testRenderPostWithoutRequestTemplateThrows() {
        SwaggerWS swaggerWS = new SwaggerWS( new TemplateEngine( 0 ) );
        swaggerWS.loginMethod = POST;
        swaggerWS.urlTemplate = "/login";

        assertThatThrownBy( swaggerWS::render ).isInstanceOf( IllegalStateException.class );
    }
}
