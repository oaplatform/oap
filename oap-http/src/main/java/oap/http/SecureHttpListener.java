package oap.http;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.IoStreams;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;

import static oap.io.IoStreams.Encoding.PLAIN;

@Slf4j
public class SecureHttpListener extends AbstractHttpListener {

    private final Path keystoreLocation;
    private final String keystorePassword;
    private final int port;

    public SecureHttpListener( HttpServer server, Path keystoreLocation, String keystorePassword, int port ) {
        super( server );
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.port = port;
        log.info( "Secure HTTP listener configured to use {} and bind to port {}",
            keystoreLocation,
            port );
    }

    @SneakyThrows
    @Override
    protected ServerSocket createSocket() {
        if( Files.exists( keystoreLocation ) ) {
            try( val inputStream = IoStreams.in( keystoreLocation, PLAIN ) ) {
                log.info( "Keystore {} exists, trying to initialize", keystoreLocation );
                KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
                keyStore.load( inputStream, keystorePassword.toCharArray() );

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm() );
                keyManagerFactory.init( keyStore, keystorePassword.toCharArray() );

                SSLContext sslContext = SSLContext.getInstance( "TLS" );
                sslContext.init( keyManagerFactory.getKeyManagers(), null, null );

                ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket();
                serverSocket.setReuseAddress( true );
                serverSocket.setSoTimeout( timeout );
                serverSocket.bind( new InetSocketAddress( port ) );

                log.info( "Successfully initialized secure http listener" );
                return serverSocket;
            } catch( BindException e ) {
                log.error( "Cannot bind to port [{}]", port );
                throw e;
            }
        } else {
            throw new CertificateException( keystoreLocation + " not found" );
        }
    }
}