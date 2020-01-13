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
import oap.concurrent.Threads;
import oap.io.Closeables;
import oap.io.Files;
import oap.json.Binder;
import oap.util.ByteSequence;
import oap.util.Cuid;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
public class MessageSender implements Closeable, Runnable {
    private final String host;
    private final int port;
    private final Path directory;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final MessageDigest md5Digest;
    private final ConcurrentHashMap<ByteSequence, Message> messages = new ConcurrentHashMap<>();
    public long retryAfter = 1000;
    protected long timeout = 5000;
    private MessageSocketConnection connection;
    private boolean loggingAvailable = true;
    private boolean closed = false;

    public MessageSender( String host, int port, Path directory ) {
        this.host = host;
        this.port = port;
        this.directory = directory;

        md5Digest = DigestUtils.getMd5Digest();

        log.info( "message server host = {}, port = {}, storage = {}", host, port, directory );
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

    public CompletableFuture<?> sendJson( byte messageType, Object data ) {
        var baos = new ByteArrayOutputStream();
        Binder.json.marshal( baos, data );
        return sendObject( messageType, baos.toByteArray() );
    }

    public CompletableFuture<?> sendObject( byte messageType, byte[] data ) {
        md5Digest.reset();
        md5Digest.update( data );
        var md5 = md5Digest.digest();

        var message = new Message( clientId, messageType, ByteSequence.of( md5 ), data );
        messages.put( message.md5, message );

        return CompletableFuture.runAsync( () -> {
            while( !closed ) {
                try {
                    if( _sendObject( message ) ) {
                        messages.remove( message.md5 );
                        return;
                    }
                    Threads.sleepSafely( retryAfter );
                } catch( Exception e ) {
                    log.trace( e.getMessage(), e );
                    log.trace( "retrying..." );
                }
            }
        } );
    }

    private synchronized boolean _sendObject( Message message ) throws IOException {
        if( !closed ) {
            try {
                loggingAvailable = true;

                refreshConnection();

                log.debug( "sending data [type = {}] to server...", message.messageType );

                var out = connection.out;
                var in = connection.in;

                out.writeByte( message.messageType );
                out.writeShort( PROTOCOL_VERSION_1 );
                out.writeLong( message.clientId );

                out.write( message.md5.bytes );

                out.write( MessageProtocol.RESERVED, 0, MessageProtocol.RESERVED_LENGTH );
                out.writeInt( message.data.length );
                out.write( message.data );

                var version = in.readByte();
                if( version != PROTOCOL_VERSION_1 ) {
                    log.error( "Version mismatch, expected: {}, received: {}", PROTOCOL_VERSION_1, version );
                    Closeables.close( connection );
                    throw new MessageException( "Version mismatch" );
                }
                in.readLong(); // clientId
                in.skipNBytes( MessageProtocol.MD5_LENGTH ); // digestionId
                in.skipNBytes( MessageProtocol.RESERVED_LENGTH );
                var status = in.readShort();

                if( log.isTraceEnabled() )
                    log.trace( "sending done, server status: {}", getServerStatus( status ) );

                switch( status ) {
                    case STATUS_ALREADY_WRITTEN:
                        log.trace( "already written {}", message.getHexMd5() );
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

        saveMessagesToDirectory( directory );
    }

    private void saveMessagesToDirectory( Path directory ) {
        while( !messages.isEmpty() ) {
            messages.values().removeIf( msg -> {
                var parentDirectory = directory
                    .resolve( Long.toHexString( clientId ) )
                    .resolve( String.valueOf( Byte.toUnsignedInt( msg.messageType ) ) );
                var tmpMsgPath = parentDirectory.resolve( msg.getHexMd5() + ".bin.tmp" );
                log.debug( "writing unsent message to {}", tmpMsgPath );
                try {
                    Files.write( tmpMsgPath, msg.data );
                    var msgPath = parentDirectory.resolve( msg.getHexMd5() + ".bin" );
                    Files.rename( tmpMsgPath, msgPath );
                } catch( Exception e ) {
                    log.error( "type: {}, md5: {}, data: {}",
                        msg.messageType, msg.getHexMd5(), msg.getHexData() );
                }

                return true;
            } );
        }
    }

    public MessageAvailabilityReport availabilityReport() {
        boolean operational = loggingAvailable && !closed;
        return new MessageAvailabilityReport( operational ? OPERATIONAL : FAILED );
    }

    @Override
    public void run() {
        var messageFiles = Files.fastWildcard( directory, "*/*/*.bin" );

        for( var msgFile : messageFiles ) {
            var lockFile = Paths.get( FilenameUtils.removeExtension( msgFile.toString() ) + ".lock" );

            if( Files.createFile( lockFile ) ) {
                log.debug( "reading unsent message {}", msgFile );
                try {
                    var fileName = FilenameUtils.getName( msgFile.toString() );
                    var md5Hex = FilenameUtils.removeExtension( fileName );
                    var md5 = ByteSequence.of( Hex.decodeHex( md5Hex.toCharArray() ) );
                    var typePath = FilenameUtils.getFullPathNoEndSeparator( msgFile.toString() );
                    var messageTypeStr = FilenameUtils.getName( typePath );
                    var messageType = ( byte ) Integer.parseInt( messageTypeStr );

                    var clientIdPath = FilenameUtils.getFullPathNoEndSeparator( typePath );
                    var clientIdStr = FilenameUtils.getName( clientIdPath );
                    var msgClientId = Long.parseLong( clientIdStr, 16 );

                    var data = Files.read( msgFile );

                    log.trace( "client id = {}, message type = {}, md5 = {}", msgClientId, messageType, md5Hex );

                    var msg = new Message( clientId, messageType, md5, data );

                    if( _sendObject( msg ) ) {
                        Files.delete( msgFile );
                        Files.delete( lockFile );
                    }
                } catch( Exception e ) {
                    log.error( msgFile + ": " + e.getMessage(), e );
                }
            }
        }

        Files.deleteEmptyDirectories( directory, false );
    }

    private static final class Message {
        public final ByteSequence md5;
        public final byte messageType;
        public final long clientId;
        public final byte[] data;

        public Message( long clientId, byte messageType, ByteSequence md5, byte[] data ) {
            this.clientId = clientId;
            this.md5 = md5;
            this.messageType = messageType;
            this.data = data;
        }

        public String getHexMd5() {
            return Hex.encodeHexString( md5.bytes );
        }

        public String getHexData() {
            return Hex.encodeHexString( data );
        }
    }
}
