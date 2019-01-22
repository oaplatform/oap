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

package oap.application;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.application.ServiceOne.Complex;
import oap.application.linked.ServiceContainee;
import oap.application.linked.ServiceContainer;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class KernelTest extends AbstractTest {
    @BeforeMethod
    public void unregister() {
        Application.unregisterServices();
    }

    @Test
    public void stopCloseable() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/start_stop.conf" ) );

        Kernel kernel = new Kernel( modules );
        kernel.start();
        TestCloseable tc = Application.service( "c1" );
        TestCloseable2 tc2 = Application.service( "c2" );
        kernel.stop();

        assertThat( tc.closed ).isTrue();
        assertThat( tc2.closed ).isFalse();
        assertThat( tc2.stopped ).isTrue();
    }

    @Test
    public void start() {
        System.setProperty( "failedValue", "value that can fail config parsing" );
        List<URL> modules = Lists.of(
            urlOfTestResource( getClass(), "modules/m1.conf" ),
            urlOfTestResource( getClass(), "modules/m2.json" ),
            urlOfTestResource( getClass(), "modules/m3.yaml" )
        );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "application.conf" ),
                pathOfTestResource( getClass(), "conf.d" ) );
            assertEventually( 50, 1, () -> {
                ServiceOne one = Application.service( ServiceOne.class );
                ServiceTwo two = Application.service( ServiceTwo.class );

                assertNotNull( one );
//                assertThat( one.kernel ).isSameAs( kernel );
                assertThat( one.i ).isEqualTo( 2 );
                assertThat( one.i2 ).isEqualTo( 100 );
                Complex expected = new Complex( 2 );
                expected.map = Maps.of( __( "a", new Complex( 1 ) ) );
                assertThat( one.complex ).isEqualTo( expected );
                assertThat( one.complexes ).contains( new Complex( 2 ) );
                assertNotNull( two );
                assertThat( two.j ).isEqualTo( 3000 );
                assertThat( two.one2 ).isSameAs( one );
                assertTrue( two.started );
                ServiceScheduled scheduled = Application.service( ServiceScheduled.class );
                assertNotNull( scheduled );
                assertTrue( scheduled.executed );

                ServiceDepsList depsList = Application.service( ServiceDepsList.class );
                assertNotNull( depsList );
                assertThat( depsList.deps ).contains( one, two );

                assertThat( one.listener ).isSameAs( two );

//                dont do this kind of things now.
//                ServiceOne.ComplexMap complexMap = Application.service2( ServiceOne.ComplexMap.class );
                //                assertThat( one.complexMap ).isSameAs( complexMap );
            } );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void disabled() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "disabled/disabled.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start();

            assertThat( Application.<ServiceOne>service( "s1" ) ).isNotNull();
            assertThat( Application.<ServiceOne>service( "s1" ).list ).isEmpty();
            assertThat( Application.<ServiceOne>service( "s2" ) ).isNull();
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void linked() {
        List<URL> modules = Lists.of(
            urlOfTestResource( getClass(), "linked/container.conf" ),
            urlOfTestResource( getClass(), "linked/containee.conf" )
        );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start();

            ServiceContainer container = Application.service( "container" );
            ServiceContainee containee1 = Application.service( "containee1" );
            ServiceContainee containee2 = Application.service( "containee2" );
            assertThat( container ).isNotNull();
            assertThat( containee1 ).isNotNull();
            assertThat( containee2 ).isNotNull();
            assertThat( container.containees ).contains( containee1, containee2 );
            assertThat( container.priorities ).containsExactly( containee2, containee1 );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void map() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "map/map.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start();

            assertThat( Application.<ServiceOne>service( "s1" ).map ).hasSize( 2 );
            assertThat( Application.<ServiceOne>service( "s1" ).map.get( "test1" ) ).isInstanceOf( ServiceOne.class );
            assertThat( Application.<ServiceOne>service( "s1" ).map.get( "test2" ) ).isInstanceOf( ServiceOne.class );
        } finally {
            kernel.stop();
        }
    }

    public static class TestCloseable implements Closeable {

        public boolean closed;

        @Override
        public void close() {
            this.closed = true;
        }
    }

    public static class TestCloseable2 implements Closeable {
        public boolean stopped;
        public boolean closed;

        public void stop() {
            this.stopped = true;

        }

        @Override
        public void close() {
            this.closed = true;
        }
    }

    public static class Service1 {
        public final List<Object> list = new ArrayList<>();
        public Object ref = null;
    }

    @ToString
    @EqualsAndHashCode
    public static class Service2 {
        private final String val;

        public Service2( String val ) {
            this.val = val;
        }
    }
}

