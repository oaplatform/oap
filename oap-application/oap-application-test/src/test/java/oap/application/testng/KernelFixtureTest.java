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
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Path;
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
        testFixture = new TestFixture();
        kernelFixture = new KernelFixture(
            new TestDirectoryFixture(),
            urlOfTestResource( KernelFixtureTest.class, "application.test.conf" ),
            List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
        );
        kernelFixture.definePort( "TEST_PORT" );

        var f = Fixtures.fixtures( testFixture, kernelFixture );
        f.fixBeforeMethod();

        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( kernelFixture.getProperty( "TEST_PORT" ) );
        assertThat( testFixture.service( ANY, Service.class ).value )
            .isEqualTo( testFixture.getProperty( "TEST_PORT" ) );
        assertThat( kernelFixture.service( ANY, Service.class ) )
            .isNotSameAs( testFixture.service( ANY, Service.class ) );
        assertThat( kernelFixture.<Integer>getProperty( "TEST_PORT" ) )
            .isNotSameAs( testFixture.getProperty( "TEST_PORT" ) );
    }

    @Test
    public void isolationSecondRun() {
        testFixture = new TestFixture();
        kernelFixture = new KernelFixture(
            new TestDirectoryFixture(),
            urlOfTestResource( KernelFixtureTest.class, "application.test.conf" ),
            List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
        );
        kernelFixture.definePort( "TEST_PORT" );

        var f = Fixtures.fixtures( testFixture, kernelFixture );
        f.fixBeforeMethod();

        assertThat( kernelFixture.service( ANY, Service.class ).value )
            .isEqualTo( kernelFixture.getProperty( "TEST_PORT" ) );
        assertThat( testFixture.service( ANY, Service.class ).value )
            .isEqualTo( testFixture.getProperty( "TEST_PORT" ) );
        assertThat( kernelFixture.service( ANY, Service.class ) )
            .isNotSameAs( testFixture.service( ANY, Service.class ) );
        assertThat( kernelFixture.<Integer>getProperty( "TEST_PORT" ) )
            .isNotSameAs( testFixture.getProperty( "TEST_PORT" ) );
    }

    @Test
    public void testCyclicDependency() {
        var f = new Fixtures() {
            {
                testFixtureC = fixture( new TestFixtureC( "f1" ) );
                testFixtureC2 = fixture( new TestFixtureC( "f2" ) );

                testFixtureC.addDependency( "f2", testFixtureC2 );
                testFixtureC2.addDependency( "f1", testFixtureC );
            }
        };

        f.fixBeforeMethod();

        assertThat( testFixtureC.service( ANY, Service.class ).value )
            .isEqualTo( testFixtureC2.getProperty( "TEST_PORT" ) );
        assertThat( testFixtureC2.service( ANY, Service.class ).value )
            .isEqualTo( testFixtureC.getProperty( "TEST_PORT" ) );
    }

    @Test
    public void testConfdIsolation() {
        TestDirectoryFixture testDirectoryFixture = new TestDirectoryFixture();
        KernelFixture kernelFixture1 = new KernelFixture1(
            testDirectoryFixture,
            urlOfTestResource( KernelFixtureTest.class, "application-fixture-confd.conf" ),
            List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
        ).withConfResource( getClass(), "/oap/application/testng/KernelFixtureTest/application-confd1.conf" );
        KernelFixture kernelFixture2 = new KernelFixture2(
            testDirectoryFixture,
            urlOfTestResource( KernelFixtureTest.class, "application-fixture-confd.conf" ),
            List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
        ).withConfResource( getClass(), "/oap/application/testng/KernelFixtureTest/application-confd2.conf" );

        Fixtures f = Fixtures.fixtures( testDirectoryFixture, kernelFixture1, kernelFixture2 );
        try {
            f.fixBeforeMethod();

            assertThat( kernelFixture1.service( ANY, Service.class ).value )
                .isEqualTo( 123 );
            assertThat( kernelFixture2.service( ANY, Service.class ).value )
                .isEqualTo( 1 );
        } finally {
            f.fixAfterMethod();
        }
    }

    public static class Service {
        public int value;
    }

    public static class TestFixture extends AbstractKernelFixture<TestFixture> {
        public TestFixture() {
            super( new TestDirectoryFixture(), urlOfTestResource( KernelFixtureTest.class, "application.fixture.conf" ),
                List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
            );
            definePort( "TEST_PORT" );
        }
    }

    public static class TestFixtureC extends AbstractKernelFixture<TestFixtureC> {
        public TestFixtureC( String name ) {
            super( new TestDirectoryFixture(),
                urlOfTestResource( KernelFixtureTest.class, "application.fixture-" + name + ".conf" ),
                List.of( urlOfTestResource( KernelFixtureTest.class, "oap-module.conf" ) )
            );
            definePort( "TEST_PORT" );
        }
    }

    public static class KernelFixture1 extends KernelFixture {
        public KernelFixture1( TestDirectoryFixture testDirectoryFixture, URL conf ) {
            super( testDirectoryFixture, conf );
        }

        public KernelFixture1( TestDirectoryFixture testDirectoryFixture, URL conf, Path confd ) {
            super( testDirectoryFixture, conf, confd );
        }

        public KernelFixture1( TestDirectoryFixture testDirectoryFixture, URL conf, List<URL> additionalModules ) {
            super( testDirectoryFixture, conf, additionalModules );
        }

        public KernelFixture1( TestDirectoryFixture testDirectoryFixture, URL conf, Path confd, List<URL> additionalModules ) {
            super( testDirectoryFixture, conf, confd, additionalModules );
        }
    }

    public static class KernelFixture2 extends KernelFixture {
        public KernelFixture2( TestDirectoryFixture testDirectoryFixture, URL conf ) {
            super( testDirectoryFixture, conf );
        }

        public KernelFixture2( TestDirectoryFixture testDirectoryFixture, URL conf, Path confd ) {
            super( testDirectoryFixture, conf, confd );
        }

        public KernelFixture2( TestDirectoryFixture testDirectoryFixture, URL conf, List<URL> additionalModules ) {
            super( testDirectoryFixture, conf, additionalModules );
        }

        public KernelFixture2( TestDirectoryFixture testDirectoryFixture, URL conf, Path confd, List<URL> additionalModules ) {
            super( testDirectoryFixture, conf, confd, additionalModules );
        }
    }
}
