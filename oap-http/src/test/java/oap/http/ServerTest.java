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
import java.security.*;
import java.security.cert.CertificateException;

public class ServerTest {

    private final Server server = new Server( 10 );

    private SecureHttpRequestListener secureHttpRequestListener;

    @BeforeTest
    public void setUp() {
        server.bind( "test", new Cors(), ( request, response ) -> {

            System.out.println( "Base URL " + request.baseUrl );

            System.out.println( "------------------------" );
            System.out.println( "Headers:" );

            for( final Header header : request.headers ) {
                System.out.println( header.getName() + " " + header.getValue() );
            }
            System.out.println( "------------------------" );

            response.respond( new HttpResponse( 200 ) );
        }, Protocol.HTTPS );

        secureHttpRequestListener = new SecureHttpRequestListener( server, "server_keystore.jks", "123456", Env.port() );
        secureHttpRequestListener.start();
    }

    @Test
    public void test() throws KeyStoreException, IOException, CertificateException,
        NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
        final HttpGet httpGet = new HttpGet( "https://localhost:" + Env.port() + "/test/" );

        System.out.println( httpGet.getRequestLine().getUri() );

        final KeyStore keyStore = KeyStore.getInstance( "JKS" );
        keyStore.load( Resources.getResource( "client_truststore.jks" ).openStream(),
            "123456".toCharArray() );

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance( TrustManagerFactory
                .getDefaultAlgorithm() );
        trustManagerFactory.init( keyStore );

        final SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

        final CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setSSLContext( sslContext )
            .setSSLHostnameVerifier( NoopHostnameVerifier.INSTANCE ).build();

        final CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute( httpGet );

        System.out.println( "Status code is " + closeableHttpResponse.getStatusLine().getStatusCode() );
    }

    @AfterTest
    public void tearDown() {
        secureHttpRequestListener.stop();
        server.stop();
    }
}
