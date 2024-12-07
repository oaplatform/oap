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

package oap.http.pnio;

import oap.http.Http;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static oap.http.pnio.PnioRequestHandler.Type.ASYNC;
import static oap.http.pnio.PnioRequestHandler.Type.BLOCK;
import static oap.http.pnio.PnioRequestHandler.Type.COMPUTE;
import static oap.http.test.HttpAsserts.assertPost;

public class PnioHttpHandlerTest extends Fixtures {
    @Test
    public void testProcess() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu-1", COMPUTE ) )
            .next( new TestHandler( "cpu-2", COMPUTE ) )
            .next( new TestHandler( "block-3", BLOCK ) )
            .next( new TestHandler( "async-4", ASYNC ) )
            .next( new TestHandler( "block-5", BLOCK ) )
            .next( new TestHandler( "cpu-6", COMPUTE ) )
            .next( new TestHandler( "async-7", ASYNC ) )
            .next( new TestHandler( "cpu-8", COMPUTE ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( workflow, port -> {

            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.OK )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( """
                    name 'cpu-1' type COMPUTE thread 'I/O-' new thread false
                    name 'cpu-2' type COMPUTE thread 'I/O-' new thread false
                    name 'block-3' type BLOCK thread 'task' new thread true
                    name 'async-4' type ASYNC thread 'I/O-' new thread true
                    name 'block-5' type BLOCK thread 'task' new thread true
                    name 'cpu-6' type COMPUTE thread 'I/O-' new thread true
                    name 'async-7' type ASYNC thread 'I/O-' new thread false
                    name 'cpu-8' type COMPUTE thread 'I/O-' new thread false"""
                );
        } );
    }

    @Test
    public void testProcessWithException() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu-2", COMPUTE ).withException( new RuntimeException( "test exception" ) ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.BAD_GATEWAY )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "test exception" );
        } );
    }

    @Test
    public void testRequestBufferOverflow() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu-2", COMPUTE ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 2, 1024, 5, Dates.s( 100 ), workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "REQUEST_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testResponseBufferOverflow() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu-2", COMPUTE ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 1024, 2, 5, Dates.s( 100 ), workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "BO" );
        } );
    }

    @Test
    public void testTimeoutBlock() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "block", BLOCK ).withSleepTime( Dates.s( 20 ) ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 1024, 1024, 1, 200, workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
        } );
    }

    @Test
    public void testTimeoutAsync() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "async", ASYNC ).withSleepTime( Dates.s( 5 ) ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 1024, 1024, 1, 200, workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
        } );
    }

    private void errorResponse( PnioExchange<TestState> pnioExchange, TestState workflowState ) {
        var httpResponse = pnioExchange.httpResponse;
        switch( pnioExchange.processState ) {
            case EXCEPTION -> {
                httpResponse.status = Http.StatusCode.BAD_GATEWAY;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                pnioExchange.responseBuffer.setAndResize( pnioExchange.throwable.getMessage() );
            }
            case RESPONSE_BUFFER_OVERFLOW -> {
                httpResponse.status = Http.StatusCode.BAD_REQUEST;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                pnioExchange.responseBuffer.setAndResize( "BO" );
            }
            case REQUEST_BUFFER_OVERFLOW, TIMEOUT, REJECTED -> {
                httpResponse.status = Http.StatusCode.BAD_REQUEST;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                pnioExchange.responseBuffer.setAndResize( pnioExchange.processState.name() );
            }
            default -> {
                httpResponse.status = Http.StatusCode.NO_CONTENT;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                pnioExchange.responseBuffer.setAndResize( "DEFAULT" );
            }
        }
    }

    private void runWithWorkflow( RequestWorkflow<TestState> workflow, Consumer<Integer> cons ) throws IOException {
        runWithWorkflow( 1024, 1024, 5, Dates.s( 100 ), workflow, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads, long timeout, RequestWorkflow<TestState> workflow, Consumer<Integer> cons ) throws IOException {
        int port = Ports.getFreePort( getClass() );

        var settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( requestSize )
            .responseSize( responseSize )
            .build();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            try( PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( httpServer, settings, workflow, this::errorResponse ) ) {

                httpServer.bind( "/test",
                    exchange -> httpHandler.handleRequest( exchange, System.nanoTime(), timeout, new TestState() ) );

                cons.accept( port );
            }
        }
    }
}
