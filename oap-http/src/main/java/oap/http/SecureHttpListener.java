package oap.http;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.*;
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
   }

   @Override
   protected ServerSocket createSocket() {
      try {
         KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
         keyStore.load( IoStreams.in( keystoreLocation, PLAIN ), keystorePassword.toCharArray() );

         KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm() );
         keyManagerFactory.init( keyStore, keystorePassword.toCharArray() );

         SSLContext sslContext = SSLContext.getInstance( "TLS" );
         sslContext.init( keyManagerFactory.getKeyManagers(), null, null );

         ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket();
         serverSocket.setReuseAddress( true );
         serverSocket.setSoTimeout( timeout );
         serverSocket.bind( new InetSocketAddress( port ) );

         return serverSocket;
      } catch( KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e ) {
         throw Throwables.propagate( e );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }
}
