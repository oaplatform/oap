package oap.http;

import com.google.common.io.Resources;
import oap.testng.Env;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

import static org.testng.Assert.assertEquals;

public class ServerTest {

    private static final String KEYSTORE_PASSWORD = "123456";

    private final Server server = new Server( 10 );

    @BeforeTest
    public void setUp() {
        server.bind( "test", Cors.DEFAULT, ( request, response ) -> {

            System.out.println( "Base URL " + request.baseUrl );
            System.out.println( "Headers:" );

            for( final Header header : request.headers ) {
                System.out.println( header.getName() + " " + header.getValue() );
            }

            response.respond( new HttpResponse( 200 ) );
        }, Protocol.HTTPS );

        new Thread( new SecureHttpListener( server, Paths.get( "server_keystore.jks" ),
            KEYSTORE_PASSWORD, Env.port() ) ).start();
    }

    @Test
    public void testShouldVerifySSLCommunication() throws KeyStoreException, IOException, CertificateException,
        NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        final KeyStore keyStore = KeyStore.getInstance( "JKS" );
        keyStore.load( Resources.getResource( "client_truststore.jks" ).openStream(),
            KEYSTORE_PASSWORD.toCharArray() );

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance( TrustManagerFactory
                .getDefaultAlgorithm() );
        trustManagerFactory.init( keyStore );

        final SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

        final CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setSSLContext( sslContext )
            .setSSLHostnameVerifier( NoopHostnameVerifier.INSTANCE ).build();

        final HttpGet httpGet = new HttpGet( "https://localhost:" + Env.port() + "/test/" );

        final CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute( httpGet );

        assertEquals( closeableHttpResponse.getStatusLine().getStatusCode(), 200 );
    }

    @AfterTest
    public void tearDown() {
        server.stop();
    }
}
