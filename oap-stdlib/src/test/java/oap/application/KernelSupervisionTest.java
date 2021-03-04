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

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelSupervisionTest {
    @Test
    public void testSupervisionThread() {
        var modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/thread.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "thread" ) );

            var srv = kernel.serviceOfClass2( TestThread.class );

            assertEventually( 100, 100, () ->
                assertThat( srv.count.get() ).isGreaterThan( 1 )
            );
            assertEventually( 100, 100, () -> assertThat( srv.count.get() ).isGreaterThan( 10 ) );
        }
    }

    @Test
    public void testSupervisionCron() {
        var modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/cron.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "cron" ) );

            var srv = kernel.serviceOfClass2( TestCron.class );

            assertEventually( 100, 100, () ->
                assertThat( srv.count.get() ).isGreaterThan( 1 )
            );
            assertEventually( 100, 100, () -> assertThat( srv.count.get() ).isGreaterThan( 3 ) );
        }
        assertThat( TestCron.str.toString() ).isEqualTo( "" );
    }

    @Test
    public void testSupervisionCronWithSupervise() {
        var modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/cronWithSupervise.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "cron" ) );

            var srv = kernel.serviceOfClass2( TestCron.class );

            assertEventually( 100, 100, () ->
                assertThat( srv.count.get() ).isGreaterThan( 1 )
            );
            assertEventually( 100, 100, () -> assertThat( srv.count.get() ).isGreaterThan( 3 ) );
        }

        assertThat( TestCron.str.toString() ).isEqualTo( "start/stop/" );
    }

    @Test
    public void testStopCloseable() {
        var modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/start_stop.conf" ) );

        var kernel = new Kernel( modules );
        kernel.start( Map.of( "boot.main", "start_stop" ) );
        var tc = kernel.<TestCloseable>service( "*:c1" ).orElseThrow();
        var tc2 = kernel.<TestCloseable2>service( "*:c2" ).orElseThrow();
        kernel.stop();

        assertThat( tc.closed ).isTrue();
        assertThat( tc2.closed ).isFalse();
        assertThat( tc2.stopped ).isTrue();
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
    public static class TestThread implements Runnable {
        public final AtomicLong count = new AtomicLong();
        public volatile boolean done = false;

        @Override
        public void run() {
            log.info( "run..." );
            while( !done ) {
                count.incrementAndGet();
                log.info( "loop" );
                try {
                    Thread.sleep( 1 );
                } catch( InterruptedException e ) {
                    done = true;
                }
            }
            log.info( "run... Done!" );
        }
    }

    @Slf4j
    public static class TestCron implements Runnable {
        public static final StringBuilder str = new StringBuilder();
        public final AtomicLong count = new AtomicLong();

        public synchronized void start() {
            str.append( "start/" );
        }

        public synchronized void stop() {
            str.append( "stop/" );
        }

        @Override
        public void run() {
            log.info( "run" );
            count.incrementAndGet();
        }
    }
}
