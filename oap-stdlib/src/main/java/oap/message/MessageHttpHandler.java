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

package oap.message;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.util.Dates;
import oap.util.Lists;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.http.Http.ContentType.APPLICATION_OCTET_STREAM;
import static oap.http.Http.Headers.CONTENT_TYPE;
import static oap.message.MessageProtocol.MD5_LENGTH;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_OK;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR_NO_RETRY;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;
import static oap.message.MessageProtocol.messageStatusToString;
import static oap.message.MessageProtocol.messageTypeToString;

/**
 * Input protocol:
 * <ul>
 * <li><b>byte</b>         - message type
 * <li><b>short</b>        - message version
 * <li><b>long</b>         - client id
 * <li><b>byte(16)</b>     - md5
 * <li><b>bytes(8)</b>     - reserved
 * <li><b>int</b>          - data size
 * <li><b>...</b>          - protocol data
 * </ul>
 * <p>
 * output protocol:
 * <ul>
 * <li><b>short</b>        - message version
 * <li><b>long</b>         - client id
 * <li><b>byte(16)</b>     - md5
 * <li><b>bytes(8)</b>     - reserved
 * <li><b>short</b>        - response status
 * <ul>
 * <li><b>0</b>   - ok
 * <li><b>1</b>   - unknown error
 * <li><b>100</b>   - unknown message type
 * <li><b>101</b>   - already written
 * </ul>
 * </ul>
 * <p>
 * <p>
 * Created by igor.petrenko on 2019-12-10.
 */
@Slf4j
public class MessageHttpHandler implements HttpHandler, Closeable {
    public final HashMap<Byte, MessageListener> map = new HashMap<>();
    public int clientHashCacheSize = 1024;
    private final List<MessageListener> listeners;
    private final long hashTtl;
    private final Path controlStatePath;
    private final NioHttpServer server;
    private final String context;
    private MessageHashStorage hashes;
    private Scheduled scheduled;

    public int port = -1;

    public MessageHttpHandler( NioHttpServer server, String context, Path controlStatePath, List<MessageListener> listeners, long hashTtl ) {
        this.server = server;
        this.context = context;
        this.controlStatePath = controlStatePath;
        this.listeners = listeners;
        this.hashTtl = hashTtl;
    }

    public void updateHash() {
        hashes.update( hashTtl );
    }

    public void preStart() {
        log.info( "controlStatePath '{}' listeners {} hashTtl {} clientHashCacheSize {} http context '{}'",
            controlStatePath, Lists.map( listeners, MessageListener::getClass ), Dates.durationToString( hashTtl ),
            clientHashCacheSize, context );

        log.info( "custom status = {}", MessageProtocol.printMapping() );

        hashes = new MessageHashStorage( clientHashCacheSize );
        Metrics.gauge( "messages_hash", Tags.empty(), hashes, MessageHashStorage::size );

        if( port == -1 )
            server.bind( context, this );
        else
            server.bind( context, this, port );

        scheduled = Scheduler.scheduleWithFixedDelay( 1, TimeUnit.SECONDS, this::updateHash );

        try {
            if( controlStatePath.toFile().exists() ) hashes.load( controlStatePath );
        } catch( Exception e ) {
            log.warn( "Cannot load hashes", e );
        }

        for( var listener : listeners ) {
            var d = this.map.put( listener.getId(), listener );
            if( d != null )
                throw new IllegalArgumentException( "duplicate listener [" + listener.getId() + ":" + listener.getInfo()
                        + ", " + d.getId() + ":" + d.getInfo() + "]" );
        }
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        try( var in = new DataInputStream( exchange.getInputStream() ) ) {
            InetSocketAddress peerAddress = ( InetSocketAddress ) exchange.exchange.getConnection().getPeerAddress();
            var hostName = peerAddress.getHostName();
            var port = peerAddress.getPort();
            String clientHostPort = hostName + ":" + port;

            var messageType = in.readByte();

            var messageVersion = in.readShort();
            var clientId = in.readLong();
            var md5 = Hex.encodeHexString( in.readNBytes( MD5_LENGTH ) ).intern();

            in.skipBytes( 8 ); // reserved
            var size = in.readInt();

            log.trace( "new message from [{}], type {}, version {}, clientId {}, md5 {}, size '{}'",
                clientHostPort, messageTypeToString( messageType ), messageVersion, clientId, md5, FileUtils.byteCountToDisplaySize( size ) );

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized( md5 ) {
                if ( hashes.contains( messageType, md5 ) ) {
                    log.warn( "[{}/{}] buffer ({}, {}) already written.)", clientHostPort, clientId, md5, size );
                    Metrics.counter( "oap.server.messages", Tags.of( "type", messageTypeToString( messageType ), "status", messageStatusToString( STATUS_ALREADY_WRITTEN ) ) ).increment();

                    in.skipNBytes( size );

                    writeResponse( exchange, STATUS_ALREADY_WRITTEN, clientId, md5 );
                    return;
                }
                var listener = map.get( messageType );
                if( listener == null ) {
                    log.error( "[{}] Unknown message type {}", clientHostPort, messageType );
                    in.skipNBytes( size );
                    writeResponse( exchange, STATUS_UNKNOWN_MESSAGE_TYPE, clientId, md5 );
                    return;
                }
                var data = in.readNBytes( size );
                short status;
                try {
                    status = listener.run( messageVersion, hostName, size, data, md5 );
                    if( status == STATUS_OK ) {
                        hashes.add( messageType, clientId, md5 );
                    } else {
                        log.trace( "[{}] WARN [{}/{}] buffer ({}, " + size + ") status == {}.)",
                                clientHostPort, hostName, clientId, md5, messageStatusToString( status ) );
                    }
                    Metrics.counter( "oap.server.messages", Tags.of( "type", String.valueOf( Byte.toUnsignedInt( messageType ) ), "status", messageStatusToString( status ) ) ).increment();
                    writeResponse( exchange, status, clientId, md5 );
                } catch( Exception e ) {
                    log.error( "[" + clientHostPort + "] " + e.getMessage(), e );
                    Metrics.counter( "oap.server.messages", Tags.of( "type", messageTypeToString( messageType ), "status", messageStatusToString( STATUS_UNKNOWN_ERROR_NO_RETRY ) ) ).increment();
                    writeResponse( exchange, STATUS_UNKNOWN_ERROR_NO_RETRY, clientId, md5 );
                }
            }
        }
    }

    public void writeResponse( HttpServerExchange exchange, short status, long clientId, String md5 ) throws IOException, DecoderException {
        exchange.setResponseHeader( CONTENT_TYPE, APPLICATION_OCTET_STREAM );
        exchange.setStatusCode( HTTP_OK );

        try( var out = new DataOutputStream( exchange.getOutputStream() ) ) {
            out.writeByte( PROTOCOL_VERSION_1 );
            out.writeLong( clientId );
            out.write( Hex.decodeHex( md5 ) );
            out.write( MessageProtocol.RESERVED, 0, MessageProtocol.RESERVED.length );
            out.writeShort( status );
        }
        exchange.endExchange();
    }

    @Override
    public void close() {
        try {
            if( scheduled != null ) scheduled.close();
            hashes.store( controlStatePath );
        } catch( IOException e ) {
            log.error( "Cannot close handler", e );
        }
    }
}
