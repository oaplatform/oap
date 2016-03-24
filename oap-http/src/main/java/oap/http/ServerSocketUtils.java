package oap.http;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.KeyStore;

@Slf4j
final class ServerSocketUtils {

    private ServerSocketUtils() {
    }

    public static ServerSocket createLocalSocket( final int port ) {
        return handleSocketExceptions( () -> {
                final ServerSocket serverSocket = new ServerSocket( port, 0, InetAddress.getByName( "127.0.0.1" ) );
                serverSocket.setReuseAddress( true );

                return serverSocket;
            }, port
        );
    }

    public static ServerSocket createPlainSocket( final int port ) {
        return handleSocketExceptions( () -> {
            final ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.bind( new InetSocketAddress( port ) );

            return serverSocket;
        }, port );
    }

    public static ServerSocket createSecureSocket( final Path keystoreLocation, final String keystorePassword,
                                                   final int port ) {
        return handleSocketExceptions( () -> {
            final KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            keyStore.load( Resources.getResource( keystoreLocation.toString() ).openStream(),
                keystorePassword.toCharArray() );

            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm() );
            keyManagerFactory.init( keyStore, keystorePassword.toCharArray() );

            final SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( keyManagerFactory.getKeyManagers(), null, null );

            final ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket();
            serverSocket.setReuseAddress( true );
            serverSocket.bind( new InetSocketAddress( port ) );

            return serverSocket;
        }, port );
    }

    private static ServerSocket handleSocketExceptions( final ThrowingSupplier<ServerSocket> resultSupplier,
                                                        final int port ) {
        try {
            return resultSupplier.get();
        } catch( final BindException e ) {
            log.error( "Cannot bind to port [{}]", port, e );
            throw new RuntimeException( e.getMessage(), e );
        } catch( final Exception e ) {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
