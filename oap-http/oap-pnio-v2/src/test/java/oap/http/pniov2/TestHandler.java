package oap.http.pniov2;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class TestHandler {
    public static ComputeTask<TestState> compute( String name ) {
        return compute( name, _ -> {} );
    }

    public static ComputeTask<TestState> compute( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return ( pnioExchange, testState ) -> {
            TestHandler.handle( name, "COMPUTE", pnioExchange, testState, testHandlerOptionsBuilder.build() );

            pnioExchange.complete();
            pnioExchange.response();
        };
    }

    public static AsyncTask<TestState> async( String name ) {
        return async( name, _ -> {} );
    }

    public static AsyncTask<TestState> async( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return ( pnioExchange, testState ) -> {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            TestHandler.handle( name, "ASYNC", pnioExchange, testState, testHandlerOptionsBuilder
                .async( true )
                .exceptionCallback( completableFuture::completeExceptionally )
                .successCallback( () -> completableFuture.complete( null ) )
                .build() );

            return completableFuture;
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
