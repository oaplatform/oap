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

package oap.zabbix.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.ObjectWriter;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;
import lombok.val;
import oap.net.Inet;
import oap.zabbix.Data;
import oap.zabbix.Request;
import org.apache.commons.lang3.NotImplementedException;

import javax.net.SocketFactory;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by igor.petrenko on 29.09.2017.
 */
public class ZabbixAppender extends AppenderBase<ILoggingEvent> implements SocketConnector.ExceptionHandler {
    public static final int DEFAULT_QUEUE_SIZE = 128;
    private static final int prefixLength = "zabbix.".length();
    private static final int DEFAULT_EVENT_DELAY_TIMEOUT = 100;
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;

    private int queueSize = DEFAULT_QUEUE_SIZE;
    private String remoteHost;
    private int port;
    private InetAddress address;
    private Duration eventDelayLimit = new Duration( DEFAULT_EVENT_DELAY_TIMEOUT );
    private int acceptConnectionTimeout = DEFAULT_ACCEPT_CONNECTION_DELAY;
    private Future<?> task;
    private String peerId;

    volatile private LinkedBlockingDeque<ILoggingEvent> deque;
    private volatile Socket socket;
    private SocketConnector connector;

    @Override
    protected void append( ILoggingEvent event ) {
        if( event == null || !isStarted() )
            return;

        try {
            final boolean inserted = deque.offer( event, eventDelayLimit.getMilliseconds(), TimeUnit.MILLISECONDS );
            if( !inserted ) {
                addInfo( "Dropping event due to timeout limit of [" + eventDelayLimit + "] being exceeded" );
            }
        } catch( InterruptedException e ) {
            addError( "Interrupted while appending event to SocketAppender", e );
        }
    }

    @Override
    public void start() {
        if( isStarted() )
            return;

        int errorCount = 0;
        if( port <= 0 ) {
            errorCount++;
            addError( "No port was configured for appender" + name + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_port" );
        }

        if( remoteHost == null ) {
            errorCount++;
            addError( "No remote host was configured for appender" + name
                + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_host" );
        }

        if( queueSize == 0 ) {
            addWarn( "Queue size of zero is deprecated, use a size of one to indicate synchronous processing" );
        }

        if( queueSize < 0 ) {
            errorCount++;
            addError( "Queue size must be greater than zero" );
        }

        if( errorCount == 0 ) {
            try {
                address = InetAddress.getByName( remoteHost );
            } catch( UnknownHostException ex ) {
                addError( "unknown host: " + remoteHost );
                errorCount++;
            }
        }


        if( errorCount == 0 ) {
            deque = createDeque();
            peerId = "remote peer " + remoteHost + ":" + port + ": ";
            connector = createConnector( address, port );
            task = getContext().getScheduledExecutorService().submit( this::connectSocketAndDispatchEvents );
            super.start();
        }
    }

    @Override
    public void stop() {
        if( !isStarted() )
            return;
        CloseUtil.closeQuietly( socket );
        task.cancel( true );
        super.stop();
    }

    private void connectSocketAndDispatchEvents() {
        try {
            while( socketConnectionCouldBeEstablished() ) {
                try {
                    ObjectWriter objectWriter = createObjectWriterForSocket();
                    addInfo( peerId + "connection established" );
                    dispatchEvents( objectWriter );
                } catch( IOException ex ) {
                    addInfo( peerId + "connection failed: " + ex );
                } finally {
                    CloseUtil.closeQuietly( socket );
                    socket = null;
                    addInfo( peerId + "connection closed" );
                }
            }
        } catch( InterruptedException ex ) {
            assert true; // ok... we'll exit now
        }
        addInfo( "shutting down" );
    }

    private void dispatchEvents( ObjectWriter objectWriter ) throws InterruptedException, IOException {
        while( true ) {
            val q = deque;
            deque = createDeque();

            val dataList = q
                .stream()
                .map( ( ILoggingEvent event ) -> new Data( Inet.hostname(), event.getLoggerName().substring( prefixLength ), event.getMessage() ) )
                .collect( Collectors.toList() );

            val request = new Request( dataList );
            val zabbixRequest = new ZabbixRequest( request );

            try {
                objectWriter.write( zabbixRequest );
            } catch( IOException e ) {
                tryReAddingEventsToFrontOfQueue( q );
                throw e;
            }
        }
    }

    private LinkedBlockingDeque<ILoggingEvent> createDeque() {
        return new LinkedBlockingDeque<>( queueSize );
    }

    private void tryReAddingEventsToFrontOfQueue( Collection<ILoggingEvent> events ) {
        events.forEach( event -> {
            final boolean wasInserted = deque.offerFirst( event );
            if( !wasInserted ) {
                addInfo( "Dropping event due to socket connection error and maxed out deque capacity" );
            }
        } );
    }

    private boolean socketConnectionCouldBeEstablished() throws InterruptedException {
        return ( socket = connector.call() ) != null;
    }

    private SocketConnector createConnector( InetAddress address, int port ) {
        SocketConnector connector = newConnector( address, port );
        connector.setExceptionHandler( this );
        connector.setSocketFactory( getSocketFactory() );
        return connector;
    }

    @Override
    public void connectionFailed( SocketConnector connector, Exception ex ) {

    }

    protected SocketConnector newConnector( InetAddress address, int port ) {
        return new DefaultSocketConnector( address, port, 0, 0 );
    }

    protected SocketFactory getSocketFactory() {
        return SocketFactory.getDefault();
    }

    private ObjectWriter createObjectWriterForSocket() throws IOException {
        socket.setSoTimeout( acceptConnectionTimeout );
        ObjectWriter objectWriter = new DirectAutoFlushingObjectWriter( socket.getOutputStream() );
        socket.setSoTimeout( 0 );
        return objectWriter;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost( String host ) {
        remoteHost = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize( int queueSize ) {
        this.queueSize = queueSize;
    }

    public Duration getEventDelayLimit() {
        return eventDelayLimit;
    }

    public void setEventDelayLimit( Duration eventDelayLimit ) {
        this.eventDelayLimit = eventDelayLimit;
    }

    void setAcceptConnectionTimeout( int acceptConnectionTimeout ) {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }

    private static class DirectAutoFlushingObjectWriter implements ObjectWriter {
        private final OutputStream outputStream;

        public DirectAutoFlushingObjectWriter( OutputStream outputStream ) {
            this.outputStream = outputStream;
        }

        @Override
        public void write( Object object ) throws IOException {
            throw new NotImplementedException( "" );
        }

        public void write( Externalizable externalizable ) throws IOException {
            externalizable.writeExternal( new ObjectOutputStream( outputStream ) );
            outputStream.flush();
        }
    }
}
