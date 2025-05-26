package oap.http.pniov2;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class TestHandler {
    public static ComputeTask<TestPnioExchange> compute( String name ) {
        return compute( name, _ -> {} );
    }

    public static ComputeTask<TestPnioExchange> compute( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return pnioExchange -> {
            TestHandler.handle( name, "COMPUTE", pnioExchange, testHandlerOptionsBuilder.build() );

            pnioExchange.complete();
            pnioExchange.response();
        };
    }

    public static AsyncTask<Void, TestPnioExchange> async( String name ) {
        return async( name, _ -> {} );
    }

    public static AsyncTask<Void, TestPnioExchange> async( String name, Consumer<TestHandlerOptions.TestHandlerOptionsBuilder> builder ) {
        TestHandlerOptions.TestHandlerOptionsBuilder testHandlerOptionsBuilder = TestHandlerOptions.builder( false );
        builder.accept( testHandlerOptionsBuilder );

        return pnioExchange -> {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            TestHandler.handle( name, "ASYNC", pnioExchange, testHandlerOptionsBuilder
                .async( true )
                .exceptionCallback( completableFuture::completeExceptionally )
                .successCallback( () -> completableFuture.complete( null ) )
                .build() );

            return completableFuture;
        };
    }

    public static void handle( String name, String type, TestPnioExchange pnioExchange,
                               TestHandlerOptions testHandlerOptions ) throws InterruptedException {
        String currentThreadName = Thread.currentThread().getName();

        String data = "name '" + name + "' type " + type + " thread '" + currentThreadName.substring( 7, 11 )
            + "' new thread " + !pnioExchange.oldThreadName.equals( currentThreadName );

        log.debug( data );

        if( !pnioExchange.sb.isEmpty() ) {
            pnioExchange.sb.append( "\n" );
        }

        pnioExchange.sb.append( data );

        pnioExchange.oldThreadName = currentThreadName;

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
