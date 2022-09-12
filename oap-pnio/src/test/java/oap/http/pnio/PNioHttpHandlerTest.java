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
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static oap.http.testng.HttpAsserts.assertPost;

public class PNioHttpHandlerTest extends Fixtures {
    private final EnvFixture envFixture;

    public PNioHttpHandlerTest() {
        envFixture = fixture( new EnvFixture() );
    }

    @Test
    public void testProcess() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestTask( "cpu-1", true ) )
            .next( new TestTask( "cpu-2", true ) )
            .next( new TestTask( "io-1", false ) )
            .next( new TestTask( "io-2", false ) )
            .next( new TestTask( "cpu-3", true ) )
            .next( new TestTask( "cpu-4", true ) )
            .build( new TestResponseTask() );

        runWithWorkflow( workflow, port -> {

            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.OK )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( """
                    name 'cpu-1' cpu true thread 'cp' new thread true
                    name 'cpu-2' cpu true thread 'cp' new thread false
                    name 'io-1' cpu false thread 'XN' new thread true
                    name 'io-2' cpu false thread 'XN' new thread false
                    name 'cpu-3' cpu true thread 'cp' new thread true
                    name 'cpu-4' cpu true thread 'cp' new thread false""" );
        } );
    }

    @Test
    public void testProcessWithException() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestTask( "cpu-2", true ).withException( new RuntimeException( "test exception" ) ) )
            .build( new TestResponseTask() );

        runWithWorkflow( workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "{}" )
                .hasCode( Http.StatusCode.BAD_GATEWAY )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "java.lang.RuntimeException: test exception" );
        } );
    }

    @Test
    public void testRequestBufferOverflow() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestTask( "cpu-2", true ) )
            .build( new TestResponseTask() );

        runWithWorkflow( 2, 1024, 5, 4, Dates.s( 100 ), workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "REQUEST_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testResponseBufferOverflow() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestTask( "cpu-2", true ) )
            .build( new TestResponseTask() );

        runWithWorkflow( 1024, 2, 5, 4, Dates.s( 100 ), workflow, port -> {
            assertPost( "http://localhost:" + port + "/test", "[{}]" )
                .hasCode( Http.StatusCode.BAD_REQUEST )
                .hasContentType( Http.ContentType.TEXT_PLAIN )
                .hasBody( "RESPONSE_BUFFER_OVERFLOW" );
        } );
    }

    @Test
    public void testTimeout() throws IOException {
        RequestWorkflow<TestState> workflow = RequestWorkflow.init( new TestTask( "cpu", true ).withSleepTime( Dates.s( 20 ) ) )
            .build( new TestResponseTask() );

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

    private void getResponse( RequestTaskState<TestState> requestTaskState, TestState workflowState, RequestTaskState.HttpResponse httpResponse ) {
        switch( requestTaskState.processState ) {
            case EXCEPTION -> {
                httpResponse.status = Http.StatusCode.BAD_GATEWAY;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                httpResponse.setBody( requestTaskState.throwable.getMessage() );
            }
            case REQUEST_BUFFER_OVERFLOW, RESPONSE_BUFFER_OVERFLOW, TIMEOUT, REJECTED -> {
                httpResponse.status = Http.StatusCode.BAD_REQUEST;
                httpResponse.contentType = Http.ContentType.TEXT_PLAIN;
                httpResponse.setBody( requestTaskState.processState.name() );
            }
        }
    }

    private void runWithWorkflow( RequestWorkflow<TestState> workflow, Consumer<Integer> cons ) throws IOException {
        runWithWorkflow( 1024, 1024, 5, 4, Dates.s( 100 ), workflow, cons );
    }

    private void runWithWorkflow( int requestSize, int responseSize, int ioThreads, int cpuThreads, long timeout, RequestWorkflow<TestState> workflow, Consumer<Integer> cons ) throws IOException {
        int port = envFixture.portFor( "pnio" );
        try( PNioHttpHandler<TestState> httpHandler = new PNioHttpHandler<>( requestSize, responseSize, 0.99, cpuThreads,
            true, -1, workflow, this::getResponse );
             NioHttpServer httpServer = new NioHttpServer( port ) ) {
            httpServer.ioThreads = ioThreads;
            httpServer.start();

            httpServer.bind( "/test",
                exchange -> httpHandler.handleRequest( exchange, System.nanoTime(), timeout, new TestState() ) );

            cons.accept( port );
        }
    }
}
