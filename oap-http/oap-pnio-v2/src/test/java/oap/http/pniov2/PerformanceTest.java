package oap.http.pniov2;

import oap.benchmark.Benchmark;
import oap.concurrent.LongAdder;
import oap.concurrent.scheduler.Scheduler;
import oap.http.server.nio.NioHttpServer;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static oap.http.test.HttpAsserts.assertPost;

@Test( enabled = false )
public class PerformanceTest {
    @Test( enabled = false )
    public void test() throws IOException {

        ConcurrentHashMap<Integer, LongAdder> count = new ConcurrentHashMap<>();

//        int port = Ports.getFreePort( getClass() );
        int port = 12345;


        AbstractPnioHttpHandler.PnioHttpSettings settings = AbstractPnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( 64000 )
            .responseSize( 64000 )
            .build();
        try( PnioController pnioController = new PnioController( 10 );
             NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = 4;
            httpServer.statistics = true;
            httpServer.start();

            DefaultPnioHttpHandler httpHandler = new DefaultPnioHttpHandler( settings, new TestHandler(), new PnioHttpHandlerTest.TestPnioListener(), pnioController );

            Scheduler.scheduleWithFixedDelay( 10, TimeUnit.SECONDS, () -> {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println( Binder.json.marshal( new PnioWS( Map.of( httpHandler.getClass().getSimpleName(), httpHandler ) ).queue() ) );
            } );

            httpServer.bind( "/test",
                exchange -> httpHandler.handleRequest( exchange, 100 ), false );


//            Threads.sleepSafely( 100000000 );

            Benchmark.benchmark( "test", 100000, i -> {

                assertPost( "http://localhost:" + port + "/test", "{}" )
                    .is( r -> {
                        count.computeIfAbsent( r.code, k -> new LongAdder() ).increment();
                    } );
            } ).threads( 24 ).experiments( 5 ).run();

            System.out.println( count );
        }

    }

    public static class TestHandler implements ComputeTask<TestPnioExchange> {
        @Override
        public void accept( TestPnioExchange pnioExchange ) {
            double sum = 0.0;
            for( int i = 0; i < 200; i++ ) {
                for( int j = 0; j < 200; j++ ) {
                    sum += i + Math.pow( j * 1.0, 0.5 );
                }
            }

            double sum2 = 0.0;
            for( int i = 0; i < 200; i++ ) {
                for( int j = 0; j < 200; j++ ) {
                    sum2 += i + Math.pow( j * 1.0, 0.5 );
                }
            }

            pnioExchange.complete();
            pnioExchange.response();
        }
    }
}
