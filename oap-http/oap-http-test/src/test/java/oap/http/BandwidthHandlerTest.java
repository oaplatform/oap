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

package oap.http;

import io.github.bucket4j.Bandwidth;
import oap.http.server.nio.NioHttpServer;
import oap.http.server.nio.handlers.BandwidthHandler;
import oap.testng.Fixtures;
import oap.testng.Ports;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Http.ContentType.TEXT_PLAIN;
import static oap.http.Http.StatusCode.TOO_MANY_REQUESTS;
import static org.assertj.core.api.Assertions.assertThat;

public class BandwidthHandlerTest extends Fixtures {
    private NioHttpServer server;
    private BandwidthHandler bandwidthHandler;

    @BeforeMethod
    public void beforeMethod() {
        server = new NioHttpServer( new NioHttpServer.DefaultPort( Ports.getFreePort( getClass() ) ) );
        bandwidthHandler = new BandwidthHandler();
        bandwidthHandler.start();
        server.handlers.add( bandwidthHandler );
    }

    @AfterMethod
    public void afterMethod() {
        server.preStop();
    }

    @Test
    public void singleBandwidth() {
        bandwidthHandler.bandwidths.clear();
        bandwidthHandler.bandwidths.add( Bandwidth.simple( 2, Duration.ofMinutes( 1 ) ) );
        bandwidthHandler.start();

        server.bind( "test", exchange ->
            exchange.responseOk( "test", true, TEXT_PLAIN )
        );
        server.start();

        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
    }

    @Test
    public void coupleBandwidths() throws Exception {
        bandwidthHandler.bandwidths.clear();
        bandwidthHandler.bandwidths.add( Bandwidth.simple( 2, Duration.ofMinutes( 1 ) ) );
        bandwidthHandler.bandwidths.add( Bandwidth.simple( 1, Duration.ofSeconds( 2 ) ) );
        bandwidthHandler.start();

        server.bind( "test", exchange ->
            exchange.responseOk( "test", true, TEXT_PLAIN )
        );
        server.start();

        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( HTTP_OK );
        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
        Thread.currentThread().sleep( 2_100L );
        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( HTTP_OK );
        Thread.currentThread().sleep( 2_100L );
        assertThat( Client.DEFAULT.get( "http://localhost:" + server.defaultPort.httpPort + "/test" ).code ).isEqualTo( TOO_MANY_REQUESTS );
    }
}
