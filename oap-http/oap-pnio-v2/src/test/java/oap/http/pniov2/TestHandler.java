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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class TestHandler {
    private TestHandler() {
    }

    public static PnioRequestHandler<TestState> compute( String name ) {
        return compute( name, _ -> {} );
    }

    public static PnioRequestHandler<TestState> compute( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );
        return new PnioRequestHandler<>( PnioRequestHandler.Type.COMPUTE ) {
            @Override
            public void handle( PnioExchange<TestState> pnioExchange, TestState testState ) throws InterruptedException {
                TestHandler.handle( name, "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );
            }
        };
    }

    public static PnioRequestHandler<TestState> block( String name ) {
        return block( name, _ -> {} );
    }

    public static PnioRequestHandler<TestState> block( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return new PnioRequestHandler<>( PnioRequestHandler.Type.BLOCKING ) {
            @Override
            public void handle( PnioExchange<TestState> pnioExchange, TestState testState ) throws InterruptedException, IOException {
                TestHandler.handle( name, "BLOCK", pnioExchange, testState, testHandlerOptionsBuilder.build() );
            }
        };
    }

    public static PnioRequestHandler<TestState> async( String name ) {
        return async( name, _ -> {} );
    }

    public static PnioRequestHandler<TestState> async( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( true );
        builder.accept( testHandlerOptionsBuilder );

        return new PnioRequestHandler<>( PnioRequestHandler.Type.ASYNC ) {
            @Override
            public void handle( PnioExchange<TestState> pnioExchange, TestState testState, Runnable success, Consumer<Throwable> exception ) throws InterruptedException {
                TestHandler.handle( name, "ASYNC", pnioExchange, testState, testHandlerOptionsBuilder
                    .exceptionCallback( exception )
                    .successCallback( success )
                    .build() );
            }
        };
    }

    public static void handle( String name, String type, PnioExchange<TestState> pnioExchange, TestState testState,
                               TestHandlerOptions testHandlerOptions ) throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();

        String data = "name '" + name + "' type " + type + " thread '" + currentThreadName.substring( 7, 11 )
            + "' new thread " + !testState.oldThreadName.equals( currentThreadName );

        log.debug( data );

        if( !testState.sb.isEmpty() ) {
            testState.sb.append( "\n" );
        }

        testState.sb.append( data );

        testState.oldThreadName = currentThreadName;

        if( testHandlerOptions.runtimeException != null ) {
            if( testHandlerOptions.async ) {
                testHandlerOptions.exceptionCallback.accept( testHandlerOptions.runtimeException );
            } else {
                throw testHandlerOptions.runtimeException;
            }
        } else if( testHandlerOptions.sleepTime != null ) {
            if( testHandlerOptions.async ) {
                CompletableFuture.runAsync( () -> {
                        Threads.sleepSafely( testHandlerOptions.sleepTime );
                    } )
                    .thenRun( () -> testHandlerOptions.successCallback.run() );
            } else {
                Thread.sleep( testHandlerOptions.sleepTime );
            }
        } else if( testHandlerOptions.async ) {
            CompletableFuture.runAsync( () -> {
                    Threads.sleepSafely( 500 );
                } )
                .thenRun( () -> testHandlerOptions.successCallback.run() );
        }
    }

    @Builder( builderMethodName = "" )
    public static class TestHandlerOptions {
        public RuntimeException runtimeException;
        public Runnable successCallback;
        public Consumer<Throwable> exceptionCallback;
        public Long sleepTime;
        public boolean async;

        public static TestHandlerOptionsBuilder builder( boolean async ) {
            return new TestHandlerOptionsBuilder().async( async );
        }
    }
}
