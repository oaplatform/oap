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

package oap.application.testng;

import oap.testng.AbstractEnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.List;

import static oap.application.testng.KernelFixture.ANY;
import static oap.testng.AbstractEnvFixture.Kind;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelFixtureTest extends Fixtures {
    private final KernelFixture kernelFixture;
    private final TestFixture testFixture;

    {
        kernelFixture = fixture( new KernelFixture(
            urlOfTestResource( KernelFixtureTest.class, "application.test.conf" ),
            pathOfTestResource( KernelFixture.class, "confd" ),
            List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
        ) ).withKind( Kind.MAP );

        testFixture = kernelFixture.fixture( new TestFixture().withKind( Kind.MAP ) );

        kernelFixture.define( "MY_TEST_PORT", testFixture.portFor( "TEST_PORT" ) );
    }

    @Test
    public void value() {
        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( "from fixture" + testFixture.portFor( "TEST_PORT" ) );
    }

    @Test( dependsOnMethods = "value" )
    public void value2() {
        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( "from fixture" + testFixture.portFor( "TEST_PORT" ) );
    }

    public static class Service {
        public String value;
    }

    public static class TestFixture extends AbstractEnvFixture<TestFixture> {
        public TestFixture() {
            definePort( "TEST_PORT", "TEST_PORT" );
        }
    }
}
