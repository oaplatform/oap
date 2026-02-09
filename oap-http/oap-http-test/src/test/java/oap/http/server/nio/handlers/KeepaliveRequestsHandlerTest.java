package oap.http.server.nio.handlers;

import oap.http.Http;
import oap.http.client.Client;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.testng.Ports;
import org.eclipse.jetty.client.HttpClient;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Map;

import static oap.http.test.HttpAsserts.assertGet;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

public class KeepaliveRequestsHandlerTest extends Fixtures {

    private final int testHttpPort;

    public KeepaliveRequestsHandlerTest() {
        testHttpPort = Ports.getFreePort( getClass() );
    }

    @Test
    public void testCloseConnectionBlocking() throws Exception {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( testHttpPort ) );
             HttpClient client = Client.customHttpClient() ) {
            client.setMaxConnectionsPerDestination( 10 );

            KeepaliveRequestsHandler keepaliveRequestsHandler = new KeepaliveRequestsHandler( 2 );
            httpServer.handlers.add( keepaliveRequestsHandler );

            httpServer.start();

            httpServer.bind( "/test", exchange -> {
                long id = exchange.exchange.getConnection().getId();
                ids.add( id );
                exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
            } );


            for( int i = 0; i < 101; i++ ) {
                assertGet( client, "http://localhost:" + testHttpPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
            }

            assertThat( ids ).hasSize( 51 );
            assertEventually( 500, 10, () -> {
                assertThat( keepaliveRequestsHandler.requests ).hasSize( 1 );
            } );
        }
    }

    @Test
    public void testCloseConnectionAsync() throws Exception {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( testHttpPort ) );
             HttpClient client = Client.customHttpClient() ) {
            client.setMaxConnectionsPerDestination( 10 );

            KeepaliveRequestsHandler keepaliveRequestsHandler = new KeepaliveRequestsHandler( 2 );
            httpServer.handlers.add( keepaliveRequestsHandler );

            httpServer.start();

            httpServer.bind( "/test", exchange -> {
                long id = exchange.exchange.getConnection().getId();
                ids.add( id );
                exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
            }, true );

            for( int i = 0; i < 101; i++ ) {
                assertGet( client, "http://localhost:" + testHttpPort + "/test", Map.of(), Map.of() ).body().isEqualTo( "ok" );
            }

            assertThat( ids ).hasSize( 51 );
            assertThat( keepaliveRequestsHandler.requests ).hasSize( 1 );
        }
    }
}
