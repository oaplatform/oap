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
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;
import lombok.val;
import oap.net.Inet;
import oap.zabbix.Data;
import oap.zabbix.Request;

import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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

    private LinkedBlockingDeque<ILoggingEvent> deque;
    private volatile Socket socket;
    private SocketConnector connector;

    private static void addEvent( ILoggingEvent event, ArrayList<Data> list ) {
        val data = new Data( Inet.hostname(), event.getLoggerName().substring( prefixLength ), event.getFormattedMessage() );

        list.add( data );
    }

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
            while( true ) {
                try {
                    dispatchEvents();
                } catch( IOException ex ) {
                    addInfo( peerId + "connection failed: " + ex );
                }
            }
        } catch( InterruptedException ex ) {
            assert true; // ok... we'll exit now
        }
        addInfo( "shutting down" );
    }

    private void dispatchEvents() throws InterruptedException, IOException {
        ILoggingEvent event = deque.takeFirst();

        val events = new ArrayList<ILoggingEvent>();
        events.add( event );

        val list = new ArrayList<Data>();

        addEvent( event, list );

        while( ( event = deque.pollFirst() ) != null ) {
            events.add( event );
            addEvent( event, list );
        }


        val request = new Request( list );
        val zabbixRequest = new ZabbixRequest( request );

        try {
            if( !socketConnectionCouldBeEstablished() ) {
                throw new ConnectException();
            }

            addInfo( peerId + "connection established" );

            val objectOutputStream = new ObjectOutputStream( socket.getOutputStream() );
            val inputStream = socket.getInputStream();

            addInfo( "zabbixRequest = " + zabbixRequest );
            zabbixRequest.writeExternal( objectOutputStream );
            objectOutputStream.flush();

            val buf = new byte[1024];
            val responseBaos = new ByteArrayOutputStream();


            while( true ) {
                int read = inputStream.read( buf );
                if( read <= 0 ) {
                    break;
                }
                responseBaos.write( buf, 0, read );
            }

            val bResponse = responseBaos.toByteArray();

            if( bResponse.length < 13 ) {
                addInfo( "response.length < 13" );
            } else {
                String jsonString = new String( bResponse, 13, bResponse.length - 13, StandardCharsets.UTF_8 );
                addInfo( "response = " + jsonString );
            }

        } catch( IOException e ) {
            tryReAddingEventsToFrontOfQueue( events );
            throw e;
        } finally {
            CloseUtil.closeQuietly( socket );
            socket = null;
            addInfo( peerId + "connection closed" );
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

    private ObjectOutputStream createObjectWriterForSocket() throws IOException {
        socket.setSoTimeout( acceptConnectionTimeout );
        ObjectOutputStream objectWriter = new ObjectOutputStream( socket.getOutputStream() );
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
}
