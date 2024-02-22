/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.http.server.nio.health;

//import oap.application.module.Module;
//import oap.application.testng.KernelFixture;

import oap.http.server.nio.NioHttpServer;
import oap.http.test.HttpAsserts;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static oap.http.test.HttpAsserts.assertGet;

public class HealthHttpHandlerTest extends Fixtures {
    private final EnvFixture fixture = fixture( new EnvFixture() );

    public HealthHttpHandlerTest() {
//        fixture( new KernelFixture(
//            urlOfTestResource( getClass(), "application.test.conf" ),
//            Lists.concat(
//                List.of( urlOfTestResource( getClass(), "oap-module.conf" ) ),
//                Module.CONFIGURATION.urlsFromClassPath()
//            )
//        ) );
    }

    @Test
    public void health() throws IOException {
        int httpPort = fixture.portFor( "TEST_HTTP_PORT" );

        try( NioHttpServer httpServer = new NioHttpServer( new NioHttpServer.DefaultPort( httpPort ) ) ) {
            HealthHttpHandler healthHttpHandler = new HealthHttpHandler( httpServer, "/healtz", "default-http", "secret" );
            healthHttpHandler.start();

            healthHttpHandler.addProvider( new TestDataProvider() );

            httpServer.start();

            assertGet( HttpAsserts.httpUrl( httpPort, "/healtz" ) ).hasCode( HTTP_NO_CONTENT );
            assertGet( HttpAsserts.httpUrl( httpPort, "/healtz?secret=secret" ) )
                .respondedJson( "{\"test\":{\"k1\":1, \"k2\":2}}" );
        }
    }

    public static class TestDataProvider implements HealthDataProvider<Map<String, Integer>> {
        @Override
        public String name() {
            return "test";
        }

        @Override
        public Map<String, Integer> data() {
            return Map.of(
                "k1", 1,
                "k2", 2
            );
        }
    }
}
