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

import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.List;

import static oap.application.testng.KernelFixture.ANY;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelFixtureTest extends Fixtures {
    private KernelFixture kernelFixture;
    private TestFixture testFixture;
    private TestFixtureC testFixtureC;
    private TestFixtureC testFixtureC2;

    @Test
    public void isolation() {
        var f = new Fixtures() {
            {
                testFixture = fixture( new TestFixture( "PREFIXED_" ) );
                kernelFixture = fixture( new KernelFixture(
                    urlOfTestResource( KernelFixtureTest.class, "application.test.conf" ),
                    List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
                ) ).definePort( "TEST_PORT" );
            }
        };

        f.fixBeforeMethod();

        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( kernelFixture.portFor( "TEST_PORT" ) );
        assertThat( testFixture.service( ANY, Service.class ).value )
            .isEqualTo( testFixture.portFor( "TEST_PORT" ) );
        assertThat( kernelFixture.service( ANY, Service.class ) )
            .isNotSameAs( testFixture.service( ANY, Service.class ) );
        assertThat( kernelFixture.portFor( "TEST_PORT" ) )
            .isNotSameAs( testFixture.portFor( "TEST_PORT" ) );
    }

    @Test
    public void isolationSecondRun() {
        var f = new Fixtures() {
            {
                testFixture = fixture( new TestFixture( "PREFIXED_" ) );
                kernelFixture = fixture( new KernelFixture(
                    urlOfTestResource( KernelFixtureTest.class, "application.test.conf" ),
                    List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
                ) ).definePort( "TEST_PORT" );
            }
        };

        f.fixBeforeMethod();
        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( kernelFixture.portFor( "TEST_PORT" ) );
        assertThat( testFixture.service( ANY, Service.class ).value )
            .isEqualTo( testFixture.portFor( "TEST_PORT" ) );
        assertThat( kernelFixture.service( ANY, Service.class ) )
            .isNotSameAs( testFixture.service( ANY, Service.class ) );
        assertThat( kernelFixture.portFor( "TEST_PORT" ) )
            .isNotSameAs( testFixture.portFor( "TEST_PORT" ) );
    }

    @Test
    public void testCyclicDependency() {
        var f = new Fixtures() {
            {
                testFixtureC = fixture( new TestFixtureC( "f1" ) );
                testFixtureC2 = fixture( new TestFixtureC( "f2" ) );
            }
        };

        f.fixBeforeMethod();

        assertThat( testFixtureC.service( ANY, Service.class ).value )
            .isEqualTo( testFixtureC2.portFor( "TEST_PORT" ) );
        assertThat( testFixtureC2.service( ANY, Service.class ).value )
            .isEqualTo( testFixtureC.portFor( "TEST_PORT" ) );
    }

    public static class Service {
        public int value;
    }

    public static class TestFixture extends AbstractKernelFixture<TestFixture> {
        public TestFixture( String name ) {
            super( name,
                urlOfTestResource( KernelFixtureTest.class, "application.fixture.conf" ),
                List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
            );
            definePort( "TEST_PORT" );
        }
    }

    public static class TestFixtureC extends AbstractKernelFixture<TestFixtureC> {
        public TestFixtureC( String name ) {
            super( name,
                urlOfTestResource( KernelFixtureTest.class, "application.fixture-" + name + ".conf" ),
                List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
            );
            definePort( "TEST_PORT" );
        }
    }
}
