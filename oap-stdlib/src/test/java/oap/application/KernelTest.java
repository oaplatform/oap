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
import lombok.extern.slf4j.Slf4j;
import oap.application.ServiceOne.Complex;
import oap.application.linked.ServiceContainee;
import oap.application.linked.ServiceContainer;
import oap.concurrent.Threads;
import oap.testng.Env;
import oap.util.Lists;
import oap.util.Maps;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertTrue;

public class KernelTest {

    @AfterMethod
    public void afterMethod() {
        new ArrayList<>( System.getenv().keySet() ).stream().filter( k -> k.startsWith( "CONFIG." ) ).forEach( k -> Env.putEnv( k, null ) );
    }

    @Test
    public void testLifecycle() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/lifecycle.yaml" ) );

        TestLifecycle service;
        TestLifecycle thread;
        TestLifecycle delayScheduled;

        try( var kernel = new Kernel( modules ) ) {
            kernel.start();

            service = kernel.<TestLifecycle>service( "service" ).orElseThrow();
            thread = kernel.<TestLifecycle>service( "thread" ).orElseThrow();
            delayScheduled = kernel.<TestLifecycle>service( "delayScheduled" ).orElseThrow();
        }

        assertThat( service.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
        assertThat( thread.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
        assertThat( delayScheduled.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
    }

    @Test
    public void stopCloseable() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/start_stop.conf" ) );

        Kernel kernel = new Kernel( modules );
        kernel.start();
        TestCloseable tc = kernel.<TestCloseable>service( "c1" ).orElseThrow();
        TestCloseable2 tc2 = kernel.<TestCloseable2>service( "c2" ).orElseThrow();
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
                Optional<ServiceOne> serviceOne = kernel.service( "ServiceOne" );
                Optional<ServiceTwo> serviceTwo = kernel.service( "ServiceTwo" );

                assertThat( serviceOne ).isPresent().get().satisfies( one -> {
                    assertThat( one.kernel ).isSameAs( kernel );
                    assertThat( one.i ).isEqualTo( 2 );
                    assertThat( one.i2 ).isEqualTo( 100 );
                    Complex expected = new Complex( 2 );
                    expected.map = Maps.of( __( "a", new Complex( 1 ) ) );
                    assertThat( one.complex ).isEqualTo( expected );
                    assertThat( one.complexes ).contains( new Complex( 2 ) );
                } );
                assertThat( serviceTwo ).isPresent().get().satisfies( two -> {
                    assertThat( two.j ).isEqualTo( 3000 );
                    assertThat( two.one2 ).isSameAs( serviceOne.get() );
                    assertTrue( two.started );
                } );
                //wait for scheduled service to be executed
                Threads.sleepSafely( 2000 );
                Optional<ServiceScheduled> serviceScheduled = kernel.service( "ServiceScheduled" );
                assertThat( serviceScheduled ).isPresent().get().satisfies( scheduled ->
                    assertThat( scheduled.executed ).isTrue()
                );

                Optional<ServiceDepsList> serviceDepsList = kernel.service( "ServiceDepsList" );
                assertThat( serviceDepsList ).isPresent().get()
                    .satisfies( depsList -> assertThat( depsList.deps ).contains( serviceOne.get(), serviceTwo.get() ) );

                assertThat( serviceOne.get().listener ).isSameAs( serviceTwo.get() );

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

            assertThat( kernel.<ServiceOne>service( "s1" ) ).isPresent().get()
                .satisfies( s1 -> assertThat( s1.list ).isEmpty() );
            assertThat( kernel.<ServiceOne>service( "s2" ) ).isNotPresent();
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
            assertThat( kernel.<ServiceContainer>service( "container" ) ).isPresent().get().satisfies( container ->
                assertThat( kernel.<ServiceContainee>service( "containee1" ) ).isPresent().get().satisfies( containee1 ->
                    assertThat( kernel.<ServiceContainee>service( "containee2" ) ).isPresent().get().satisfies( containee2 -> {
                        assertThat( container.containees ).contains( containee1, containee2 );
                        assertThat( container.priorities ).containsExactly( containee2, containee1 );
                    } ) ) );
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

            assertThat( kernel.<ServiceOne>service( "s1" ) ).isPresent().get()
                .satisfies( s1 -> {
                    assertThat( s1.map ).hasSize( 2 );
                    assertThat( s1.map.get( "test1" ) ).isInstanceOf( ServiceOne.class );
                    assertThat( s1.map.get( "test2" ) ).isInstanceOf( ServiceOne.class );
                } );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void mapWithEntries() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "modules/map.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start();

            assertThat( kernel.<TestServiceMap>service( "ServiceMap" ) ).isPresent().get()
                .satisfies( sm -> {
                    assertThat( sm.map1 ).hasSize( 1 );
                    assertThat( sm.map1.get( "ok" ) ).isInstanceOf( TestServiceMap.TestEntry.class );
                    assertThat( sm.map1.get( "ok" ).i ).isEqualTo( 10 );
                } );
        } finally {
            kernel.stop();
        }

    }

    @Test
    public void mapEnvToConfig() {
        var modules = Lists.of( urlOfTestResource( getClass(), "env/env.conf" ) );

        Env.putEnv( "CONFIG.services.s1.enabled", "false" );
        Env.putEnv( "CONFIG.services.s2.parameters.val", "\"test$value\"" );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start();

            assertThat( kernel.<Service1>service( "s1" ) ).isNotPresent();
            assertThat( kernel.<Service2>service( "s2" ) ).isPresent();
            assertThat( kernel.<Service2>service( "s2" ) ).isPresent().get()
                .satisfies( s2 -> assertThat( s2.val ).isEqualTo( "test$value" ) );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testReference() {
        var modules = Lists.of( urlOfTestResource( getClass(), "reference/reference.conf" ) );

        var kernel = new Kernel( modules );
        try {
            assertThatCode( kernel::start )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "dependencies are not ready [s1]" );
        } finally {
            kernel.stop();
        }
    }

    @Slf4j
    public static class TestCloseable implements Closeable {

        public boolean closed;

        @Override
        public void close() {
            log.info( "log_close" );
            this.closed = true;
        }
    }

    @Slf4j
    public static class TestCloseable2 implements Closeable {
        public boolean stopped;
        public boolean closed;

        public void stop() {
            log.info( "log_stop" );
            this.stopped = true;

        }

        @Override
        public void close() {
            log.info( "log_close" );
            this.closed = true;
        }
    }

    @Slf4j
    public static class Service1 {
        public final List<Object> list = new ArrayList<>();
        public Object ref = null;
    }

    @ToString
    @EqualsAndHashCode
    public static class Service2 {
        private final Logger log = org.slf4j.LoggerFactory.getLogger( Service2.class );

        private final String val;

        public Service2( String val ) {
            this.val = val;
        }
    }

    public static class TestLifecycle implements Runnable {
        public final StringBuilder str = new StringBuilder();

        public void preStart() {
            str.append( "/preStart" );
        }

        public void start() {
            str.append( "/start" );
        }


        public void preStop() {
            str.append( "/preStop" );
        }

        public void stop() {
            str.append( "/stop" );
        }

        @Override
        public void run() {
            var done = false;
            while( !done ) {
                try {
                    Thread.sleep( 1 );
                } catch( InterruptedException e ) {
                    done = true;
                }
            }
        }
    }
}

