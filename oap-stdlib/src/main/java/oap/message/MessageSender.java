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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Metrics;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.io.Files;
import oap.json.Binder;
import oap.util.ByteSequence;
import oap.util.Cuid;
import oap.util.Dates;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTimeUtils;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_OK;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;
import static oap.message.MessageStatus.ALREADY_WRITTEN;
import static oap.message.MessageStatus.ERROR;
import static oap.message.MessageStatus.OK;

@Slf4j
@ToString
public class MessageSender implements Closeable, Runnable {
    private final String host;
    private final int port;
    private final Path directory;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final ConcurrentHashMap<ByteSequence, Message> messages = new ConcurrentHashMap<>();
    public long retryAfter = 1000;
    public long storageLockExpiration = Dates.h( 1 );
    public int poolSize = 4;
    protected long timeout = 5000;
    protected long connectionTimeout = Dates.s( 30 );
    private ThreadPoolExecutor pool;
    private ConnectionState[] states;
    private boolean closed = false;
    private Semaphore poolSemaphore;

    public MessageSender( String host, int port, Path directory ) {
        this.host = host;
        this.port = port;
        this.directory = directory;
    }

    private static String getServerStatus( short status, Function<Short, String> checkStatus ) {
        return switch( status ) {
            case STATUS_OK -> "OK";
            case STATUS_UNKNOWN_ERROR -> "UNKNOWN_ERROR";
            case STATUS_ALREADY_WRITTEN -> "ALREADY_WRITTEN";
            case STATUS_UNKNOWN_MESSAGE_TYPE -> "UNKNOWN_MESSAGE_TYPE";
            default -> {
                var str = checkStatus.apply( status );
                yield str != null ? str : "Unknown status: " + status;
            }
        };
    }

    public static Path lock( Path file, long storageLockExpiration ) {
        var lockFile = Paths.get( FilenameUtils.removeExtension( file.toString() ) + ".lock" );

        if( Files.createFile( lockFile ) ) return lockFile;
        if( storageLockExpiration <= 0 ) return null;

        log.trace( "lock found {}, expiration = {}", lockFile,
            Dates.durationToString( Files.getLastModifiedTime( lockFile ) + storageLockExpiration - DateTimeUtils.currentTimeMillis() ) );

        return Files.getLastModifiedTime( lockFile ) + storageLockExpiration < DateTimeUtils.currentTimeMillis() ? lockFile : null;
    }

    public final long getClientId() {
        return clientId;
    }

    public void start() {
        log.info( "message server host = {}, port = {}, storage = {}, storageLockExpiration = {}",
            host, port, directory, Dates.durationToString( storageLockExpiration ) );

        pool = new ThreadPoolExecutor( poolSize, poolSize,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat( "message-sender-%d" ).build()
        );

        states = new ConnectionState[poolSize];
        for( var i = 0; i < poolSize; i++ ) {
            states[i] = new ConnectionState();
        }
        poolSemaphore = new Semaphore( poolSize, true );
    }

    public CompletableFuture<MessageStatus> sendJson( byte messageType, Object data ) {
        return sendJson( messageType, data, s -> null );
    }

    public CompletableFuture<MessageStatus> sendJson( byte messageType, Object data, Function<Short, String> checkStatus ) {
        var baos = new ByteArrayOutputStream();
        Binder.json.marshal( baos, data );
        return sendObject( messageType, baos.toByteArray(), checkStatus );
    }

    public CompletableFuture<MessageStatus> sendObject( byte messageType, byte[] data ) {
        return sendObject( messageType, data, s -> null );
    }

    public synchronized CompletableFuture<MessageStatus> sendObject( byte messageType, byte[] data, Function<Short, String> checkStatus ) {
        assert data != null;

        var md5 = DigestUtils.getMd5Digest().digest( data );
        var message = new Message( clientId, messageType, ByteSequence.of( md5 ), data );
        messages.put( message.md5, message );

        try {
            poolSemaphore.acquire();

            var state = findFreeState();

            return CompletableFuture.supplyAsync( () -> {
                try {
                    return state.sendMessage( message, checkStatus );
                } finally {
                    poolSemaphore.release();
                }
            } );
        } catch( InterruptedException e ) {
            poolSemaphore.release();

            return ( CompletableFuture<MessageStatus> ) CompletableFuture.<MessageStatus>failedStage( e );
        }
    }

    private ConnectionState findFreeState() {
        for( var state : states ) {
            if( state.free.compareAndExchange( true, false ) ) return state;
        }
        throw new IllegalStateException( "no free states" );
    }


    @Override
    public synchronized void close() {
        closed = true;

        pool.shutdownNow();

        for( var state : states )
            Closeables.close( state );

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

    private boolean loggingAvailable() {
        for( var state : states ) {
            if( state.loggingAvailable ) return true;
        }

        return false;
    }

    public MessageAvailabilityReport availabilityReport() {
        boolean operational = loggingAvailable() && !closed;
        return new MessageAvailabilityReport( operational ? OPERATIONAL : FAILED );
    }

    @Override
    public void run() {
        var messageFiles = Files.fastWildcard( directory, "*/*/*.bin" );

        for( var msgFile : messageFiles ) {
            Path lockFile;

            if( ( lockFile = lock( msgFile, storageLockExpiration ) ) != null ) {
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

                    log.debug( "client id = {}, message type = {}, md5 = {}", msgClientId, messageType, md5Hex );

                    var msg = new Message( clientId, messageType, md5, data );

                    poolSemaphore.acquire();
                    try {
                        var state = findFreeState();
                        try {

                            if( state._sendMessage( msg, s -> null ) != ERROR ) {
                                Files.delete( msgFile );
                                Files.delete( lockFile );
                            }
                        } finally {
                            state.free.set( true );
                        }
                    } finally {
                        poolSemaphore.release();
                    }
                } catch( Exception e ) {
                    LogConsolidated.log( log, Level.ERROR, Dates.s( 5 ), msgFile + ": " + e.getMessage(), e );
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

    private class ConnectionState implements Closeable {
        public MessageSocketConnection connection;
        public AtomicBoolean free = new AtomicBoolean( true );
        public boolean loggingAvailable = true;

        private MessageStatus _sendMessage( Message message, Function<Short, String> checkStatus ) throws IOException {
            if( !closed ) {
                try {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "trysend" ).increment();

                    refreshConnection();

                    loggingAvailable = true;

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

                    log.trace( "sending done, server status: {}", getServerStatus( status, checkStatus ) );

                    switch( status ) {
                        case STATUS_ALREADY_WRITTEN -> {
                            log.trace( "already written {}", message.getHexMd5() );
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "already_written" ).increment();
                            return ALREADY_WRITTEN;
                        }
                        case STATUS_OK -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "success" ).increment();
                            return OK;
                        }
                        case STATUS_UNKNOWN_ERROR -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error" ).increment();
                            log.error( "unknown error" );
                            return ERROR;
                        }
                        case STATUS_UNKNOWN_MESSAGE_TYPE -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "unknown_message_type" ).increment();
                            log.error( "unknown message type: {}", status );
                            return ERROR;
                        }
                        default -> {
                            var clientStatus = checkStatus.apply( status );
                            if( clientStatus != null ) {
                                Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "status_" + status + "(" + clientStatus + ")" ).increment();
                            } else {
                                Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "unknown_status" ).increment();
                                log.error( "unknown status: {}", status );
                            }
                            return ERROR;
                        }
                    }
                } catch( IOException e ) {
                    loggingAvailable = false;

                    Closeables.close( connection );

                    throw e;
                } catch( UncheckedIOException e ) {
                    loggingAvailable = false;

                    Closeables.close( connection );

                    throw e.getCause();
                } catch( Exception e ) {
                    loggingAvailable = false;

                    LogConsolidated.log( log, Level.WARN, Dates.s( 5 ), e.getMessage(), null );
                    log.trace( e.getMessage(), e );
                    Closeables.close( connection );
                }
            }

            if( !loggingAvailable ) log.debug( "logging unavailable" );

            return ERROR;
        }

        public MessageStatus sendMessage( Message message, Function<Short, String> checkStatus ) {
            try {
                while( !closed ) {
                    try {
                        var status = _sendMessage( message, checkStatus );
                        if( status != ERROR ) {
                            messages.remove( message.md5 );
                            return status;
                        }

                        Thread.sleep( retryAfter );
                        log.debug( "retrying [type = {}]", message.messageType );
                    } catch( InterruptedException e ) {
                        log.info( e.getMessage() );
                        break;
                    } catch( Exception e ) {
                        Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error" ).increment();
                        log.debug( e.getMessage() );
                        log.trace( e.getMessage(), e );
                        try {
                            log.debug( "sleep {}...", Dates.durationToString( retryAfter ) );
                            Thread.sleep( retryAfter );
                            log.debug( "retrying..." );
                        } catch( InterruptedException interruptedException ) {
                            log.info( e.getMessage() );
                            break;
                        }
                    }
                }
            } finally {
                free.set( true );
            }

            return ERROR;
        }

        private void refreshConnection() {
            if( this.connection == null || !connection.isConnected() ) {
                Closeables.close( connection );
                log.debug( "opening connection..." );
                this.connection = new MessageSocketConnection( host, port, timeout, connectionTimeout );
                log.debug( "connected!" );
            }
        }

        @Override
        public void close() {
            Closeables.close( connection );
        }
    }
}
