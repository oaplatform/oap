package oap.http.pniov3;

import oap.benchmark.Benchmark;
import oap.concurrent.LongAdder;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.Scheduler;
import oap.http.server.nio.NioHttpServer;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static oap.http.test.HttpAsserts.assertPost;

@Test( enabled = false )
public class PerformanceTest {
    public static AtomicInteger count = new AtomicInteger();

    @Test( enabled = false )
    public void test() throws IOException {

        ConcurrentHashMap<Integer, LongAdder> count = new ConcurrentHashMap<>();

//        int port = Ports.getFreePort( getClass() );
        int port = 12345;


        PnioHttpHandler.PnioHttpSettings settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( 64000 )
            .responseSize( 64000 )
            .build();
        try( PnioController pnioController = new PnioController( 10, 10000 );
             NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = 2;
            httpServer.statistics = true;
            httpServer.start();

            PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( "perf", settings, new TestHandler(), new PnioHttpHandlerTest.TestPnioListener(), pnioController );

            Scheduler.scheduleWithFixedDelay( 10, TimeUnit.SECONDS, () -> {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println( Binder.json.marshal( new PnioWS( Map.of( httpHandler.getClass().getSimpleName(), httpHandler ) ).queue() ) );
            } );

            httpServer.bind( "/test",
                exchange -> httpHandler.handleRequest( exchange, 1000, new TestState() ), false );


//            Threads.sleepSafely( 100000000 );

            Benchmark.benchmark( "test", 100000, i -> {

                assertPost( "http://localhost:" + port + "/test", "{}" )
                    .is( r -> {
                        count.computeIfAbsent( r.code, k -> new LongAdder() ).increment();
                    } );
            } ).threads( 10000 ).experiments( 5 ).warming( 10 ).run();

            System.out.println( count );
        }

    }

    public static class TestHandler implements ComputeTask<TestState> {
        public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

        @Override
        public void run( PnioExchange<TestState> pnioExchange ) {
            double sum = 0.0;
            for( int x = 0; x < 1; x++ ) {
                for( int i = 0; i < 200; i++ ) {
                    for( int j = 0; j < 200; j++ ) {
                        sum += i + Math.pow( j * 1.0, 0.5 );
                    }
                }
            }

            pnioExchange.runAsyncTask( "perf", _ -> CompletableFuture.runAsync( () -> Threads.sleepSafely( 100 ), EXECUTOR_SERVICE ) );

            pnioExchange.complete();
            pnioExchange.response();
        }
    }
}
