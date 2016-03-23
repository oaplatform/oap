package oap.http;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;

final class ServerSocketUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger( ServerSocketUtils.class );

    private ServerSocketUtils() {
    }

    public static ServerSocket createLocalSocket() {
        return handleSocketExceptions( () -> {
                final ServerSocket serverSocket = new ServerSocket( 9090, 0, InetAddress.getByName( "127.0.0.1" ) );
                serverSocket.setReuseAddress( true );

                return serverSocket;
            }, 9090
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

    public static ServerSocket createSecureSocket( final String certificateLocation, final String keystorePassword,
                                                   final int port ) {
        return handleSocketExceptions( () -> {
            final KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            keyStore.load( Resources.getResource( certificateLocation ).openStream(),
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
            LOGGER.error( "Cannot bind to port [{}]", port, e );
            throw new RuntimeException( e.getMessage(), e );
        } catch( final Exception e ) {
            LOGGER.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
