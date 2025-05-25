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

public class PnioHttpHandlerTest extends Fixtures {
    @Test
    public void testProcess() throws IOException {
        ComputeTask<TestState> task = ( pnioExchange, testState ) -> {
            TestHandler.TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandler.TestHandlerOptions.builder( false );

            TestHandler.handle( "cpu-1", "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );
            TestHandler.handle( "cpu-2", "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );

            AsyncRunnable block3 = pnioExchange.blockingTask( TestHandler.block( "block3" ) );
            block3.fork();
            block3.join();

            AsyncRunnable async4 = pnioExchange.asyncTask( TestHandler.async( "async-4" ) );
            async4.fork();
            async4.join();

            AsyncRunnable block5 = pnioExchange.blockingTask( TestHandler.block( "block5" ) );
            block5.fork();
            block5.join();

            TestHandler.handle( "cpu-6", "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );

            AsyncRunnable async7 = pnioExchange.asyncTask( TestHandler.async( "async-7" ) );
            async7.fork();
            async7.join();
            AsyncRunnable async8 = pnioExchange.asyncTask( TestHandler.async( "async-8" ) );
            async8.fork();
            async8.join();

            TestHandler.handle( "cpu-9", "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( task, port -> {

            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( OK )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( """
                    name 'cpu-1' type COMPUTE thread 'nPoo' new thread true
                    name 'cpu-2' type COMPUTE thread 'nPoo' new thread false
                    name 'block3' type BLOCK thread 'BLK-' new thread true
                    name 'async-4' type ASYNC thread 'nPoo' new thread true
                    name 'block5' type BLOCK thread 'BLK-' new thread true
                    name 'cpu-6' type COMPUTE thread 'nPoo' new thread true
                    name 'async-7' type ASYNC thread 'nPoo' new thread false
                    name 'async-8' type ASYNC thread 'nPoo' new thread false
                    name 'cpu-9' type COMPUTE thread 'nPoo' new thread false"""
                );
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
    public void testRequestBufferOverflow() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 2, 1024, 5, 40, 10, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "REQUEST_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testResponseBufferOverflow() throws IOException {
        ComputeTask<TestState> task = TestHandler.compute( "cpu-2" );

        runWithWorkflow( 1024, 2, 5, 40, 10, Dates.s( 100 ), task, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( ContentType.TEXT_PLAIN )
                .hasBody( "BO" );
        } );
    }

    @Test
    public void testTimeoutBlock() throws IOException {
        ComputeTask<TestState> task = ( pnioExchange, testState ) -> {
            AsyncRunnable block = pnioExchange.blockingTask( TestHandler.block( "async", builder -> builder.sleepTime( Dates.s( 20 ) ) ) );
            block.fork();
            block.join();

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( 1024, 1024, 1, 40, 10, 200, task, port -> {
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

    @Test
    public void testTimeoutAsync() throws IOException {
        ComputeTask<TestState> task = ( pnioExchange, testState ) -> {
            AsyncRunnable block = pnioExchange.asyncTask( TestHandler.async( "async", builder -> builder.sleepTime( Dates.s( 5 ) ) ) );
            block.fork();
            block.join();

            pnioExchange.complete();
            pnioExchange.response();
        };

        runWithWorkflow( 1024, 1024, 1, 40, 10, 200, task, port -> {
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

    private void runWithWorkflow( ComputeTask<TestState> task, Consumer<Integer> cons ) throws IOException {
        runWithWorkflow( 1024, 1024, 10, 5, 10, Dates.s( 100 ), task, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads, int maxQueueSize, int blockingPoolSize,
                                  long timeout, ComputeTask<TestState> task, Consumer<Integer> cons ) throws IOException {
        int port = Ports.getFreePort( getClass() );

        PnioHttpHandler.PnioHttpSettings settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( requestSize )
            .responseSize( responseSize )
            .build();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            try( PnioController pnioController = new PnioController( ioThreads, blockingPoolSize ) ) {
                PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( settings, task, new TestPnioListener(), pnioController );
                httpServer.bind( "/test",
                    exchange -> httpHandler.handleRequest( exchange, timeout, new TestState() ), false );

                cons.accept( port );
            }
        }
    }

    @Slf4j
    public static class TestPnioListener implements PnioListener<TestState> {
        private static void defaultResponse( PnioExchange<TestState> pnioExchange ) {
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            httpResponse.status = Http.StatusCode.BAD_REQUEST;
            httpResponse.contentType = ContentType.TEXT_PLAIN;
            pnioExchange.responseBuffer.setAndResize( pnioExchange.printState() );

            pnioExchange.send();
        }

        @Override
        @SneakyThrows
        public void onDone( PnioExchange<TestState> pnioExchange ) {
            if( log.isDebugEnabled() ) {
                String data = "name 'TestResponseBuilder thread '" + Thread.currentThread().getName().substring( 7, 11 )
                    + "' new thread " + !pnioExchange.workflowState.oldThreadName.equals( Thread.currentThread().getName() );

                log.debug( data );
            }

            OutputStream outputStream = null;
            try {
                outputStream = pnioExchange.responseBuffer.getOutputStream();

                if( pnioExchange.gzipSupported() ) {
                    outputStream = new GZIPOutputStream( outputStream );
                    pnioExchange.httpResponse.headers.put( Http.Headers.CONTENT_ENCODING, "gzip" );
                }
                outputStream.write( pnioExchange.workflowState.sb.toString().getBytes( StandardCharsets.UTF_8 ) );

                pnioExchange.httpResponse.status = OK;
                pnioExchange.httpResponse.contentType = ContentType.TEXT_PLAIN;
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
            pnioExchange.responseBuffer.setAndResize( pnioExchange.throwable.getMessage() );

            pnioExchange.send();
        }

        @Override
        public void onRequestBufferOverflow( PnioExchange<TestState> pnioExchange ) {
            defaultResponse( pnioExchange );
        }

        @Override
        public void onRejected( PnioExchange<TestState> pnioExchange ) {
            defaultResponse( pnioExchange );
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
            pnioExchange.responseBuffer.setAndResize( "BO" );

            pnioExchange.send();
        }
    }
}
