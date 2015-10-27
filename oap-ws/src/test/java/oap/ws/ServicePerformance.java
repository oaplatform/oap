/**
 * Copyright
 */
package oap.ws;

import oap.testng.AbstractPerformance;
import oap.testng.Env;
import oap.ws.apache.NioServer;
import oap.ws.apache.Server;
import oap.ws.testng.HttpAsserts;
import org.apache.http.entity.ContentType;
import org.testng.annotations.Test;

import static oap.ws.testng.HttpAsserts.HTTP_PREFIX;

public class ServicePerformance extends AbstractPerformance {
    private final int samples = 100000;
    private final int experiments = 5;


    @Test
    public void blocking_threads() {

        try( Server server = new Server( Env.port(), 100 ) ) {
            server.bind( "x/v/math", new MathWS() );
            server.start();

            HttpAsserts.reset();
            benchmark( "Server.invocations", samples, experiments,
                number -> HttpAsserts.get( HTTP_PREFIX + "/x/v/math/id?a=aaa" ).assertResponse( 200, "OK",
                    ContentType.APPLICATION_JSON, "\"aaa\"" ) );

            HttpAsserts.reset();
        }
    }

    @Test
    public void nio_threads() throws Exception {

        try( NioServer server = new NioServer( Env.port() ) ) {
            server.bind( "x/v/math", new MathWS() );
            server.start();
            Thread.sleep( 3000 ); // ??? TODO: fix me

            HttpAsserts.reset();
            benchmark( "NioServer.invocations", samples, experiments, ( number ) -> {
                try {
                    HttpAsserts.get( HTTP_PREFIX + "/x/v/math/id?a=aaa" ).assertResponse( 200, "OK",
                        ContentType.APPLICATION_JSON, "\"aaa\"" );
                } catch( Throwable e ) {
                    e.printStackTrace();
                }
            } );

            HttpAsserts.reset();
        }
    }
}

