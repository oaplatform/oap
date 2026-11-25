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

package oap.http.server.nio;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.http.server.nio.handlers.CompressionNioHandler;
import oap.io.Closeables;
import oap.util.Dates;
import oap.util.Lists;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.JsseSslUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToLongFunction;

@Slf4j
public class NioHttpServer implements Closeable, AutoCloseable {
    public static final String DEFAULT_HTTP_PORT = "default-http";
    public static final String DEFAULT_HTTPS_PORT = "default-https";
    public static final String NIO_REQUESTS = "nio_requests";
    public static final String NIO_POOL_SIZE = "nio_pool_size";
    public static final String ACTIVE = "active";
    public static final String WORKER = "worker";
    /**
     * gauge, type = queue | active. Counts SSLEngine delegated tasks (a handshake produces one or more of them), not handshakes.
     */
    public static final String NIO_SSL_HANDSHAKE = "nio_ssl_handshake";
    /**
     * counter of completed SSLEngine delegated tasks (one or more per handshake)
     */
    public static final String NIO_SSL_HANDSHAKE_COMPLETED = "nio_ssl_handshake_completed";
    /**
     * counter of SSLEngine delegated tasks rejected by a full queue; each rejection closes the new connection
     */
    public static final String NIO_SSL_HANDSHAKE_REJECTED = "nio_ssl_handshake_rejected";
    /**
     * timer of SSLEngine delegated task execution (count/sum/max)
     */
    public static final String NIO_SSL_HANDSHAKE_TASK = "nio_ssl_handshake_task";
    /**
     * counter of accepted TCP connections, tag = port ( prometheus: nio_connections_opened_total ). Counted at accept:
     * before the TLS handshake and before the first request, so the connections without requests are included.
     * Connections accepted between the listener start and the counter installation ( server startup ) are not counted.
     */
    public static final String NIO_CONNECTIONS_OPENED = "nio_connections_opened";

    public final DefaultPort defaultPort;
    public final LinkedHashMap<String, Integer> defaultPorts = new LinkedHashMap<>();
    public final LinkedHashMap<String, Integer> additionalHttpPorts = new LinkedHashMap<>();
    public final ArrayList<NioHandlerBuilder> handlers = new ArrayList<>();
    private final ConcurrentHashMap<Integer, PathHandler> pathHandler = new ConcurrentHashMap<>();
    private final AtomicLong requestId = new AtomicLong();
    public int backlog = -1;
    public long idleTimeout = -1;
    public boolean tcpNodelay = true;
    /**
     * relative to the number of cores, see {@link Threads#count(double, int)}: 0 = max(2, cores), N, -N = cores - N, 1.5 = cores * 1.5, -1.5 = cores / 1.5
     */
    public double ioThreads = 0;
    /**
     * https only: true = run TLS handshake tasks (SSLEngine delegated tasks) in a separate bounded pool of platform threads
     * instead of xnio io threads; false = inline on io threads.
     * Runtime change: {@link #setSslHandshakeOffload(boolean)}
     */
    public volatile boolean sslHandshakeOffload = true;
    /**
     * TLS handshake pool size (sslHandshakeOffload = true), relative to the number of cores, see {@link Threads#count(double, int)}:
     * 0 = max(2, cores), N, -N = cores - N, 1.5 = cores * 1.5, -1.5 = cores / 1.5.
     * Runtime change: {@link #setSslHandshakeThreads(double)}, the pool is resized in place
     */
    public volatile double sslHandshakeThreads = 0.25;
    /**
     * lower bound of the TLS handshake pool size.
     * Runtime change: {@link #setSslHandshakeMinThreads(int)}, the pool is resized in place
     */
    public volatile int sslHandshakeMinThreads = 2;
    /**
     * pending TLS handshake tasks (sslHandshakeOffload = true); overflow closes the new connection.
     * Runtime change: {@link #setSslHandshakeQueue(int)}, the pool is replaced, the old one completes its queued tasks
     */
    public volatile int sslHandshakeQueue = 1024;
    public int workerThreads = -1; // default = Runtime.getRuntime().availableProcessors() * 8
    public long maxEntitySize = -1; // default = unlimited
    public int maxParameters = -1; // default = 1000
    public int maxHeaders = -1; // default = 200
    public int maxHeaderSize = -1; // default = 1024 * 1024
    public boolean statistics = false;
    public boolean alwaysSetDate = true;
    public boolean alwaysSetKeepAlive = true;
    public int pathHandlerCacheSize = 0; // without cache
    public long autoRefreshCertificates = Dates.h( 12 );
    public Undertow undertow;
    private KeyManager[] keyManagers;
    private XnioWorker xnioWorker;
    private Scheduled autoRefreshCertificatesScheduled;
    /**
     * the current TLS handshake pool, null = inline ( sslHandshakeOffload = false or the server is not running );
     * changed under {@link #sslHandshakeLock}
     */
    volatile ThreadPoolExecutor sslHandshakeExecutor;
    /**
     * lock order: sslHandshakeLock -> ThreadPoolExecutor internal lock ( the pool threads never take sslHandshakeLock )
     */
    private final Object sslHandshakeLock = new Object();
    private final ThreadFactory sslHandshakeThreadFactory = Thread.ofPlatform().name( "ssl-handshake-", 0 ).daemon( true ).factory();
    /**
     * replaced pools completing their queued tasks, under sslHandshakeLock
     */
    private final Set<ThreadPoolExecutor> sslHandshakeRetired = new HashSet<>();
    /**
     * completed tasks of the terminated pools, under sslHandshakeLock
     */
    private long sslHandshakeCompletedBefore;
    /**
     * under sslHandshakeLock: the server is running with https, Undertow calls {@link #executeSslHandshakeTask(Runnable)}
     */
    private boolean sslHandshakeInstalled;
    private boolean sslHandshakeMetricsRegistered;
    private volatile Counter sslHandshakeRejected;
    private volatile Timer sslHandshakeTaskTimer;

    public NioHttpServer( DefaultPort defaultPort ) {
        this.defaultPort = defaultPort;
        defaultPorts.put( DEFAULT_HTTP_PORT, defaultPort.httpPort );
        if( isHttpsEnabled() ) {
            defaultPorts.put( DEFAULT_HTTPS_PORT, defaultPort.httpsPort );
        }

        if( isHttpsEnabled() ) {
            keyManagers = makeKeyManagers( defaultPort.keyStore, defaultPort.password );
        } else {
            keyManagers = null;
        }
    }

    @SneakyThrows
    private static KeyStore loadKeyStore( URL keyStoreLocation, String storePassword ) {
        final KeyStore loadedKeystore;
        final String type = "JKS";
        try {
            loadedKeystore = KeyStore.getInstance( type );
        } catch( KeyStoreException ex ) {
            log.error( "loadKeyStore KeyStore.getInstance({}) exception: {}", type, ex.toString() );
            throw ex;
        }
        try( InputStream stream = keyStoreLocation.openStream() ) {
            loadedKeystore.load( stream, storePassword.toCharArray() );
            return loadedKeystore;
        } catch( NoSuchAlgorithmException | CertificateException | IOException ex ) {
            log.error( "loadKeyStore KeyStore.load({}, {}) as {} exception: {}", keyStoreLocation, storePassword.length(), type, ex.toString() );
            throw ex;
        }
    }

    @SneakyThrows
    private static KeyManager[] getKeyManagers( KeyStore keyStore, final String storePassword ) {
        final KeyManagerFactory keyManagerFactory;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
        } catch( NoSuchAlgorithmException ex ) {
            log.error( "getKeyManagers KeyManagerFactory.getInstance exception: {}", ex.toString() );
            throw ex;
        }
        // https://github.com/ops4j/org.ops4j.pax.web/blob/web-5.0.0.M1/pax-web-undertow/src/main/java/org/ops4j/pax/web/undertow/ssl/SslContextFactory.java
        try {
            keyManagerFactory.init( keyStore, storePassword.toCharArray() );
        } catch( NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException exc ) {
            log.error( "getKeyManagers exception initialising keyManagerFactory: {}", exc.toString() );
            throw exc;
        }
        return keyManagerFactory.getKeyManagers();
    }

    @SneakyThrows
    private static KeyManager[] makeKeyManagers( URL keyStoreLocation, final String password ) {
        KeyStore keyStore = loadKeyStore( keyStoreLocation, password );
        KeyManager[] managers = getKeyManagers( keyStore, password );
        log.info( "makeKeyManagers({}, {}) created KeyManagers {}", keyStoreLocation, password.length(), managers );
        return managers;
    }

    private boolean isHttpsEnabled() {
        return defaultPort.httpsPort > 0;
    }

    public void start() {
        long time = System.currentTimeMillis();

        pathHandler.computeIfAbsent( defaultPort.httpPort, p -> new PathHandler( pathHandlerCacheSize ) );
        if( isHttpsEnabled() ) {
            pathHandler.computeIfAbsent( defaultPort.httpsPort, p -> new PathHandler( pathHandlerCacheSize ) );
        }

        additionalHttpPorts.values().forEach( port -> pathHandler.computeIfAbsent( port, p -> new PathHandler( pathHandlerCacheSize ) ) );

        Undertow.Builder builder = Undertow.builder()
            .setSocketOption( Options.REUSE_ADDRESSES, true )
            .setSocketOption( Options.TCP_NODELAY, tcpNodelay )
            .setServerOption( UndertowOptions.RECORD_REQUEST_START_TIME, true );

        int io = Threads.count( ioThreads, Math.max( Runtime.getRuntime().availableProcessors(), 2 ) );

        XnioWorker.Builder workerBuilder = Xnio.getInstance( Undertow.class.getClassLoader() ).createWorkerBuilder();
        workerBuilder.setWorkerIoThreads( io );
        xnioWorker = workerBuilder
            .populateFromOptions( OptionMap.builder()
                .set( Options.TCP_NODELAY, true )
                .set( Options.CORK, true )
                .getMap() )
            .setExternalExecutorService( Executors.newVirtualThreadPerTaskExecutor() )
            .build();
        builder.setWorker( xnioWorker );

        if( isHttpsEnabled() ) {
            registerSslHandshakeMetrics();
            synchronized( sslHandshakeLock ) {
                if( sslHandshakeOffload ) sslHandshakeExecutor = newSslHandshakeExecutor();
                sslHandshakeInstalled = true;
            }
            // Undertow keeps the executor for the listener lifetime: always installed, the offload is switched inside
            builder.setSslEngineDelegatedTaskExecutor( this::executeSslHandshakeTask );
        } else if( sslHandshakeOffload ) {
            log.warn( "sslHandshakeOffload = true, but https is disabled" );
        }

        if( backlog > 0 ) builder.setSocketOption( Options.BACKLOG, backlog );
        builder.setIoThreads( io );
        if( workerThreads > 0 ) builder.setWorkerThreads( workerThreads );
        if( idleTimeout > 0 ) builder.setServerOption( UndertowOptions.IDLE_TIMEOUT, ( int ) idleTimeout );
        if( maxEntitySize > 0 ) builder.setServerOption( UndertowOptions.MAX_ENTITY_SIZE, maxEntitySize );
        if( maxParameters > 0 ) builder.setServerOption( UndertowOptions.MAX_PARAMETERS, maxParameters );
        if( maxHeaders > 0 ) builder.setServerOption( UndertowOptions.MAX_HEADERS, maxHeaders );
        if( maxHeaderSize > 0 ) builder.setServerOption( UndertowOptions.MAX_HEADER_SIZE, maxHeaderSize );
        if( statistics ) builder.setServerOption( UndertowOptions.ENABLE_STATISTICS, true );

        builder.setServerOption( UndertowOptions.ALWAYS_SET_DATE, alwaysSetDate );
        builder.setServerOption( UndertowOptions.ALWAYS_SET_KEEP_ALIVE, alwaysSetKeepAlive );

        pathHandler.forEach( ( port, ph ) -> addPortListener( port, ph, builder ) );

        undertow = builder.build();
        undertow.start();

        addAcceptCounters( undertow );

        if( statistics ) {
            addStats( undertow );
        }

        if( autoRefreshCertificates > 0 ) {
            autoRefreshCertificatesScheduled = Scheduler.scheduleWithFixedDelay( autoRefreshCertificates, TimeUnit.MILLISECONDS, this::refreshCertificates );
        }

        ThreadPoolExecutor sslExecutor = sslHandshakeExecutor;
        log.info( "server on ports: {} (statistics: {}, ioThreads: {}, workerThreads: {}, sslHandshakeThreads: {}) has started in {} ms",
            pathHandler.keySet(), statistics,
            undertow.getWorker().getMXBean().getIoThreadCount(),
            undertow.getWorker().getMXBean().getMaxWorkerPoolSize(),
            sslExecutor != null ? sslExecutor.getCorePoolSize() : "inline",
            System.currentTimeMillis() - time
        );
    }

    /**
     * the executor installed into Undertow: the pool when sslHandshakeOffload = true, else inline on the calling ( io ) thread.
     * xnio io threads stay free for established connections; RejectedExecutionException closes the new connection (SslConduit)
     */
    private void executeSslHandshakeTask( Runnable task ) {
        while( true ) {
            ThreadPoolExecutor executor = sslHandshakeExecutor;
            if( !sslHandshakeOffload || executor == null ) {
                task.run();
                return;
            }

            Timer taskTimer = sslHandshakeTaskTimer;
            try {
                executor.execute( () -> {
                    long start = System.nanoTime();
                    try {
                        task.run();
                    } finally {
                        taskTimer.record( System.nanoTime() - start, TimeUnit.NANOSECONDS );
                    }
                } );
                return;
            } catch( RejectedExecutionException e ) {
                if( !executor.isShutdown() ) throw e;
                // the pool is replaced or disabled at runtime: retry with the current one
            }
        }
    }

    private int sslHandshakePoolSize() {
        return Math.max( sslHandshakeMinThreads,
            Threads.count( sslHandshakeThreads, Math.max( Runtime.getRuntime().availableProcessors(), 2 ) ) );
    }

    private ThreadPoolExecutor newSslHandshakeExecutor() {
        Preconditions.checkArgument( sslHandshakeMinThreads >= 1, "sslHandshakeMinThreads must be >= 1, but %s", sslHandshakeMinThreads );
        Preconditions.checkArgument( sslHandshakeQueue >= 1, "sslHandshakeQueue must be >= 1, but %s", sslHandshakeQueue );

        int threads = sslHandshakePoolSize();
        Counter rejected = sslHandshakeRejected;

        log.debug( "ssl handshake pool: threads {} queue {}", threads, sslHandshakeQueue );

        return new ThreadPoolExecutor( threads, threads, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>( sslHandshakeQueue ), sslHandshakeThreadFactory,
            ( r, e ) -> {
                if( e.isShutdown() ) throw new RejectedExecutionException( "ssl handshake pool is shut down" );

                rejected.increment();
                throw new RejectedExecutionException( "ssl handshake queue is full" );
            } );
    }

    private void registerSslHandshakeMetrics() {
        Tags tags = Tags.of( "port", String.valueOf( defaultPort.httpsPort ) );
        sslHandshakeRejected = Metrics.counter( NIO_SSL_HANDSHAKE_REJECTED, tags );
        sslHandshakeTaskTimer = Timer.builder( NIO_SSL_HANDSHAKE_TASK ).tags( tags ).register( Metrics.globalRegistry );

        if( sslHandshakeMetricsRegistered ) return;

        Metrics.gauge( NIO_SSL_HANDSHAKE, tags.and( "type", "queue" ), this, s -> s.sslHandshakeStat( e -> e.getQueue().size() ) );
        Metrics.gauge( NIO_SSL_HANDSHAKE, tags.and( "type", ACTIVE ), this, s -> s.sslHandshakeStat( ThreadPoolExecutor::getActiveCount ) );
        FunctionCounter.builder( NIO_SSL_HANDSHAKE_COMPLETED, this, NioHttpServer::sslHandshakeCompleted )
            .tags( tags ).register( Metrics.globalRegistry );
        sslHandshakeMetricsRegistered = true;
    }

    /**
     * the current pool + the retired ones still completing their tasks
     */
    private double sslHandshakeStat( ToLongFunction<ThreadPoolExecutor> stat ) {
        synchronized( sslHandshakeLock ) {
            ThreadPoolExecutor executor = sslHandshakeExecutor;
            long value = executor != null ? stat.applyAsLong( executor ) : 0;
            for( ThreadPoolExecutor retired : sslHandshakeRetired ) value += stat.applyAsLong( retired );
            return value;
        }
    }

    private double sslHandshakeCompleted() {
        synchronized( sslHandshakeLock ) {
            purgeRetiredSslHandshakeExecutors();
            return sslHandshakeCompletedBefore + sslHandshakeStat( ThreadPoolExecutor::getCompletedTaskCount );
        }
    }

    /**
     * under sslHandshakeLock
     */
    private void purgeRetiredSslHandshakeExecutors() {
        sslHandshakeRetired.removeIf( executor -> {
            if( !executor.isTerminated() ) return false;
            sslHandshakeCompletedBefore += executor.getCompletedTaskCount();
            return true;
        } );
    }

    /**
     * under sslHandshakeLock: next becomes the current pool, the old one completes its queued tasks and terminates
     */
    private void replaceSslHandshakeExecutor( ThreadPoolExecutor next ) {
        ThreadPoolExecutor current = sslHandshakeExecutor;
        sslHandshakeExecutor = next;
        if( current != null ) {
            sslHandshakeRetired.add( current );
            current.shutdown();
        }
        purgeRetiredSslHandshakeExecutors();
    }

    /**
     * under sslHandshakeLock
     */
    private void resizeSslHandshakeExecutor() {
        ThreadPoolExecutor executor = sslHandshakeExecutor;
        if( executor == null ) return;

        int threads = sslHandshakePoolSize();
        if( threads > executor.getMaximumPoolSize() ) {
            executor.setMaximumPoolSize( threads );
            executor.setCorePoolSize( threads );
        } else {
            executor.setCorePoolSize( threads );
            executor.setMaximumPoolSize( threads );
        }
        log.info( "ssl handshake pool: threads {}", threads );
    }

    /**
     * runtime change: true - the pool is created, new handshake tasks go to it; false - new tasks run inline on io threads,
     * the pool completes its queued tasks and terminates
     */
    public void setSslHandshakeOffload( boolean sslHandshakeOffload ) {
        synchronized( sslHandshakeLock ) {
            log.info( "sslHandshakeOffload {} -> {}", this.sslHandshakeOffload, sslHandshakeOffload );

            if( sslHandshakeOffload ) {
                if( sslHandshakeInstalled && sslHandshakeExecutor == null ) sslHandshakeExecutor = newSslHandshakeExecutor();
                this.sslHandshakeOffload = true;
            } else {
                this.sslHandshakeOffload = false;
                replaceSslHandshakeExecutor( null );
            }
        }
    }

    /**
     * runtime change: the pool is resized in place
     */
    public void setSslHandshakeThreads( double sslHandshakeThreads ) {
        synchronized( sslHandshakeLock ) {
            log.info( "sslHandshakeThreads {} -> {}", this.sslHandshakeThreads, sslHandshakeThreads );
            this.sslHandshakeThreads = sslHandshakeThreads;
            resizeSslHandshakeExecutor();
        }
    }

    /**
     * runtime change: the pool is resized in place
     */
    public void setSslHandshakeMinThreads( int sslHandshakeMinThreads ) {
        Preconditions.checkArgument( sslHandshakeMinThreads >= 1, "sslHandshakeMinThreads must be >= 1, but %s", sslHandshakeMinThreads );

        synchronized( sslHandshakeLock ) {
            log.info( "sslHandshakeMinThreads {} -> {}", this.sslHandshakeMinThreads, sslHandshakeMinThreads );
            this.sslHandshakeMinThreads = sslHandshakeMinThreads;
            resizeSslHandshakeExecutor();
        }
    }

    /**
     * runtime change: the queue capacity is fixed, so the pool is replaced; the old one completes its queued tasks
     */
    public void setSslHandshakeQueue( int sslHandshakeQueue ) {
        Preconditions.checkArgument( sslHandshakeQueue >= 1, "sslHandshakeQueue must be >= 1, but %s", sslHandshakeQueue );

        synchronized( sslHandshakeLock ) {
            log.info( "sslHandshakeQueue {} -> {}", this.sslHandshakeQueue, sslHandshakeQueue );
            this.sslHandshakeQueue = sslHandshakeQueue;
            if( sslHandshakeExecutor != null ) replaceSslHandshakeExecutor( newSslHandshakeExecutor() );
        }
    }

    @SneakyThrows
    private void addPortListener( int port, PathHandler portPathHandler, Undertow.Builder builder ) {
        Preconditions.checkNotNull( portPathHandler );

        log.info( "starting server on port: {} with {} ...", port, portPathHandler );

        io.undertow.server.HttpHandler handler = portPathHandler;

        for( int i = handlers.size() - 1; i >= 0; i-- ) {
            NioHandlerBuilder nioHandlerBuilder = handlers.get( i );
            // compression is applied per bound path (see bind(..., compressionSupport, ...)), not per port
            if( nioHandlerBuilder instanceof CompressionNioHandler ) continue;
            handler = nioHandlerBuilder.build( handler );
        }

        handler = new GracefulShutdownHandler( handler );

        if( port == defaultPort.httpsPort ) {
            SSLContext sslContext = JsseSslUtils.createSSLContext( keyManagers, null, new SecureRandom(), OptionMap.create( Options.SSL_PROTOCOL, "TLSv1.2" ) );
            builder.addHttpsListener( port, "0.0.0.0", sslContext, handler );
        } else {
            builder.addHttpListener( port, "0.0.0.0", handler );
        }
    }

    /**
     * Undertow has no accept hook: the accept listener of every port is replaced with the same one ( accept + openListener )
     * that also counts the accepted connections. The channel and the openListener are private fields of ListenerInfo.
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private void addAcceptCounters( Undertow server ) {
        for( Undertow.ListenerInfo listenerInfo : server.getListenerInfo() ) {
            String sPort = String.valueOf( ( ( InetSocketAddress ) listenerInfo.getAddress() ).getPort() );

            AcceptingChannel<StreamConnection> channel = listenerInfoField( listenerInfo, "channel", AcceptingChannel.class );
            OpenListener openListener = listenerInfoField( listenerInfo, "openListener", OpenListener.class );
            Counter opened = Metrics.counter( NIO_CONNECTIONS_OPENED, Tags.of( "port", sPort ) );

            ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ch -> {
                try {
                    StreamConnection connection = ch.accept();
                    if( connection != null ) {
                        opened.increment();
                        ChannelListeners.invokeChannelListener( connection, openListener );
                    }
                } catch( IOException e ) {
                    log.warn( "port {}: failed to accept a connection: {}", sPort, e.toString() );
                }
            };

            ( ( ChannelListener.Setter ) channel.getAcceptSetter() ).set( acceptListener );
        }
    }

    private static <T> T listenerInfoField( Undertow.ListenerInfo listenerInfo, String name, Class<T> type ) {
        try {
            Field field = Undertow.ListenerInfo.class.getDeclaredField( name );
            field.setAccessible( true );
            return type.cast( Preconditions.checkNotNull( field.get( listenerInfo ) ) );
        } catch( ReflectiveOperationException | RuntimeException e ) {
            throw new IllegalStateException( "Undertow.ListenerInfo." + name + " is not accessible as " + type.getName()
                + ", the " + NIO_CONNECTIONS_OPENED + " metric cannot be installed", e );
        }
    }

    private void addStats( Undertow server ) {
        for( var listenerInfo : server.getListenerInfo() ) {
            var sa = ( InetSocketAddress ) listenerInfo.getAddress();
            var sPort = String.valueOf( sa.getPort() );

            ConnectorStatistics connectorStatistics = listenerInfo.getConnectorStatistics();

            Metrics.gauge( NIO_REQUESTS, Tags.of( "port", sPort, "type", "total" ), connectorStatistics, ConnectorStatistics::getRequestCount );
            Metrics.gauge( NIO_REQUESTS, Tags.of( "port", sPort, "type", ACTIVE ), connectorStatistics, ConnectorStatistics::getActiveRequests );
            Metrics.gauge( NIO_REQUESTS, Tags.of( "port", sPort, "type", "errors" ), connectorStatistics, ConnectorStatistics::getErrorCount );

            Metrics.gauge( "nio_connections", Tags.of( "port", sPort, "type", ACTIVE ), connectorStatistics, ConnectorStatistics::getActiveConnections );

            Metrics.gauge( NIO_POOL_SIZE, Tags.of( "port", sPort, "name", WORKER, "type", ACTIVE ), server, s -> s.getWorker().getMXBean().getWorkerPoolSize() );
            Metrics.gauge( NIO_POOL_SIZE, Tags.of( "port", sPort, "name", WORKER, "type", "core" ), server, s -> s.getWorker().getMXBean().getCoreWorkerPoolSize() );
            Metrics.gauge( NIO_POOL_SIZE, Tags.of( "port", sPort, "name", WORKER, "type", "max" ), server, s -> s.getWorker().getMXBean().getMaxWorkerPoolSize() );
            Metrics.gauge( NIO_POOL_SIZE, Tags.of( "port", sPort, "name", WORKER, "type", "busy" ), server, s -> s.getWorker().getMXBean().getBusyWorkerThreadCount() );
            Metrics.gauge( NIO_POOL_SIZE, Tags.of( "port", sPort, "name", WORKER, "type", "queue" ), server, s -> s.getWorker().getMXBean().getWorkerQueueSize() );
        }
    }

    public void bind( String prefix, HttpHandler handler, boolean compressionSupport, boolean blocking, List<PortType> types ) {
        if( types.isEmpty() || types.contains( PortType.HTTP ) ) {
            bind( prefix, handler, compressionSupport, blocking, DEFAULT_HTTP_PORT );
        }

        if( isHttpsEnabled() && ( types.isEmpty() || types.contains( PortType.HTTPS ) ) ) {
            bind( prefix, handler, compressionSupport, blocking, DEFAULT_HTTPS_PORT );
        }
    }

    public void bind( String prefix, HttpHandler handler, boolean compressionSupport, boolean blocking, String portName ) {
        Preconditions.checkNotNull( portName );
        Preconditions.checkNotNull( prefix );
        Preconditions.checkArgument( !prefix.isEmpty() );

        if( !defaultPorts.containsKey( portName ) && !additionalHttpPorts.containsKey( portName ) ) {
            throw new IllegalArgumentException( "Unknown port " + portName );
        }

        int port = defaultPorts.getOrDefault( portName, -1 );
        if( port <= 0 ) {
            port = additionalHttpPorts.get( portName );
        }

        io.undertow.server.HttpHandler httpHandler = new io.undertow.server.HttpHandler() {
            @Override
            public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
                HttpServerExchange serverExchange = new HttpServerExchange( exchange, requestId.incrementAndGet(), this );
                handler.handleRequest( serverExchange );
            }
        };

        if( compressionSupport ) {
            httpHandler = new CompressionNioHandler().build( httpHandler );
        }

        PathHandler assignedHandler = pathHandler.computeIfAbsent( port, p -> new PathHandler( pathHandlerCacheSize ) );

        if( blocking ) {
            httpHandler = new BlockingHandler( httpHandler );
        }

        assignedHandler.addPrefixPath( prefix, httpHandler );

        log.debug( "binding '{}' on port: {}:{}", prefix, portName, port );
    }

    public void bind( String prefix, HttpHandler handler ) {
        bind( prefix, handler, true );
    }

    public void bind( String prefix, HttpHandler handler, boolean blocking ) {
        bind( prefix, handler, blocking, DEFAULT_HTTP_PORT );
        if( isHttpsEnabled() ) {
            bind( prefix, handler, blocking, DEFAULT_HTTPS_PORT );
        }
    }

    public void bind( String prefix, HttpHandler handler, boolean blocking, String port ) {
        bind( prefix, handler, true, blocking, port );
    }

    public void bind( String prefix, HttpHandler handler, String port ) {
        bind( prefix, handler, true, true, port );
    }

    public void preStop() {
        try {
            if( undertow != null ) {
                undertow.stop();
            }
            if( xnioWorker != null ) {
                xnioWorker.shutdown();
                if( !xnioWorker.awaitTermination( Dates.s( 10 ), TimeUnit.MILLISECONDS ) ) {
                    xnioWorker.shutdownNow();
                }
                xnioWorker = null;
            }
            synchronized( sslHandshakeLock ) {
                sslHandshakeInstalled = false;
                ThreadPoolExecutor sslExecutor = sslHandshakeExecutor;
                sslHandshakeExecutor = null;
                if( sslExecutor != null ) sslHandshakeRetired.add( sslExecutor );
                for( ThreadPoolExecutor executor : sslHandshakeRetired ) {
                    executor.shutdownNow();
                    sslHandshakeCompletedBefore += executor.getCompletedTaskCount();
                }
                sslHandshakeRetired.clear();
            }
        } catch( Exception ex ) {
            log.error( "Cannot stop server", ex );
        }
        pathHandler.clear();
        undertow = null;
    }

    @Override
    public void close() throws IOException {
        if( autoRefreshCertificatesScheduled != null ) {
            Closeables.close( autoRefreshCertificatesScheduled );
            autoRefreshCertificatesScheduled = null;
        }

        preStop();

        Closeables.close( xnioWorker );
    }

    public boolean hasHandler( Class<? extends NioHandler> handlerClass ) {
        return Lists.anyMatch( handlers, h -> h.getClass().equals( handlerClass ) );
    }

    public void refreshCertificates() {
        try {
            if( isHttpsEnabled() ) {
                log.debug( "refreshCertificates location {}...", defaultPort.keyStore );

                keyManagers = makeKeyManagers( defaultPort.keyStore, defaultPort.password );

                List<Undertow.ListenerInfo> listenerInfo = undertow.getListenerInfo();

                SSLContext sslContext = JsseSslUtils.createSSLContext( keyManagers, null, new SecureRandom(), OptionMap.create( Options.SSL_PROTOCOL, "TLSv1.2" ) );

                listenerInfo
                    .stream()
                    .filter( li -> li.getSslContext() != null )
                    .forEach( li -> li.setSslContext( sslContext ) );

                log.debug( "refreshCertificates location {}... Done", defaultPort.keyStore );
            }
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
    }

    public enum PortType {
        HTTP, HTTPS
    }

    @ToString
    public static class DefaultPort {
        public final int httpPort;
        public final int httpsPort;
        public final URL keyStore;
        public final String password;

        public DefaultPort( int httpPort, int httpsPort, URL keyStore, String password ) {
            this.httpPort = httpPort;
            this.httpsPort = httpsPort;
            this.keyStore = keyStore;
            this.password = password;
        }

        public DefaultPort( int httpPort ) {
            this( httpPort, -1, null, null );
        }
    }
}
