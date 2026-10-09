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

package oap.ws.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.io.Resources.urlOrThrow;
import static org.assertj.core.api.Assertions.assertThat;

public class OpenapiCacheTest extends Fixtures {
    private final KernelFixture kernel;

    public OpenapiCacheTest() {
        TestDirectoryFixture testDirectoryFixture = fixture( new TestDirectoryFixture() );
        kernel = fixture( new KernelFixture( testDirectoryFixture, urlOrThrow( getClass(), "/application-ws-openapi.test.conf" ) ) );
    }

    @Test
    public void testSameArgumentsReturnTheCachedDocument() {
        Openapi openapi = kernel.service( "oap-ws-openapi-ws", Openapi.class );

        OpenAPI first = openapi.generateOpenApi( true, Optional.empty() );
        OpenAPI second = openapi.generateOpenApi( true, Optional.empty() );

        assertThat( second ).isSameAs( first );
    }

    @Test
    public void testDifferentArgumentsAreNotCachedTogether() {
        Openapi openapi = kernel.service( "oap-ws-openapi-ws", Openapi.class );

        OpenAPI skipDeprecated = openapi.generateOpenApi( true, Optional.empty() );
        OpenAPI withDeprecated = openapi.generateOpenApi( false, Optional.empty() );
        OpenAPI privatePort = openapi.generateOpenApi( true, Optional.of( "httpprivate" ) );

        assertThat( withDeprecated ).isNotSameAs( skipDeprecated );
        assertThat( privatePort ).isNotSameAs( skipDeprecated );
    }
}
