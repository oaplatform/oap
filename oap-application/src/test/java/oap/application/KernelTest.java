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
import lombok.val;
import oap.application.ServiceOne.Complex;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class KernelTest extends AbstractTest {
    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        Application.unregisterServices();
    }

    @Test
    public void stopCloseable() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/start_stop.conf" ) );

        Kernel kernel = new Kernel( modules, emptyList() );
        kernel.start();
        TestCloseable tc = Application.service( "c1" );
        TestCloseable2 tc2 = Application.service( "c2" );
        kernel.stop();

        assertThat( tc.closed ).isTrue();
        assertThat( tc2.closed ).isFalse();
        assertThat( tc2.stopped ).isTrue();
    }

    @Test
    public void dynamicConfigurations() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "dynaconf/dynaconf.conf" ) );
        Env.deployTestData( getClass(), "dynaconf" );
        Kernel kernel = new Kernel( modules, emptyList() );
        try {
            kernel.start( pathOfTestResource( getClass(), "dynaconf/application.conf" ) );
            Dynaconf dynaconf = Application.service( "c1" );
            assertString( dynaconf.x.value.parameter ).isEqualTo( "valueUpdated" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void start() {
        System.setProperty( "failedValue", "value that can fail config parsing" );
        List<URL> modules = Lists.of(
            urlOfTestResource( getClass(), "modules/m1.conf" ),
            urlOfTestResource( getClass(), "modules/m2.json" )
        );

        Kernel kernel = new Kernel( modules, emptyList() );
        try {
            kernel.start( pathOfTestResource( getClass(), "application.conf" ),
                pathOfTestResource( getClass(), "conf.d" ) );
            assertEventually( 50, 1, () -> {
                ServiceOne one = Application.service( ServiceOne.class );
                ServiceTwo two = Application.service( ServiceTwo.class );

                assertNotNull( one );
                assertThat( one.i ).isEqualTo( 2 );
                assertThat( one.i2 ).isEqualTo( 100 );
                Complex expected = new Complex( 2 );
                expected.map = Maps.of( __( "a", new Complex( 1 ) ) );
                assertThat( one.complex ).isEqualTo( expected );
                assertThat( one.complexes ).contains( new Complex( 2 ) );
                assertNotNull( two );
                assertThat( two.j ).isEqualTo( 3000 );
                assertThat( two.one ).isSameAs( one );
                assertTrue( two.started );
                ServiceScheduled scheduled = Application.service( ServiceScheduled.class );
                assertNotNull( scheduled );
                assertTrue( scheduled.executed );

                ServiceDepsList depsList = Application.service( ServiceDepsList.class );
                assertNotNull( depsList );
                assertThat( depsList.deps ).contains( one, two );

                assertThat( one.listener ).isSameAs( two );

//                dont do this kind of things now.
//                ServiceOne.ComplexMap complexMap = Application.service( ServiceOne.ComplexMap.class );
                //                assertThat( one.complexMap ).isSameAs( complexMap );
            } );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void disabledServices() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "modules/m3.conf" ) );

        Kernel kernel = new Kernel( modules, emptyList() );
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
    public void map() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "modules/m4.conf" ) );

        Kernel kernel = new Kernel( modules, emptyList() );
        try {
            kernel.start();

            assertThat( Application.<ServiceOne>service( "s1" ).map ).hasSize( 2 );
            assertThat( Application.<ServiceOne>service( "s1" ).map.get( "test1" ) ).isInstanceOf( ServiceOne.class );
            assertThat( Application.<ServiceOne>service( "s1" ).map.get( "test2" ) ).isInstanceOf( ServiceOne.class );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void abstractModules() {
        List<URL> modules = asList(
            urlOfTestResource( getClass(), "modules/abs.conf" ),
            urlOfTestResource( getClass(), "modules/impl.conf" )
        );

        Kernel kernel = new Kernel( modules, emptyList() );
        try {
            kernel.start();

            val serviceOneP1 = Application.<ServiceOne>service( "ServiceOneP1" );
            val cm = Application.<Complex>service( "comp" );

            assertThat( serviceOneP1 ).isNotNull();
            assertThat( serviceOneP1.complex ).isNotNull();
            assertThat( cm ).isNotNull();
            assertThat( serviceOneP1.complex ).isSameAs( cm );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testPlugins() {
        val kernel = new Kernel(
            singletonList( urlOfTestResource( getClass(), "plugins/oap-module.conf" ) ),
            singletonList( urlOfTestResource( getClass(), "plugins/oap-plugin.conf" ) )
        );

        try {
            kernel.start();
            val service1 = Application.service( Service1.class );
            val service2 = Application.service( Service2.class );

            assertThat( service1.ref ).isSameAs( service2 );
            assertThat( service1.list ).hasSize( 1 );
            assertThat( service1.list.get( 0 ) ).isSameAs( service2 );
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

    public static class Dynaconf {
        DynamicConfig<DynaconfCfg> x;
    }

    public static class DynaconfCfg {
        String parameter;
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

