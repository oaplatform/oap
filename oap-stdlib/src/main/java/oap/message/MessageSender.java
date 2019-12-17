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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.json.Binder;
import oap.util.Cuid;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.security.MessageDigest;

import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_OK;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;

/**
 * Created by igor.petrenko on 2019-12-11.
 */
@Slf4j
@ToString
public class MessageSender implements Closeable {
    private final String host;
    private final int port;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final MessageDigest md5Digest;
    public int retry = 2;
    protected long timeout = 5000;
    private MessageSocketConnection connection;
    private boolean loggingAvailable = true;
    private boolean closed = false;

    protected MessageSender( String host, int port ) {
        this.host = host;
        this.port = port;

        md5Digest = DigestUtils.getMd5Digest();

        log.info( "message server host = {}, port = {}", host, port );
    }

    private static String getServerStatus( short status ) {
        return switch( status ) {
            case STATUS_OK -> "OK";
            case STATUS_UNKNOWN_ERROR -> "UNKNOWN_ERROR";
            case STATUS_ALREADY_WRITTEN -> "ALREADY_WRITTEN";
            case STATUS_UNKNOWN_MESSAGE_TYPE -> "UNKNOWN_MESSAGE_TYPE";
            default -> "Unknown status: " + status;
        };
    }

    public boolean sendJson( byte messageType, Object data ) {
        var baos = new ByteArrayOutputStream();
        Binder.json.marshal( baos, data );
        return sendObject( messageType, baos.toByteArray() );
    }

    public boolean sendObject( byte messageType, byte[] data ) {
        for( var i = 0; i < retry; i++ ) {
            try {
                return _sendObject( messageType, data );
            } catch( Exception e ) {
                log.trace( e.getMessage(), e );
                log.trace( "retrying..." );
            }
        }

        return false;
    }

    private boolean _sendObject( byte messageType, byte[] object ) throws IOException {
        if( !closed ) try {
            loggingAvailable = true;

            refreshConnection();

            log.debug( "sending data [type = {}] to server...", messageType );

            var out = connection.out;
            var in = connection.in;

            out.writeByte( messageType );
            out.writeShort( PROTOCOL_VERSION_1 );
            out.writeLong( clientId );

            md5Digest.reset();
            md5Digest.update( object );
            var digest = md5Digest.digest();
            out.write( digest );

            out.write( MessageProtocol.RESERVED, 0, MessageProtocol.RESERVED_LENGTH );
            out.writeInt( object.length );
            out.write( object );

            var version = in.readByte();
            if( version != PROTOCOL_VERSION_1 ) {
                log.error( "Version mismatch, expected: {}, received: {}", PROTOCOL_VERSION_1, version );
                throw new MessageException( "Version mismatch" );
            }
            in.readLong(); // clientId
            in.skipNBytes( 16 ); // digestionId
            in.skipNBytes( MessageProtocol.RESERVED_LENGTH );
            var status = in.readShort();

            if( log.isTraceEnabled() )
                log.trace( "sending done, server status: {}", getServerStatus( status ) );

            switch( status ) {
                case STATUS_ALREADY_WRITTEN:
                    log.trace( "already written {}", Hex.encodeHexString( digest ) );
                    return true;
                case STATUS_OK:
                    return true;
                case STATUS_UNKNOWN_ERROR:
                case STATUS_UNKNOWN_MESSAGE_TYPE:
                    log.error( "sendObject error: {}", status );
                    return false;
                default:
                    log.error( "unknown status: {}", status );
                    return false;
            }

        } catch( IOException e ) {
            loggingAvailable = false;

            Closeables.close( connection );

            throw e;
        } catch( Exception e ) {
            loggingAvailable = false;

            log.warn( e.getMessage() );
            log.trace( e.getMessage(), e );
            Closeables.close( connection );
        }

        if( !loggingAvailable ) log.debug( "logging unavailable" );

        return false;
    }

    private void refreshConnection() {
        if( this.connection == null || !connection.isConnected() ) {
            Closeables.close( connection );
            log.debug( "opening connection..." );
            this.connection = new MessageSocketConnection( host, port, timeout );
            log.debug( "connected!" );
        }
    }

    @Override
    public synchronized void close() {
        closed = true;

        Closeables.close( connection );
    }

    public MessageAvailabilityReport availabilityReport() {
        boolean operational = loggingAvailable && !closed;
        return new MessageAvailabilityReport( operational ? OPERATIONAL : FAILED );
    }
}
