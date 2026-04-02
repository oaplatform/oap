package oap.http.client;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.BaseUnits;
import lombok.SneakyThrows;
import oap.util.Dates;
import oap.util.Lists;
import org.eclipse.jetty.client.AbstractConnectionPool;
import org.eclipse.jetty.client.AbstractConnectorHttpClientTransport;
import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.RandomConnectionPool;
import org.eclipse.jetty.client.RoundRobinConnectionPool;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import static oap.http.client.OapHttpClient.OapHttpClientBuilder.ConnectionPoolFactoryType.RANDOM;

public class OapHttpClient {
    public static final HttpClient DEFAULT_HTTP_CLIENT = customHttpClient()
        .withConnectionPoolFactoryType( RANDOM )
        .metrics( "default" )
        .build();

    public static OapHttpClientBuilder customHttpClient() {
        return new OapHttpClientBuilder();
    }

    public static void resetCookies() {
        resetCookies( DEFAULT_HTTP_CLIENT );
    }

    public static void resetCookies( HttpClient httpClient ) {
        httpClient.getHttpCookieStore().clear();
    }

    public static class OapHttpClientBuilder {
        public AbstractConnectorHttpClientTransport httpClientTransport;
        public long connectionTimeout = Dates.s( 10 );
        public boolean followRedirects = false;
        public int maxConnectionsPerDestination = 64;
        public boolean dnsjava = false;
        private String metrics;
        private HttpCookieStore cookieStore;
        private ConnectionPool.Factory connectionPoolFactory;
        private ConnectionPoolFactoryType connectionPoolFactoryType;

        public OapHttpClientBuilder transport( AbstractConnectorHttpClientTransport httpClientTransport ) {
            this.httpClientTransport = httpClientTransport;

            return this;
        }

        public OapHttpClientBuilder connectionTimeout( long connectionTimeout ) {
            this.connectionTimeout = connectionTimeout;

            return this;
        }

        public OapHttpClientBuilder followRedirects( boolean followRedirects ) {
            this.followRedirects = followRedirects;

            return this;
        }

        public OapHttpClientBuilder maxConnectionsPerDestination( int maxConnectionsPerDestination ) {
            this.maxConnectionsPerDestination = maxConnectionsPerDestination;

            return this;
        }

        public OapHttpClientBuilder dnsjava( boolean enabled ) {
            this.dnsjava = enabled;

            return this;
        }

        public OapHttpClientBuilder metrics( String name ) {
            this.metrics = name;

            return this;
        }

        public OapHttpClientBuilder cookieStore( HttpCookieStore cookieStore ) {
            this.cookieStore = cookieStore;

            return this;
        }

        public OapHttpClientBuilder withConnectionPoolFactory( ConnectionPool.Factory connectionPoolFactory ) {
            this.connectionPoolFactory = connectionPoolFactory;

            return this;
        }

        public OapHttpClientBuilder withConnectionPoolFactoryType( ConnectionPoolFactoryType connectionPoolFactoryType ) {
            this.connectionPoolFactoryType = connectionPoolFactoryType;

            return this;
        }

        @SneakyThrows
        public HttpClient build() {
            HttpClient httpClient = httpClientTransport != null ? new HttpClient( httpClientTransport ) : new HttpClient();
            httpClient.setConnectTimeout( connectionTimeout );
            QueuedThreadPool qtp = new QueuedThreadPool();
            qtp.setVirtualThreadsExecutor( new VirtualThreadPool() );
            httpClient.setExecutor( qtp );
            httpClient.setFollowRedirects( followRedirects );
            httpClient.setMaxConnectionsPerDestination( maxConnectionsPerDestination );

            if( cookieStore != null ) {
                httpClient.setHttpCookieStore( cookieStore );
            }

            if( dnsjava ) {
                httpClient.setSocketAddressResolver( ( host, port, context, promise ) -> {
                    try {
                        if( "localhost".equals( host ) ) {
                            promise.succeeded( List.of( new InetSocketAddress( InetAddress.getLocalHost(), port ) ) );
                            return;
                        }

                        InetAddress[] inetAddresses = Address.getAllByName( host );

                        promise.succeeded( Lists.map( inetAddresses, ia -> new InetSocketAddress( ia, port ) ) );
                    } catch( UnknownHostException e ) {
                        promise.failed( e );
                    }
                } );
            }

            if( metrics != null ) {
                ( ( AbstractConnectorHttpClientTransport ) httpClient.getHttpClientTransport() ).getClientConnector().addEventListener( new ClientConnectorConnectListener( metrics ) );

                Gauge.builder( "http_client_connections", httpClient, cs -> cs.getDestinations().stream().mapToInt( d -> ( ( AbstractConnectionPool ) d.getConnectionPool() ).getMaxConnectionCount() ).sum() )
                    .baseUnit( BaseUnits.CONNECTIONS )
                    .tags( "event", "max", "client", metrics )
                    .register( Metrics.globalRegistry );
                Gauge.builder( "http_client_connections", httpClient, cs -> cs.getDestinations().stream().mapToInt( d -> ( ( AbstractConnectionPool ) d.getConnectionPool() ).getConnectionCount() ).sum() )
                    .baseUnit( BaseUnits.CONNECTIONS )
                    .tags( "event", "total", "client", metrics )
                    .register( Metrics.globalRegistry );
                Gauge.builder( "http_client_connections", httpClient, cs -> cs.getDestinations().stream().mapToInt( d -> ( ( AbstractConnectionPool ) d.getConnectionPool() ).getActiveConnectionCount() ).sum() )
                    .baseUnit( BaseUnits.CONNECTIONS )
                    .tags( "event", "active", "client", metrics )
                    .register( Metrics.globalRegistry );
            }

            httpClient.start();

            httpClient.getProtocolHandlers().remove( WWWAuthenticationProtocolHandler.NAME );

            if( connectionPoolFactory != null ) {
                httpClient.getHttpClientTransport().setConnectionPoolFactory( connectionPoolFactory );
            }

            if( connectionPoolFactoryType != null ) {
                httpClient.getHttpClientTransport().setConnectionPoolFactory( destination -> switch( this.connectionPoolFactoryType ) {
                    case RANDOM -> new RandomConnectionPool( destination, httpClient.getMaxConnectionsPerDestination(), 1 );
                    case ROUND_ROBIN -> new RoundRobinConnectionPool( destination, httpClient.getMaxConnectionsPerDestination(), 1 );
                } );
            }

            return httpClient;
        }

        public enum ConnectionPoolFactoryType {
            RANDOM,
            ROUND_ROBIN
        }

        public static class ClientConnectorConnectListener implements ClientConnector.ConnectListener {
            public final LongAdder connectSuccessCounter = new LongAdder();
            public final LongAdder connectFailedCounter = new LongAdder();

            public ClientConnectorConnectListener( String name ) {
                Gauge.builder( "http_client_connections", this, cs -> cs.connectSuccessCounter.doubleValue() )
                    .baseUnit( BaseUnits.CONNECTIONS )
                    .tags( "event", "success", "client", name )
                    .register( Metrics.globalRegistry );
                Gauge.builder( "http_client_connections", this, cs -> cs.connectFailedCounter.doubleValue() )
                    .baseUnit( BaseUnits.CONNECTIONS )
                    .tags( "event", "failed", "client", name )
                    .register( Metrics.globalRegistry );
            }

            @Override
            public void onConnectSuccess( SocketChannel socketChannel ) {
                connectSuccessCounter.increment();
            }

            @Override
            public void onConnectFailure( SocketChannel socketChannel, SocketAddress socketAddress, Throwable failure ) {
                connectFailedCounter.increment();
            }
        }
    }
}
