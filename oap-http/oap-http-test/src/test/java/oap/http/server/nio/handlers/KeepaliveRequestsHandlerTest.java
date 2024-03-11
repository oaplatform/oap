package oap.http.server.nio.handlers;

import oap.http.Client;
import oap.http.Http;
import oap.http.server.nio.NioHttpServer;
import oap.testng.Fixtures;
import oap.testng.Ports;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class KeepaliveRequestsHandlerTest extends Fixtures {

    private final int testHttpPort;

    public KeepaliveRequestsHandlerTest() {
        testHttpPort = Ports.getFreePort( getClass() );
    }

    @Test
    public void testCloseConnection() throws IOException {
        var ids = new LinkedHashSet<Long>();
        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( testHttpPort ) ) ) {

            KeepaliveRequestsHandler keepaliveRequestsHandler = new KeepaliveRequestsHandler( 2 );
            httpServer.handlers.add( keepaliveRequestsHandler );

            httpServer.start();

            httpServer.bind( "/test", exchange -> {
                long id = exchange.exchange.getConnection().getId();
                ids.add( id );
                exchange.responseOk( "ok", Http.ContentType.TEXT_PLAIN );
            } );

            var client = Client.custom().setMaxConnTotal( 10 ).setMaxConnPerRoute( 10 ).build();

            for( int i = 0; i < 101; i++ ) {
                assertThat( client.get( "http://localhost:" + testHttpPort + "/test" ).contentString() ).isEqualTo( "ok" );
            }

            assertThat( ids ).hasSize( 51 );
            assertThat( keepaliveRequestsHandler.requests ).hasSize( 1 );
        }
    }
}
