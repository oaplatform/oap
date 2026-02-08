package oap.http.pniov3;

import oap.concurrent.Executors;
import oap.concurrent.ThreadPoolExecutor;
import oap.http.Response;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PnioServerTest extends Fixtures {
    private final PortFixture fixture;

    public PnioServerTest() {
        fixture = fixture( new PortFixture() );
    }

    @Test
    public void testRequestUndertow() throws InterruptedException, IOException {
        int port = fixture.definePort( "test" );

        ThreadPoolExecutor threadPoolExecutor = Executors.newFixedBlockingThreadPool( 1024 );

        Client client = Client.custom().setMaxConnPerRoute( 20000 ).setMaxConnTotal( 20000 ).build();

        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger okCount = new AtomicInteger();

        try( NioHttpServer pnioServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            pnioServer.ioThreads = Runtime.getRuntime().availableProcessors();
            pnioServer.bind( "/pnio", exchange -> {
                exchange.setStatusCode( 204 );
                exchange.endExchange();
            } );
            pnioServer.start();

            long start = System.currentTimeMillis();

            for( int i = 0; i < 20; i++ ) {
                threadPoolExecutor.submit( () -> {
                    try {
                        Response response = client.get( "http://localhost:" + port + "/pnio?trace=true" );
                        okCount.incrementAndGet();
                    } catch( Exception e ) {
                        errorCount.incrementAndGet();
                    }
                } );
            }

            System.out.println( "ok " + okCount.get() + " error " + errorCount.get() + " duration " + Dates.durationToString( System.currentTimeMillis() - start ) );

            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination( 10, TimeUnit.SECONDS );
        }
    }
}
