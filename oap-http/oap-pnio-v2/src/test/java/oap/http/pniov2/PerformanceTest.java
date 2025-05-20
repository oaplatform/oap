package oap.http.pniov2;

import oap.concurrent.LongAdder;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.Scheduler;
import oap.http.server.nio.NioHttpServer;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PerformanceTest {
    @Test( enabled = false )
    public void test() throws IOException {

        RequestWorkflow<TestState> workflow = RequestWorkflow
            .init( new TestHandler() )
            .next( new TestHandler() )
            .build();

        ConcurrentHashMap<Integer, LongAdder> count = new ConcurrentHashMap<>();

//        int port = Ports.getFreePort( getClass() );
        int port = 12345;


        PnioHttpHandler.PnioHttpSettings settings = PnioHttpHandler.PnioHttpSettings.builder()
            .requestSize( 64000 )
            .responseSize( 64000 )
            .build();
        try( PnioController pnioController = new PnioController( 10, 10, 256 );
             NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            httpServer.ioThreads = 4;
            httpServer.statistics = true;
            httpServer.start();

            PnioHttpHandler<TestState> httpHandler = new PnioHttpHandler<>( settings, workflow, new PnioHttpHandlerTest.TestPnioListener(), pnioController );

            Scheduler.scheduleWithFixedDelay( 10, TimeUnit.SECONDS, () -> {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println( Binder.json.marshal( new PnioWS<>( httpHandler ).queue() ) );
            } );

            httpServer.bind( "/test",
                exchange -> httpHandler.handleRequest( exchange, 100, new TestState() ), false );


            Threads.sleepSafely( 100000000 );

//                Benchmark.benchmark( "test", 100000, i -> {
//
//                    assertPost( "http://localhost:" + port + "/test", "{}" )
//                        .is( r -> {
//                            count.computeIfAbsent( r.code, k -> new LongAdder() ).increment();
//                        } );
//                } ).threads( 24 ).experiments( 5 ).run();

            System.out.println( count );
        }

    }

    public static class TestHandler extends PnioRequestHandler<TestState> {
        public TestHandler() {
            super( Type.COMPUTE );
        }

        @Override
        public void handle( PnioExchange<TestState> pnioExchange, TestState testState ) {
            double sum = 0.0;
            for( int i = 0; i < 200; i++ ) {
                for( int j = 0; j < 200; j++ ) {
                    sum += i + Math.pow( j * 1.0, 0.5 );
                }
            }
        }
    }
}
