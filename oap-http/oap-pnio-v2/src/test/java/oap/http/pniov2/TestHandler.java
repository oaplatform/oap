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

        return pnioExchange -> {
            TestHandler.handle( name, "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            pnioExchange.complete();
            pnioExchange.response();
        };
    }

    public static AsyncTask<String, TestState> async( String name ) {
        return async( name, _ -> {} );
    }

    public static AsyncTask<String, TestState> async( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return pnioExchange -> {
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            TestHandler.handle( name, "ASYNC", pnioExchange, testHandlerOptionsBuilder
                .async( true )
                .exceptionCallback( completableFuture::completeExceptionally )
                .successCallback( () -> completableFuture.complete( name ) )
                .build() );

            return completableFuture;
        };
    }

    public static void handle( String name, String type, PnioExchange<TestState> pnioExchange,
                               TestHandlerOptions testHandlerOptions ) throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();

        String data = "name '" + name + "' type " + type + " thread '" + currentThreadName/*.substring( 7, 11 )*/
            + "' new thread " + !pnioExchange.requestState.oldThreadName.equals( currentThreadName );

        log.debug( "currentThreadName {} data {}", currentThreadName, data );

        if( !pnioExchange.requestState.sb.isEmpty() ) {
            pnioExchange.requestState.sb.append( "\n" );
        }

        pnioExchange.requestState.sb.append( data );

        pnioExchange.requestState.oldThreadName = currentThreadName;

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
                    Threads.sleepSafely( 1 );
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
