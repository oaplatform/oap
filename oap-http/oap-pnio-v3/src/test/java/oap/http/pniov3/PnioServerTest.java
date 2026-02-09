package oap.http.pniov3;

import oap.http.Http;
import oap.http.client.Client;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.util.Dates;
import org.eclipse.jetty.client.HttpClient;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static oap.http.test.HttpAsserts.assertGet;

public class PnioServerTest extends Fixtures {
    private final PortFixture fixture;

    public PnioServerTest() {
        fixture = fixture( new PortFixture() );
    }

    @Test
    public void testRequestUndertow() throws Exception {
        int port = fixture.definePort( "test" );

        try( ExecutorService threadPoolExecutor = Executors.newVirtualThreadPerTaskExecutor() ) {
            try( HttpClient httpClient = Client.customHttpClient() ) {
                httpClient.setMaxConnectionsPerDestination( 2000 );

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
                        threadPoolExecutor.execute( () -> {
                            try {
                                assertGet( httpClient, "http://localhost:" + port + "/pnio?trace=true", Map.of(), Map.of() )
                                    .hasCode( Http.StatusCode.NO_CONTENT );
                                okCount.incrementAndGet();
                            } catch( Exception e ) {
                                errorCount.incrementAndGet();
                            }
                        } );
                    }

                    System.out.println( "ok " + okCount.get() + " error " + errorCount.get() + " duration " + Dates.durationToString( System.currentTimeMillis() - start ) );
                }

                for( int i = 0; i < 20; i++ ) {
//                    threadPoolExecutor.execute( () -> {
                    try {
                        assertGet( httpClient, "http://localhost:" + port + "/pnio?trace=true", Map.of(), Map.of() )
                            .hasCode( Http.StatusCode.NO_CONTENT );
                        okCount.incrementAndGet();
                    } catch( Exception e ) {
                        errorCount.incrementAndGet();
                    }
//                    } );
                }
            }
            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination( 20, TimeUnit.SECONDS );
        }
    }
}
