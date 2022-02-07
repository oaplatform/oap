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
import oap.http.ContentTypes;
import oap.http.Headers;
import oap.http.HttpStatusCodes;
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

import static oap.message.MessageProtocol.MD5_LENGTH;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_OK;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR_NO_RETRY;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;

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
    public final int clientHashCacheSize = 1024;
    private final List<MessageListener> listeners;
    private final long hashTtl;
    private final Path controlStatePath;
    private final NioHttpServer server;
    private final String context;
    private MessageHashStorage hashes;
    private Scheduled scheduled;

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

    public void start() {
        log.info( "controlStatePath '{}' listeners {} hashTtl {} clientHashCacheSize {} http context '{}'",
            controlStatePath, Lists.map( listeners, MessageListener::getClass ), Dates.durationToString( hashTtl ),
            clientHashCacheSize, context );

        hashes = new MessageHashStorage( clientHashCacheSize );
        Metrics.gauge( "messages_hash", Tags.empty(), hashes, MessageHashStorage::size );

        server.bind( context, this );

        scheduled = Scheduler.scheduleWithFixedDelay( 1, TimeUnit.SECONDS, this::updateHash );

        try {
            if( controlStatePath.toFile().exists() ) hashes.load( controlStatePath );
        } catch( Exception e ) {
            log.warn( e.getMessage() );
        }

        for( var listener : listeners ) {
            var d = this.map.put( listener.getId(), listener );
            if( d != null )
                throw new IllegalArgumentException( "duplicate [" + listener.getInfo() + ", " + d.getInfo() + "]" );
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
            log.trace( "new message from {}", clientHostPort );

            var messageVersion = in.readShort();
            var clientId = in.readLong();
            final var md5 = Hex.encodeHexString( in.readNBytes( MD5_LENGTH ) ).intern();

            in.skipBytes( 8 ); // reserved
            var size = in.readInt();

            log.trace( "[{}] type {} version {} clientId {} md5 {} size '{}'",
                clientHostPort, messageType, messageVersion, clientId, md5, FileUtils.byteCountToDisplaySize( size ) );

            synchronized( md5 ) {
                if( !hashes.contains( messageType, md5 ) ) {
                    var listener = map.get( messageType );
                    if( listener == null ) {
                        log.error( "[{}] Unknown message type {}", clientHostPort, messageType );
                        in.skipNBytes( size );
                        writeResponse( exchange, STATUS_UNKNOWN_MESSAGE_TYPE, clientId, md5 );
                    } else {
                        var data = in.readNBytes( size );
                        short status;
                        try {
                            status = listener.run( messageVersion, hostName, size, data, md5 );

                            writeResponse( exchange, status, clientId, md5 );
                            if( status == STATUS_OK ) {
                                hashes.add( messageType, clientId, md5 );
                                Metrics.counter( "messages", Tags.of( "type", String.valueOf( Byte.toUnsignedInt( messageType ) ) ) ).increment();
                            } else {
                                log.trace( "[{}] WARN [{}/{}] buffer ({}, " + size + ") status == {}.)",
                                    clientHostPort, hostName, clientId, md5, MessageProtocol.statusToString( status ) );
                            }
                        } catch( Exception e ) {
                            log.error( "[" + clientHostPort + "] " + e.getMessage(), e );
                            writeResponse( exchange, STATUS_UNKNOWN_ERROR_NO_RETRY, clientId, md5 );
                        }
                    }
                } else {
                    log.warn( "[{}/{}] buffer ({}, {}) already written.)", clientHostPort, clientId, md5, size );
                    Metrics.counter( "oap.message.server.already_written", "type", String.valueOf( Byte.toUnsignedInt( messageType ) ) ).increment();

                    in.skipNBytes( size );

                    writeResponse( exchange, STATUS_ALREADY_WRITTEN, clientId, md5 );
                }
            }
        }
    }

    public void writeResponse( HttpServerExchange exchange, short status, long clientId, String md5 ) throws IOException, DecoderException {
        exchange.setResponseHeader( Headers.CONTENT_TYPE, ContentTypes.APPLICATION_OCTET_STREAM );
        exchange.setStatusCode( HttpStatusCodes.OK );

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
            if( scheduled != null ) {
                scheduled.close();
            }
            hashes.store( controlStatePath );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
