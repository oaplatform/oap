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

package oap.ws.admin;

import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import static oap.http.Http.ContentType.TEXT_HTML;
import static oap.http.test.HttpAsserts.assertGet;
import static oap.io.Resources.urlOrThrow;

public class InspectorWSTest extends Fixtures {
    private final KernelFixture kernel;

    public InspectorWSTest() {
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        kernel = fixture( new KernelFixture( testDirectoryFixture, urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void testUi() {
       assertGet( kernel.httpUrl( "/system/admin/inspector/ui" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "oap-ws-admin-ws-test.test-service" )
            .bodyContains( "id=\"filter\"" );
    }

    @Test
    public void testServiceUi() {
        kernel.service( "oap-ws-admin-ws-test", TestService.class ).setV2( "testv" );

        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/oap-ws-admin-ws-test.test-service" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "implementation" )
            .bodyContains( TestService.class.getName() )
            .bodyContains( "modifier" )
            .bodyContains( "testv" );
    }

    @Test
    public void testServiceUiMethods() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/oap-ws-admin-ws-test.test-service" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "getV2" )
            .bodyContains( "value?query=oap-ws-admin-ws-test.test-service.instance.getV2()" )
            .bodyDoesNotContain( "getClass" );
    }

    @Test
    public void testServiceUiMethodsWithParams() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/oap-ws-admin-ws-test.test-service" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "setV2" )
            .bodyContains( "data-query=\"oap-ws-admin-ws-test.test-service.instance.setV2\"" )
            .bodyContains( "data-kind=\"string\"" );
    }

    @Test
    public void testServiceUiNotFound() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/unknown-module.unknown-service" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "not found" );
    }

    @Test
    public void testValuePage() {
        kernel.service( "oap-ws-admin-ws-test", TestService.class ).setV2( "testv" );

        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/value?query=oap-ws-admin-ws-test.test-service.instance.value" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "testv" );
    }

    @Test
    public void testValuePageError() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/value?query=oap-ws-admin-ws-test.test-service.instance.doesNotExist()" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "at " );
    }

    @Test
    public void testValuePageInspectMode() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/value?query=oap-ws-admin-ws-test.test-service.instance" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "Methods" )
            .bodyContains( "setV2" )
            .bodyContains( "mode=json" );
    }

    @Test
    public void testValuePageJsonMode() {
        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/value?query=oap-ws-admin-ws-test.test-service.instance&mode=json" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "\"value\"" )
            .bodyContains( "mode=inspect" );
    }

    @Test
    public void testValuePageLeafDefaultsToJson() {
        kernel.service( "oap-ws-admin-ws-test", TestService.class ).setV2( "testv" );

        assertGet( kernel.httpUrl( "/system/admin/inspector/ui/value?query=oap-ws-admin-ws-test.test-service.instance.value" ) )
            .isOk()
            .hasContentType( TEXT_HTML )
            .bodyContains( "testv" )
            .bodyDoesNotContain( "Methods" );
    }
}
