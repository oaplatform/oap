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

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.impl.ConfigImpl;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.ServiceOne.Complex;
import oap.application.module.Module;
import oap.concurrent.Threads;
import oap.system.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import static oap.application.KernelTest.Enum.ONE;
import static oap.application.KernelTest.Enum.TWO;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.testng.Assert.assertTrue;

public class KernelTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public KernelTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }


    @BeforeMethod
    public void beforeMethod() {
        Env.set( "APPLICATION_STOP_DETECT_TIMEOUT", String.valueOf( 1 ) );
    }

    @AfterMethod
    public void afterMethod() {
        Env.set( "APPLICATION_STOP_DETECT_TIMEOUT", null );

        new ArrayList<>( System.getenv().keySet() )
            .stream()
            .filter( k -> k.startsWith( "CONFIG." ) )
            .forEach( k -> Env.set( k, null ) );
    }

    @Test
    public void testLifecycle() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/lifecycle.conf" ) );

        TestLifecycle service;
        TestLifecycle thread;
        TestLifecycle delayScheduled;

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "lifecycle" ) );

            service = kernel.<TestLifecycle>service( "lifecycle", "service" ).orElseThrow();
            thread = kernel.<TestLifecycle>service( "lifecycle.thread" ).orElseThrow();
            delayScheduled = kernel.<TestLifecycle>service( "lifecycle.delayScheduled" ).orElseThrow();
        }

        assertThat( service.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
        assertThat( thread.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
        assertThat( delayScheduled.str.toString() ).isEqualTo( "/preStart/start/preStop/stop" );
    }

    @Test
    public void start() {
        System.setProperty( "failedValue", "value that can fail config parsing" );
        var modules = List.of(
            url( "modules/m1.conf" ),
            url( "modules/m2.conf" ),
            url( "modules/m3.conf" )
        );

        var kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "application.conf" ),
                pathOfTestResource( getClass(), "conf.d" ) );
            assertEventually( 50, 1, () -> {
                Optional<ServiceOne> serviceOne = kernel.service( "m1.ServiceOneP1" );
                Optional<ServiceTwo> serviceTwo = kernel.service( "m2.ServiceTwo" );

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
                Optional<ServiceScheduled> serviceScheduled = kernel.service( "m2.ServiceScheduled" );
                assertThat( serviceScheduled ).isPresent().get().satisfies( scheduled ->
                    assertThat( scheduled.executed ).isTrue()
                );

                Optional<ServiceDepsList> serviceDepsList = kernel.service( "m3.ServiceDepsList" );
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
        var modules = List.of( url( "disabled/disabled.conf" ) );

        var kernel = new Kernel( modules );
        try {
            kernel.start( Map.of( "boot.main", "disabled" ) );

            assertThat( kernel.<ServiceOne>service( "disabled.s1" ) ).isPresent().get()
                .satisfies( s1 -> assertThat( s1.list ).isEmpty() );
            assertThat( kernel.<ServiceOne>service( "disabled.s2" ) ).isNotPresent();
        } finally {
            kernel.stop();
        }
    }

    public URL url( String s ) {
        return urlOfTestResource( getClass(), s );
    }

    @Test
    public void map() {
        List<URL> modules = List.of( url( "map/map.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( Map.of( "boot.main", "map" ) );

            assertThat( kernel.<ServiceOne>service( "*.s1" ) ).isPresent().get()
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
        List<URL> modules = List.of( url( "modules/map.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( Map.of( "boot.main", "map" ) );

            assertThat( kernel.<TestServiceMap>service( "*.ServiceMap" ) ).isPresent().get()
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
        var modules = List.of( url( "env/env.conf" ) );

        Env.set( "CONFIG.services.env.s1.enabled", "false" );
        Env.set( "CONFIG.services.env.s2.parameters.val", "\"test$value\"" );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "env" ) );

            assertThat( kernel.<Service1>service( "*.s1" ) ).isNotPresent();
            assertThat( kernel.<Service2>service( "*.s2" ) ).isPresent();
            assertThat( kernel.<Service2>service( "*.s2" ) ).isPresent().get()
                .satisfies( s2 -> assertThat( s2.val ).isEqualTo( "test$value" ) );
        }
    }

    @Test
    public void testUnknownService() {
        var modules = List.of( url( "env/env.conf" ) );

        Env.set( "CONFIG.services.env.s1.enabled", "false" );
        Env.set( "CONFIG.services.env.s2.parameters.val", "\"test$value\"" );
        Env.set( "CONFIG.services.env.unknownservice.val1", "false" );

        try( var kernel = new Kernel( modules ) ) {
            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "env" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "unknown application configuration services: env.[unknownservice]" );
        }
    }

    @Test
    public void testReference() {
        var modules = List.of( url( "reference/reference.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatCode( () -> kernel.start( Map.of( "boot.main", "reference" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "[reference:s1] dependencies are not enabled. Required service [s2] is disabled by 'enabled' flag." );
        }
    }

    @Test
    public void testCyclicReferences() {
        var modules = List.of(
            url( "reference/cyclic.conf" ),
            url( "reference/cyclic2.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatCode( () -> kernel.start( Map.of( "boot.main", "cyclic1" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "cyclic dependency detected" );
        }
    }

    @Test
    public void testServiceWithoutImplementation() {
        var modules = List.of( url( "modules/service-without-implementation.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            assertThatCode( () -> kernel.start( Map.of( "boot.main", "service-without-implementation" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessage( "failed to initialize service: service-without-implementation:test-service-without-implementation. implementation == null" );
        }
    }

    @Test
    public void testLoadModules() {
        var modules = List.of(
            url( "deps/m1.conf" ),
            url( "deps/m2.conf" ),
            url( "deps/m3.conf" ),
            url( "deps/m4.conf" ),
            url( "deps/activeByDefault.conf" ),
            url( "deps/m5.conf" )
        );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot", Map.of( "main", "m1", "allowActiveByDefault", true ) ) );

            assertThat( kernel.service( "m1.s11" ) ).isPresent();
            assertThat( kernel.service( "m2.s21" ) ).isNotPresent();
            assertThat( kernel.service( "m1.s31" ) ).isNotPresent();
            assertThat( kernel.service( "m3.s31" ) ).isPresent();
            assertThat( kernel.service( "m4.s41" ) ).isPresent();
            assertThat( kernel.service( "activeByDefault.sa" ) ).isPresent();
            assertThat( kernel.service( "m5.s5" ) ).isPresent();
        }

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "m1" ) );

            assertThat( kernel.service( "activeByDefault.sa" ) ).isNotPresent();
            assertThat( kernel.service( "m5.s5" ) ).isNotPresent();
        }
    }

    @Test
    public void testDuplicateServices() {
        var modules = List.of(
            url( "duplicate/d1.conf" ),
            url( "duplicate/d2.conf" )
        );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", List.of( "d1", "d2" ) ) );

            assertThat( kernel.ofClass( ServiceOne.class ) ).hasSize( 2 );
            assertThat( kernel.service( "d1.ServiceOne" ) ).isPresent();
            assertThat( kernel.<ServiceOne>service( "d1.ServiceOne" ).get().i ).isEqualTo( 1 );
            assertThat( kernel.service( "d2.ServiceOne" ) ).isPresent();
            assertThat( kernel.<ServiceOne>service( "d2.ServiceOne" ).get().i ).isEqualTo( 2 );
        }
    }

    @Test
    public void testListOfEnums() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "enum.conf" ) )
        );
        try {
            kernel.start( Map.of( "boot.main", "enum" ) );
            var enumList = kernel.serviceOfClass( TestEnum.class ).orElseThrow();
            assertThat( enumList.enums ).containsExactly( ONE, TWO );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testBeanFromString() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "beanFromString.conf" ) )
        );
        try {
            kernel.start( Map.of( "boot.main", "beanFromString" ) );
            var s3 = kernel.serviceOfClass( Service3.class ).orElseThrow();
            assertThat( s3.name ).isEqualTo( "a" );
            assertThat( s3.service3 ).isNotNull();
            assertThat( s3.service3.name ).isEqualTo( "b()" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testBeanFromClasspath() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "beanFromClasspath.conf" ) )
        );
        try {
            kernel.start( Map.of( "boot.main", "beanFromClasspath" ) );
            var s3 = kernel.serviceOfClass( Service3.class ).orElseThrow();
            assertThat( s3.name ).isEqualTo( "a" );
            assertThat( s3.service3 ).isNotNull();
            assertThat( s3.service3.name ).isEqualTo( "from resource" );
            assertThat( s3.service3.timeout ).isEqualTo( 1234 );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testBeanFromJsonField() {
        List<URL> modules = List.of( urlOfTestResource( getClass(), "testBeanFromJsonField.conf" ) );

        Env.set( "VAR_SERVICE3FIELD", "{\"name\":\"from resource\",\"timeout\":1234}" );
        ConfigFactory.invalidateCaches();

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "testBeanFromJsonField" ) );
            var s3 = kernel.serviceOfClass( Service3.class ).orElseThrow();
            assertThat( s3.name ).isEqualTo( "a" );
            assertThat( s3.service3Field ).isNotNull();
            assertThat( s3.service3Field.name ).isEqualTo( "from resource" );
            assertThat( s3.service3Field.timeout ).isEqualTo( 1234 );
        }
    }

    @Test
    public void testMapFromClasspath() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "mapFromClasspath.conf" ) )
        );
        try {
            kernel.start( Map.of( "boot.main", "mapFromClasspath" ) );
            var s4 = kernel.serviceOfClass( Service4.class ).orElseThrow();
            assertThat( s4.services ).containsOnly( entry( "a", new Service3( "a" ) ), entry( "b", new Service3( "b" ) ) );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testBeanFromPath() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "beanFromPath.conf" ) )
        );
        try {
            testDirectoryFixture.deployTestData( getClass() );
            System.setProperty( "TEST_PATH", testDirectoryFixture.testDirectory().toString() );
            ConfigImpl.reloadSystemPropertiesConfig();

            kernel.start( Map.of( "boot.main", "beanFromPath" ) );
            var s3 = kernel.serviceOfClass( Service3.class ).orElseThrow();
            assertThat( s3.name ).isEqualTo( "a" );
            assertThat( s3.service3 ).isNotNull();
            assertThat( s3.service3.name ).isEqualTo( "from path" );
        } finally {
            kernel.stop();
            TestDirectoryFixture.deleteDirectory( testDirectoryFixture.testDirectory() );
        }
    }

    @Test
    public void testFinalParameter() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "testFinalParameter.conf" ) )
        );

        try {
            testDirectoryFixture.deployTestData( getClass() );


            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "testFinalParameter" ) ) )
                .isInstanceOf( ApplicationException.class )
                .hasMessageContaining( "al=[val1], a=new value" );
        } finally {
            kernel.stop();
            TestDirectoryFixture.deleteDirectory( testDirectoryFixture.testDirectory() );
        }
    }

    @Test
    public void testInclude() {
        try( var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "testInclude.conf" ) ) ) ) {
            kernel.start( Map.of( "boot.main", "testInclude" ) );

            assertThat( kernel.<Service3>service( "testInclude.service3" ).get().name ).isEqualTo( "a" );
            assertThat( kernel.<Service3>service( "testInclude.service32" ).get().name ).isEqualTo( "b" );
        }
    }

    @Test
    public void testDisableModule() {
        try( var kernel = new Kernel(
            List.of(
                urlOfTestResource( getClass(), "testDisableModule/oap-module-testDisableModule.conf" ),
                urlOfTestResource( getClass(), "testDisableModule/oap-module-testDisableModule-disabled.conf" )
            ) ) ) {
            kernel.start( Map.of( "boot.main", "testDisableModule" ) );

            assertThat( kernel.<Service1>service( "testDisableModule.service1" ).get().list ).isEmpty();
        }
    }

    public enum Enum {
        ONE, TWO
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

    @ToString
    @EqualsAndHashCode
    public static class Service3 {
        public Service3 service3;
        public Service3 service3Field;
        public String name;
        public int timeout;

        public Service3( String name ) {
            this.name = name;
        }

        public Service3( String name, Service3 service3 ) {
            this.name = name;
            this.service3 = service3;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class Service4 {
        public final HashMap<String, Service3> services = new HashMap<>();
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
            Threads.sleepSafely( 2 );
            str.append( "/stop" );
        }

        @Override
        public void run() {
            var done = false;
            while( !done ) try {
                Thread.sleep( 1 );
            } catch( InterruptedException e ) {
                done = true;
            }
        }
    }

    public static class TestEnum {
        public final List<Enum> enums = Lists.empty();

        public TestEnum( List<Enum> enums ) {
            this.enums.addAll( enums );
        }
    }

    public static class ServiceFinalParameter {
        public final String a = "test";
        public final ArrayList<String> al = new ArrayList<>();
        private final String extra;

        public ServiceFinalParameter( String extra ) {
            this.extra = extra;
        }
    }

    public static class TestQueueContainer {
        public final Queue<String> queueArg;
        public Queue<String> queueField;

        public TestQueueContainer( Queue<String> queueArg ) {
            this.queueArg = queueArg;
        }
    }
}
