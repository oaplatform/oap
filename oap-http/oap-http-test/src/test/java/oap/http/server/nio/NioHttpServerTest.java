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

package oap.http.server.nio;

import io.micrometer.core.instrument.Tags;
import oap.http.Http;
import oap.http.client.OapHttpClient;
import oap.http.server.nio.handlers.BlockingReadTimeoutHandler;
import oap.http.server.nio.handlers.CompressionNioHandler;
import oap.http.server.nio.handlers.KeepaliveRequestsHandler;
import oap.http.server.nio.health.HealthHttpHandler;
import oap.io.Resources;
import oap.metrics.MetricsFixture;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static oap.http.Http.Headers.CONNECTION;
import static oap.http.Http.Headers.DATE;
import static oap.http.test.HttpAsserts.assertGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NioHttpServerTest extends Fixtures {
    private final MetricsFixture metrics = fixture( new MetricsFixture() );

    @Test
    public void testResponseHeaders() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.start();

            assertGet( "http://localhost:" + port + "/" )
                .hasHeadersSize( 3 )
                .containsHeader( DATE )
                .containsHeader( CONNECTION );
        }

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.alwaysSetDate = false;
            httpServer.alwaysSetKeepAlive = false;
            httpServer.start();

            assertGet( "http://localhost:" + port + "/" )
                .hasHeadersSize( 1 )
                .doesNotContainHeader( DATE )
                .doesNotContainHeader( CONNECTION );
        }
    }

    @Test
    public void testBindToSpecificPort() throws IOException {
        int port = Ports.getFreePort( getClass() );
        int testPort = Ports.getFreePort( getClass() );
        int testPort2 = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.handlers.add( new KeepaliveRequestsHandler( 1000 ) );
            httpServer.handlers.add( new CompressionNioHandler() );
            httpServer.handlers.add( new BlockingReadTimeoutHandler( Dates.s( 60 ) ) );

            httpServer.additionalHttpPorts.put( "test", testPort );
            httpServer.additionalHttpPorts.put( "test2", testPort2 );

            httpServer.bind( "/test", exchange -> exchange.responseOk( "test", Http.ContentType.TEXT_PLAIN ), "test" );
            httpServer.bind( "/test2", exchange -> exchange.responseOk( "test2", Http.ContentType.TEXT_PLAIN ), "test2" );

            httpServer.start();
            httpServer.bind( "/test3", exchange -> exchange.responseOk( "test3", Http.ContentType.TEXT_PLAIN ), "test2" );

            assertGet( "http://localhost:" + testPort + "/test" ).body().isEqualTo( "test" );
            assertGet( "http://localhost:" + testPort2 + "/test2" ).body().isEqualTo( "test2" );
            assertGet( "http://localhost:" + testPort2 + "/test3" ).body().isEqualTo( "test3" );
        }
    }

    /**
     * keytool -genkey -alias ssl -keyalg RSA -keysize 2048 -dname "CN=localhost,OU=IT" -keystore master.jks -storepass 1234567 -keypass 1234567
     */
    @Test
    public void testHttps() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        ClientConnector connector = new ClientConnector();

        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setKeyStorePath( Resources.filePath( getClass(), "/oap/http/test_https.jks" ).get() );
        sslContextFactory.setKeyStorePassword( "1234567" );
        connector.setSslContextFactory( sslContextFactory );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( httpPort, httpsPort, Resources.urlOrThrow( getClass(), "/oap/http/test_https.jks" ), "1234567" ) );
             org.eclipse.jetty.client.HttpClient httpClient = OapHttpClient.customHttpClient().transport( new HttpClientTransportDynamic( connector ) ).build() ) {
            httpClient.start();

            new TestHttpHandler( httpServer, "/test", "default-https" );
            new HealthHttpHandler( httpServer, "/healtz", "default-http" ).start();

            httpServer.start();

            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
            assertGet( "http://localhost:" + httpPort + "/healtz" ).hasCode( Http.StatusCode.NO_CONTENT );
        }
    }

    @Test
    public void testHttpsSslHandshakePool() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );
        Tags tags = Tags.of( "port", String.valueOf( httpsPort ) );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort );
             org.eclipse.jetty.client.HttpClient httpClient = httpsClient() ) {
            httpServer.sslHandshakeThreads = 2;
            new TestHttpHandler( httpServer, "/test", "default-https" );

            httpServer.start();

            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );

            assertThat( httpServer.sslHandshakeExecutor.getCorePoolSize() ).isEqualTo( 2 );
            assertThat( metrics.get( NioHttpServer.NIO_SSL_HANDSHAKE_COMPLETED, tags ).functionCounter().count() ).isGreaterThan( 0 );
            assertThat( metrics.get( NioHttpServer.NIO_SSL_HANDSHAKE_TASK, tags ).timer().count() ).isGreaterThan( 0 );
            metrics.assertMetric( NioHttpServer.NIO_SSL_HANDSHAKE, tags.and( "type", "queue" ) ).isGauge().isEqualTo( 0 );
            metrics.assertMetric( NioHttpServer.NIO_SSL_HANDSHAKE, tags.and( "type", "active" ) ).isGauge();
            metrics.assertMetric( NioHttpServer.NIO_SSL_HANDSHAKE_REJECTED, tags ).isCounter().isEqualTo( 0 );
        }
    }

    @Test
    public void testHttpsSslHandshakeRejected() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );
        Tags tags = Tags.of( "port", String.valueOf( httpsPort ) );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort );
             org.eclipse.jetty.client.HttpClient httpClient = httpsClient() ) {
            httpServer.sslHandshakeThreads = 1;
            httpServer.sslHandshakeMinThreads = 1;
            httpServer.sslHandshakeQueue = 1;
            new TestHttpHandler( httpServer, "/test", "default-https" );
            new HealthHttpHandler( httpServer, "/healtz", "default-http" ).start();

            httpServer.start();

            CountDownLatch latch = new CountDownLatch( 1 );
            try {
                // the only handshake thread is busy, the queue is full
                httpServer.sslHandshakeExecutor.execute( () -> awaitUninterruptibly( latch ) );
                httpServer.sslHandshakeExecutor.execute( () -> awaitUninterruptibly( latch ) );

                assertThatThrownBy( () -> httpClient.newRequest( "https://localhost:" + httpsPort + "/test" )
                    .timeout( 10, TimeUnit.SECONDS )
                    .send() )
                    .isInstanceOfAny( ExecutionException.class, TimeoutException.class );
                assertThat( metrics.get( NioHttpServer.NIO_SSL_HANDSHAKE_REJECTED, tags ).counter().count() ).isGreaterThan( 0 );

                // io threads are free
                assertGet( "http://localhost:" + httpPort + "/healtz" ).hasCode( Http.StatusCode.NO_CONTENT );
            } finally {
                latch.countDown();
            }

            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
        }
    }

    @Test
    public void testHttpsSslHandshakeInline() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort );
             org.eclipse.jetty.client.HttpClient httpClient = httpsClient() ) {
            httpServer.sslHandshakeOffload = false;
            new TestHttpHandler( httpServer, "/test", "default-https" );

            httpServer.start();

            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
            assertThat( httpServer.sslHandshakeExecutor ).isNull();
        }
    }

    @Test
    public void testHttpsSslHandshakeThreadsRelativeToCores() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.sslHandshakeThreads = -1;

            httpServer.start();

            assertThat( httpServer.sslHandshakeExecutor.getCorePoolSize() )
                .isEqualTo( Math.max( 2, Runtime.getRuntime().availableProcessors() - 1 ) );
        }
    }

    @Test
    public void testHttpsSslHandshakeMinThreads() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.sslHandshakeThreads = 1;

            httpServer.start();

            assertThat( httpServer.sslHandshakeExecutor.getCorePoolSize() ).isEqualTo( 2 );
        }
    }

    @Test
    public void testHttpsSslHandshakeOffloadRuntime() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );
        Tags tags = Tags.of( "port", String.valueOf( httpsPort ) );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.sslHandshakeOffload = false;
            new TestHttpHandler( httpServer, "/test", "default-https" );

            httpServer.start();

            assertHttpsOk( httpsPort );
            assertThat( httpServer.sslHandshakeExecutor ).isNull();

            httpServer.setSslHandshakeOffload( true );

            ThreadPoolExecutor executor = httpServer.sslHandshakeExecutor;
            assertThat( executor ).isNotNull();
            assertHttpsOk( httpsPort );
            double completed = metrics.get( NioHttpServer.NIO_SSL_HANDSHAKE_COMPLETED, tags ).functionCounter().count();
            assertThat( completed ).isGreaterThan( 0 );

            httpServer.setSslHandshakeOffload( false );

            assertThat( httpServer.sslHandshakeExecutor ).isNull();
            assertThat( executor.isShutdown() ).isTrue();
            assertHttpsOk( httpsPort );
            assertThat( metrics.get( NioHttpServer.NIO_SSL_HANDSHAKE_COMPLETED, tags ).functionCounter().count() )
                .isGreaterThanOrEqualTo( completed );
        }
    }

    @Test
    public void testHttpsSslHandshakeThreadsRuntime() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.sslHandshakeThreads = 2;
            new TestHttpHandler( httpServer, "/test", "default-https" );

            httpServer.start();

            ThreadPoolExecutor executor = httpServer.sslHandshakeExecutor;
            assertThat( executor.getCorePoolSize() ).isEqualTo( 2 );

            httpServer.setSslHandshakeThreads( 3 );
            assertThat( httpServer.sslHandshakeExecutor ).isSameAs( executor );
            assertThat( executor.getCorePoolSize() ).isEqualTo( 3 );
            assertThat( executor.getMaximumPoolSize() ).isEqualTo( 3 );

            httpServer.setSslHandshakeMinThreads( 4 );
            assertThat( executor.getCorePoolSize() ).isEqualTo( 4 );
            assertThat( executor.getMaximumPoolSize() ).isEqualTo( 4 );

            httpServer.setSslHandshakeMinThreads( 1 );
            httpServer.setSslHandshakeThreads( 1 );
            assertThat( httpServer.sslHandshakeExecutor ).isSameAs( executor );
            assertThat( executor.getCorePoolSize() ).isEqualTo( 1 );
            assertThat( executor.getMaximumPoolSize() ).isEqualTo( 1 );

            assertHttpsOk( httpsPort );
        }
    }

    @Test
    public void testHttpsSslHandshakeQueueRuntime() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );
        Tags tags = Tags.of( "port", String.valueOf( httpsPort ) );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.sslHandshakeQueue = 1;
            new TestHttpHandler( httpServer, "/test", "default-https" );

            httpServer.start();

            ThreadPoolExecutor executor = httpServer.sslHandshakeExecutor;
            assertThat( executor.getQueue().remainingCapacity() ).isEqualTo( 1 );

            httpServer.setSslHandshakeQueue( 8 );

            assertThat( httpServer.sslHandshakeExecutor ).isNotSameAs( executor );
            assertThat( httpServer.sslHandshakeExecutor.getQueue().remainingCapacity() ).isEqualTo( 8 );
            assertThat( executor.isShutdown() ).isTrue();

            assertHttpsOk( httpsPort );
            metrics.assertMetric( NioHttpServer.NIO_SSL_HANDSHAKE_REJECTED, tags ).isCounter().isEqualTo( 0 );
        }
    }

    @Test
    public void testHttpsSslHandshakeSetterBeforeStart() throws Exception {
        int httpPort = Ports.getFreePort( getClass() );
        int httpsPort = Ports.getFreePort( getClass() );

        try( NioHttpServer httpServer = httpsServer( httpPort, httpsPort ) ) {
            httpServer.setSslHandshakeThreads( 3 );
            httpServer.setSslHandshakeOffload( true );
            assertThat( httpServer.sslHandshakeExecutor ).isNull();

            httpServer.start();

            assertThat( httpServer.sslHandshakeExecutor.getCorePoolSize() ).isEqualTo( 3 );
        }
    }

    private void assertHttpsOk( int httpsPort ) throws Exception {
        // a new client - a new TLS handshake
        try( org.eclipse.jetty.client.HttpClient httpClient = httpsClient() ) {
            assertGet( httpClient, "https://localhost:" + httpsPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
        }
    }

    private NioHttpServer httpsServer( int httpPort, int httpsPort ) {
        return new NioHttpServer( new NioHttpServer.DefaultPort( httpPort, httpsPort, Resources.urlOrThrow( getClass(), "/oap/http/test_https.jks" ), "1234567" ) );
    }

    private org.eclipse.jetty.client.HttpClient httpsClient() throws Exception {
        ClientConnector connector = new ClientConnector();

        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setKeyStorePath( Resources.filePath( getClass(), "/oap/http/test_https.jks" ).get() );
        sslContextFactory.setKeyStorePassword( "1234567" );
        connector.setSslContextFactory( sslContextFactory );

        org.eclipse.jetty.client.HttpClient httpClient = OapHttpClient.customHttpClient().transport( new HttpClientTransportDynamic( connector ) ).build();
        httpClient.start();
        return httpClient;
    }

    private static void awaitUninterruptibly( CountDownLatch latch ) {
        try {
            latch.await();
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    public static class TestHttpHandler implements HttpHandler {
        public TestHttpHandler( NioHttpServer server, String prefix, String port ) {
            server.bind( prefix, this, port );
        }

        @Override
        public void handleRequest( HttpServerExchange exchange ) {
            assertThat( Thread.currentThread().isVirtual() ).isTrue();

            exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
        }
    }
}
