/**
 * Copyright
 */
package oap.ws;

import oap.metrics.Metrics;
import oap.testng.Env;
import oap.ws.apache.Server;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static oap.ws.testng.HttpAsserts.*;
import static oap.ws.testng.HttpAsserts.get;
import static org.apache.http.entity.ContentType.*;
import static org.testng.Assert.assertEquals;

public class ServiceTest {
    protected final Server server = new Server( Env.port(), 100 );

    @BeforeClass
    public void startServer() {
        server.bind( "x/v/math", new MathWS() );
        server.start();
    }

    @AfterClass
    public void stopServer() {
        server.stop();
        server.unbind( "x/v/math" );
        reset();
    }

    @Test
    public void invocations() throws IOException {
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/id?a=aaa" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"aaa\"" );
        get( HTTP_PREFIX + "/x/v/math/req" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"" + HTTP_PREFIX + "/x/v/math\"" );
        get( HTTP_PREFIX + "/x/v/math/sumab?a=1&b=2" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "3" );
        get( HTTP_PREFIX + "/x/v/math/sumabopt?a=1&b=2" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "3" );
        get( HTTP_PREFIX + "/x/v/math/x?i=1&s=2" )
            .assertResponse( 500, "failed", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), "failed" );
        get( HTTP_PREFIX + "/x/v/math/sumabopt?a=1" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "1" );
        get( HTTP_PREFIX + "/x/v/math/en?a=CLASS" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"CLASS\"" );
        get( HTTP_PREFIX + "/x/v/math/sum?a=1&b=2&b=3" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "6" );
        get( HTTP_PREFIX + "/x/v/math/bean?i=1&s=sss" )
            .assertResponse( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
        post( HTTP_PREFIX + "/x/v/math/bytes", "1234", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        post( HTTP_PREFIX + "/x/v/math/string", "1234", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "\"1234\"" );
        post( HTTP_PREFIX + "/x/v/math/json", "{\"i\":1,\"s\":\"sss\"}", APPLICATION_OCTET_STREAM )
            .assertResponse( 200, "OK", APPLICATION_JSON, "{\"i\":1,\"s\":\"sss\"}" );
        get( HTTP_PREFIX + "/x/v/math/code?code=204" )
            .assertResponse( 204, "No Content" );
        assertEquals(
            Metrics.snapshot( Metrics.name( "rest_timer" )
                .tag( "service", MathWS.class.getSimpleName() )
                .tag( "method", "bean" ) ).count,
            1 );
    }
}

