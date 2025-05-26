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

package oap.http.pniov2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.http.Http.ContentType;
import oap.http.server.nio.NioHttpServer;
import oap.io.Closeables;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static oap.http.Http.StatusCode.OK;
import static oap.http.test.HttpAsserts.assertPost;
import static org.assertj.core.api.Assertions.assertThat;

public class PnioHttpHandlerTest extends Fixtures {
    @Test( invocationCount = 100 )
    public void testProcess() throws IOException {
        ComputeTask<TestPnioExchange> task = pnioExchange -> {
            TestHandler.TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandler.TestHandlerOptions.builder( false );

            TestHandler.handle( "cpu-1", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );
            TestHandler.handle( "cpu-2", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            String name = pnioExchange.runAsyncTask( TestHandler.async( "async-4" ) );
            assertThat( name ).isEqualTo( "async-4" );

            TestHandler.handle( "cpu-6", "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            name = pnioExchange.runAsyncTask( TestHandler.async( "async-7" ) );
            assertThat( name ).isEqualTo( "async-7" );
            name = pnioExchange.runAsyncTask( TestHandler.async( "async-8" ) );
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
        ComputeTask<TestPnioExchange> task = TestHandler.compute( "cpu-2", builder -> builder.runtimeException( new RuntimeException( "test exception" ) ) );

        runWithWorkflow( task, port -> {
            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.BAD_GATEWAY )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "test exception" );
        } );
    }

    @Test
    public void testRequestBufferOverflow() throws IOException {
        ComputeTask<TestPnioExchange> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 2, 1024, 5, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "REQUEST_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testResponseBufferOverflow() throws IOException {
        ComputeTask<TestPnioExchange> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 1024, 2, 5, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "BO" );
        } );
    }

    @Test
    public void testTimeoutAsync() throws IOException {
        ComputeTask<TestPnioExchange> task = pnioExchange -> {
            pnioExchange.runAsyncTask( TestHandler.async( "async", builder -> builder.sleepTime( Dates.s( 5 ) ) ) );

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( 1024, 1024, 1, 200, task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "DONE, TIMEOUT" );
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "DONE, TIMEOUT" );
        } );
    }

    private void runWithWorkflow( ComputeTask task, Consumer<Integer> cons ) throws IOException {
        runWithWorkflow( 1024, 1024, 10, Dates.s( 100 ), task, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads,
                                  long timeout, ComputeTask task, Consumer<Integer> cons ) throws IOException {
        int port = Ports.getFreePort( getClass() );

        AbstractPnioHttpHandler.PnioHttpSettings settings = AbstractPnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( requestSize )
            .responseSize( responseSize )
            .build();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            try( PnioController pnioController = new PnioController( ioThreads ) ) {
                TestPnioHandler httpHandler = new TestPnioHandler( settings, task, new TestPnioListener(), pnioController );
                httpServer.bind( "/test",
                    exchange -> httpHandler.handleRequest( exchange, timeout ), false );

                cons.accept( port );
            }
        }
    }

    @Slf4j
    public static class TestPnioListener implements PnioListener<TestPnioExchange> {
        private static void defaultResponse( TestPnioExchange pnioExchange ) {
            AbstractPnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_REQUEST;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( pnioExchange.printState() );

            pnioExchange.send();
        }

        @Override
        @SneakyThrows
        public void onDone( TestPnioExchange pnioExchange ) {
            if( log.isDebugEnabled() ) {
                String data = "name 'TestResponseBuilder thread '" + Thread.currentThread().getName().substring( 7, 11 )
                    + "' new thread " + !pnioExchange.oldThreadName.equals( Thread.currentThread().getName() );

                log.debug( data );
            }

            OutputStream outputStream = null;
            try {
                AbstractPnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
                outputStream = httpResponse.responseBuffer.getOutputStream();

                if( pnioExchange.gzipSupported() ) {
                    outputStream = new GZIPOutputStream( outputStream );
                    httpResponse.headers.put( Http.Headers.CONTENT_ENCODING, "gzip" );
                }
                outputStream.write( pnioExchange.sb.toString().getBytes( StandardCharsets.UTF_8 ) );

                httpResponse.status = OK;
                httpResponse.contentType = ContentType.TEXT_PLAIN;
            } finally {
                Closeables.close( outputStream );
            }

            pnioExchange.send();
        }

        @Override
        public void onException( TestPnioExchange pnioExchange ) {
            AbstractPnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_GATEWAY;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( pnioExchange.throwable.getMessage() );

            pnioExchange.send();
        }

        @Override
        public void onRequestBufferOverflow( TestPnioExchange pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onRejected( TestPnioExchange pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onTimeout( TestPnioExchange pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onUnknown( TestPnioExchange pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onResponseBufferOverflow( TestPnioExchange pnioExchange ) {
            AbstractPnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_REQUEST;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            httpResponse.responseBuffer.setAndResize( "BO" );

            pnioExchange.send();
        }
    }
}
