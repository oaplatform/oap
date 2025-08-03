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

package oap.http.pniov3;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.http.Http;
import oap.http.Http.ContentType;
import oap.http.server.nio.NioHttpServer;
import oap.io.Closeables;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.Http.StatusCode.OK;
import static oap.http.Http.StatusCode.TOO_MANY_REQUESTS;
import static oap.http.test.HttpAsserts.assertPost;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class PnioHttpHandlerTest extends Fixtures {
    @Test( invocationCount = 100 )
    public void testProcess() throws IOException {
        ComputeTask<TestState> task = pnioExchange -> {
            TestHandler.TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandler.TestHandlerOptions.builder( false );

            TestHandler.handle( "cpu-1", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );
            TestHandler.handle( "cpu-2", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            String name = pnioExchange.runAsyncTask( "async-4", TestHandler.async( "async-4" ) );
            assertThat( name ).isEqualTo( "async-4" );

            TestHandler.handle( "cpu-6", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            name = pnioExchange.runAsyncTask( "async-7", TestHandler.async( "async-7" ) );
            assertThat( name ).isEqualTo( "async-7" );
            name = pnioExchange.runAsyncTask( "async-8", TestHandler.async( "async-8" ) );
            assertThat( name ).isEqualTo( "async-8" );

            TestHandler.handle( "cpu-9", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( task, port -> {

            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( OK )
                .hasContentType( ContentType.TEXT_PLAIN );
        } );
    }

    @Test
    public void testProcessWithException() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2", builder -> builder.runtimeException( new RuntimeException( "test exception" ) ) );

        runWithWorkflow( task, port -> {
            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.BAD_GATEWAY )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "test exception" );
        } );
    }

    @Test
    public void testProcessGzip() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( task, port -> {
            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.OK );

            try( CloseableHttpClient client = HttpClientBuilder.create()
                .disableContentCompression()
                .build() ) {

                String request = "{}";
                HttpEntity entity = EntityBuilder.create()
                    .setText( request )
                    .setContentType( org.apache.http.entity.ContentType.APPLICATION_JSON )
                    .gzipCompress()
                    .build();
                HttpPost post = new HttpPost( "http://localhost:" + port + "/test" );
                post.setEntity( entity );

                CloseableHttpResponse actionPostRequest = client.execute( post );
                assertThat( actionPostRequest.getStatusLine().getStatusCode() ).isEqualTo( 200 );
                actionPostRequest = client.execute( post );
                assertThat( actionPostRequest.getStatusLine().getStatusCode() ).isEqualTo( 200 );
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }

    @Test
    public void testRequestBufferOverflow() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 2, 1024, 5, 1000, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "REQUEST_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testResponseBufferOverflow() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 1024, 2, 5, 1000, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "BO" );
        } );
    }

    @Test
    public void testReject() throws IOException {
        AtomicInteger runAfterTimeout = new AtomicInteger( 0 );

        ComputeTask<TestState> task = pnioExchange -> {
            pnioExchange.runAsyncTask( "async", _ -> {
                Threads.sleepSafely( 10000 );
                return CompletableFuture.completedFuture( null );
            } );

            runAfterTimeout.incrementAndGet();

            pnioExchange.complete();
            pnioExchange.response();
        };

        try( CloseableHttpAsyncClient client = HttpAsyncClients
            .custom()
            .setMaxConnPerRoute( 100_000 )
            .setMaxConnTotal( 100_000 )
            .build() ) {
            client.start();


            runWithWorkflow( 1024, 1024, 1, 1000, 100000, task, port -> {
                ArrayList<CompletableFuture<HttpResponse>> futures = new ArrayList<>();

                for( int i = 0; i < 10_000; i++ ) {
                    CompletableFuture<HttpResponse> future = new CompletableFuture<>();
                    HttpPost httpPost = new HttpPost( "http://localhost:" + port + "/test" );
                    httpPost.setEntity( new ByteArrayEntity( "[{}]".getBytes( UTF_8 ) ) );
                    client.execute( httpPost, new FutureCallback<>() {
                        @Override
                        public void completed( HttpResponse httpResponse ) {
                            future.complete( httpResponse );

                        }

                        @Override
                        public void failed( Exception e ) {
                            future.completeExceptionally( e );
                        }

                        @Override
                        public void cancelled() {
                            future.completeExceptionally( new CancellationException() );
                        }
                    } );
                    futures.add( future );
                }

                CompletableFuture.allOf( futures.toArray( new CompletableFuture[futures.size()] ) ).join();

                AtomicLong counter200 = new AtomicLong( 0 );
                AtomicLong counter429 = new AtomicLong( 0 );
                AtomicLong counterOther = new AtomicLong( 0 );

                for( CompletableFuture<HttpResponse> future : futures ) {
                    switch( future.resultNow().getStatusLine().getStatusCode() ) {
                        case OK -> counter200.incrementAndGet();
                        case TOO_MANY_REQUESTS -> counter429.incrementAndGet();
                        default -> counterOther.incrementAndGet();
                    }
                }

                log.info( "OK {} TOO_MANY_REQUESTS {} other {}", counter200, counter429, counterOther );
                assertThat( counter200.get() ).isGreaterThan( 0 );
                assertThat( counter429.get() ).isGreaterThan( 0 );
                assertThat( counterOther.get() ).isZero();
            } );
        }
    }

    @Test
    public void testTimeoutAsync() throws IOException {
        AtomicInteger runAfterTimeout = new AtomicInteger( 0 );

        ComputeTask<TestState> task = pnioExchange -> {
            pnioExchange.runAsyncTask( "async", TestHandler.async( "async", builder -> builder.sleepTime( Dates.s( 5 ) ) ) );

            runAfterTimeout.incrementAndGet();

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( 1024, 1024, 1, 1000, 1000, task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
        } );

        assertThat( runAfterTimeout.get() ).isEqualTo( 0 );
    }

    private void runWithWorkflow( ComputeTask<TestState> task, Consumer<Integer> cons ) throws IOException {
        runWithWorkflow( 1024, 1024, 10, 1000, Dates.s( 100 ), task, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads,
                                  int maxThreads, long timeout, ComputeTask<TestState> task, Consumer<Integer> cons ) throws IOException {
        int port = Ports.getFreePort( getClass() );

        PnioHttpHandler.PnioHttpSettings settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( requestSize )
            .responseSize( responseSize )
            .build();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            try( PnioController pnioController = new PnioController( ioThreads, maxThreads ) ) {
                PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( "test", settings, task, new TestPnioListener(), pnioController );
                httpServer.bind( "/test",
                    exchange -> httpHandler.handleRequest( exchange, timeout, new TestState() ), false );

                cons.accept( port );
            }
        }
    }

    @Slf4j
    public static class TestPnioListener implements PnioListener<TestState> {
        private static void defaultResponse( PnioExchange<?> pnioExchange ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_REQUEST;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( pnioExchange.printState() );

            pnioExchange.send();
        }

        @Override
        @SneakyThrows
        public void onDone( PnioExchange<TestState> pnioExchange ) {
            if( log.isDebugEnabled() ) {
                String data = "name 'TestResponseBuilder thread '" + Thread.currentThread().getName()/*.substring( 7, 11 )*/
                    + "' new thread " + !pnioExchange.requestState.oldThreadName.equals( Thread.currentThread().getName() );

                log.debug( data );
            }

            OutputStream outputStream = null;
            try {
                PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
                outputStream = httpResponse.responseBuffer.getOutputStream();

                if( pnioExchange.gzipSupported() ) {
                    outputStream = new GZIPOutputStream( outputStream );
                    httpResponse.headers.put( Http.Headers.CONTENT_ENCODING, "gzip" );
                }
                outputStream.write( pnioExchange.requestState.sb.toString().getBytes( UTF_8 ) );

                httpResponse.status = OK;
                httpResponse.contentType = ContentType.TEXT_PLAIN;
            } finally {
                Closeables.close( outputStream );
            }

            pnioExchange.send();
        }

        @Override
        public void onException( PnioExchange<TestState> pnioExchange ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_GATEWAY;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( pnioExchange.throwable.getMessage() );

            pnioExchange.send();
        }

        @Override
        public void onRequestBufferOverflow( PnioExchange<TestState> pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onRejected( PnioExchange<TestState> pnioExchange ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.TOO_MANY_REQUESTS;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( pnioExchange.printState() );

            pnioExchange.send();
        }

        @Override
        public void onTimeout( PnioExchange<TestState> pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onUnknown( PnioExchange<TestState> pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onResponseBufferOverflow( PnioExchange<TestState> pnioExchange ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_REQUEST;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( "BO" );

            pnioExchange.send();
        }
    }
}
