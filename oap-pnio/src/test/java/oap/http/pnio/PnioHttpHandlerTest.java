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

import oap.highload.Affinity;
import oap.http.Http;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import oap.util.Dates;
import org.testng.annotations.Test;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.util.function.Consumer;

import static oap.http.pnio.PnioRequestHandler.Type.COMPUTE;
import static oap.http.pnio.PnioRequestHandler.Type.IO;
import static oap.http.testng.HttpAsserts.assertPost;

public class PnioHttpHandlerTest extends Fixtures {
    private final EnvFixture envFixture;

    public PnioHttpHandlerTest() {
        envFixture = fixture( new EnvFixture() );
    }

    @Test
    public void testProcess() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestHandler( "cpu-1", COMPUTE ) )
            .next( new TestHandler( "cpu-2", COMPUTE ) )
            .next( new TestHandler( "io-1", IO ) )
            .next( new TestHandler( "io-2", IO ) )
            .next( new TestHandler( "cpu-3", COMPUTE ) )
            .next( new TestHandler( "cpu-4", COMPUTE ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( workflow, port -> {

            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.OK )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( """
                    name 'cpu-1' type COMPUTE thread 'cp' new thread true
                    name 'cpu-2' type COMPUTE thread 'cp' new thread false
                    name 'io-1' type IO thread 'XN' new thread true
                    name 'io-2' type IO thread 'XN' new thread false
                    name 'cpu-3' type COMPUTE thread 'cp' new thread true
                    name 'cpu-4' type COMPUTE thread 'cp' new thread false""" );
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
                .hasBody( "java.lang.RuntimeException: test exception" );
        } );
    }

    @Test
    public void testRequestBufferOverflow() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu-2", COMPUTE ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 2, 1024, 5, 4, Dates.s( 100 ), workflow, port -> {
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

        runWithWorkflow( 1024, 2, 5, 4, Dates.s( 100 ), workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "BO" );
        } );
    }

    @Test
    public void testTimeout() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler( "cpu", COMPUTE ).withSleepTime( Dates.s( 20 ) ) )
            .next( new TestResponseBuilder() )
            .build();

        runWithWorkflow( 1024, 1024, 5, 1, 200, workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "TIMEOUT" );
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "REJECTED" );
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
        runWithWorkflow( 1024, 1024, 5, 4, Dates.s( 100 ), workflow, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads, int cpuThreads, long timeout, RequestWorkflow<TestState> workflow, Consumer<Integer> cons ) throws IOException {
        int port = envFixture.portFor( "pnio" );
        var settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( requestSize )
            .responseSize( responseSize )
            .queueTimeoutPercent( 0.99 )
            .cpuThreads( cpuThreads )
            .cpuQueueFair( true )
            .cpuAffinity( new Affinity( "0" ) )
            .ioAffinity( new Affinity( "1+" ) )
            .build();
        try( PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( settings, workflow, this::errorResponse );
             NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            httpServer.bind( "/test",
                new HttpHandler() {
                    @Override
                    public void init( XnioWorker xnioWorker ) {
                        httpHandler.init( xnioWorker );
                    }

                    @Override
                    public void handleRequest( HttpServerExchange exchange ) throws Exception {
                        httpHandler.handleRequest( exchange, System.nanoTime(), timeout, new TestState() );
                    }
                } );

            cons.accept( port );
        }
    }
}
