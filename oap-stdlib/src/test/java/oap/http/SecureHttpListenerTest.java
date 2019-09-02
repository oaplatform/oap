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

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.SynchronizedThread;
import oap.http.cors.GenericCorsPolicy;
import oap.io.IoStreams;
import oap.testng.Env;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.testng.Asserts.pathOfTestResource;
import static org.testng.Assert.assertEquals;

@Slf4j
public class SecureHttpListenerTest {

    private static final String KEYSTORE_PASSWORD = "123456";

    private Server server;
    private SynchronizedThread listener;

    @BeforeClass
    public void setUp() {
        Env.resetPorts();
        server = new Server( 10, false );
        server.start();
        server.bind( "test", GenericCorsPolicy.DEFAULT, ( request, response ) -> {

            log.debug( "Base URL {}", request.getBaseUrl() );
            log.debug( "Headers:" );

            log.debug( "{}", request.getHeaders() );

            response.respond( HttpResponse.status( 200 ).response() );
        }, Protocol.HTTPS );

        SecureHttpListener http = new SecureHttpListener( server, pathOfTestResource( getClass(), "server_keystore.jks" ), KEYSTORE_PASSWORD, Env.port(), false );
        listener = new SynchronizedThread( http );
        listener.start();
    }

    @Test
    public void shouldVerifySSLCommunication() throws Exception {

        try( var inputStream = IoStreams.in( pathOfTestResource( getClass(), "client_truststore.jks" ), PLAIN ) ) {
            KeyStore keyStore = KeyStore.getInstance( "JKS" );
            keyStore.load( inputStream, KEYSTORE_PASSWORD.toCharArray() );

            TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( keyStore );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setSSLContext( sslContext ).build();

            HttpGet httpGet = new HttpGet( "https://localhost:" + Env.port() + "/test/" );

            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute( httpGet );

            Assert.assertEquals( closeableHttpResponse.getStatusLine().getStatusCode(), 200 );
        }
    }

    @AfterClass
    public void tearDown() {
        listener.stop();
        server.stop();
    }
}
