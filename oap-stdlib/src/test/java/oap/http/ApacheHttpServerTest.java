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

package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedThread;
import oap.concurrent.Threads;
import oap.http.cors.GenericCorsPolicy;
import oap.http.server.apache.ApacheHttpServer;
import oap.http.server.apache.PlainHttpListener;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.testng.Asserts.assertEventually;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ApacheHttpServerTest extends Fixtures {

    private final EnvFixture envFixture;
    public static final String PORT = "PORT";

    {
        envFixture = fixture( new EnvFixture() );
    }

    @Test
    public void withoutQueue() throws InterruptedException {
        SynchronizedThread listener = null;

        var semaphore = new Semaphore( 0 );

        try( var server = new ApacheHttpServer( 1, 1, false ) ) {
            server.bind( "test", GenericCorsPolicy.DEFAULT, ( request, response ) -> {
                semaphore.release();
                Threads.sleepSafely( 100000 );
                response.respond( HttpResponse.status( 200 ).response() );
            }, Protocol.HTTP );
            server.start();

            var http = new PlainHttpListener( server, envFixture.portFor( PORT ) );
            listener = new SynchronizedThread( http );
            listener.start();

            CompletableFuture.runAsync( () -> assertPost( "http://localhost:" + envFixture.portFor( PORT ) + "/test/", "{}" ).isOk() );
            semaphore.acquire();

            CompletableFuture.runAsync( () -> assertPost( "http://localhost:" + envFixture.portFor( PORT ) + "/test/", "{}" ).isOk() );

            assertEventually( 10, 1000, () -> {
                var activeCount = server.getActiveCount();
                var queueSize = server.getQueueSize();

                System.out.println( activeCount + " / " + queueSize );

                assertThat( activeCount ).isEqualTo( 1 );
                assertThat( queueSize ).isEqualTo( 1 );
            } );

            var body = RandomStringUtils.random( 1_000_000 );

            assertPost( "http://localhost:" + envFixture.portFor( PORT ) + "/test/", body ).hasCode( HTTP_UNAVAILABLE );
        } finally {
            if( listener != null )
                listener.stop();
        }
    }

    /**
     * we need some way for it test to work
     */
    @Test( enabled = false )
    public void streamingError() {
        SynchronizedThread listener = null;

        try( var server = new ApacheHttpServer( 1, 1, false ) ) {
            server.bind( "test", GenericCorsPolicy.DEFAULT, ( request, response ) -> {
                response.respond( HttpResponse.outputStream( stream -> {
                    throw new RuntimeException( "fail" );
                }, TEXT_PLAIN ).response() );
            }, Protocol.HTTP );
            server.start();

            var http = new PlainHttpListener( server, envFixture.portFor( PORT ) );
            listener = new SynchronizedThread( http );
            listener.start();

            assertGet( "http://localhost:" + envFixture.portFor( PORT ) + "/test" )
                .hasCode( HTTP_INTERNAL_ERROR );
        } finally {
            if( listener != null )
                listener.stop();
        }
    }
}
