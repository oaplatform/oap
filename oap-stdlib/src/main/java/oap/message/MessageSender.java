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

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.concurrent.ThreadPoolExecutor;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.Resources;
import oap.json.Binder;
import oap.util.ByteSequence;
import oap.util.Cuid;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import oap.util.Pair;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.github.jamm.MemoryMeter;
import org.joda.time.DateTimeUtils;
import org.slf4j.event.Level;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageProtocol.EOF_MESSAGE_TYPE;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;
import static oap.message.MessageStatus.ALREADY_WRITTEN;
import static oap.message.MessageStatus.ERROR;
import static oap.message.MessageStatus.OK;
import static oap.util.Dates.durationToString;
import static oap.util.Pair.__;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

@Slf4j
@ToString
public class MessageSender implements Closeable {
    private static final MemoryMeter MEMORY_METER = new MemoryMeter()
        .withGuessing( MemoryMeter.Guess.ALWAYS_SPEC )
        .omitSharedBufferOverhead()
        .ignoreOuterClassReference();
    private static final HashMap<Short, String> statusMap = new HashMap<>();
    private static final Pair<MessageStatus, Short> STATUS_OK = __( OK, MessageProtocol.STATUS_OK );

    static {
        var properties = Resources.readAllProperties( "META-INF/oap-messages.properties" );
        for( var propertyName : properties.stringPropertyNames() ) {
            var key = propertyName.trim();
            if( key.startsWith( "map." ) ) key = key.substring( 4 );

            statusMap.put( Short.parseShort( properties.getProperty( propertyName ) ), key );
        }
    }

    private final String host;
    private final int port;
    private final Path directory;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final Messages messages = new Messages();
    private final ConcurrentHashMap<Byte, Pair<MessageStatus, Short>> lastStatus = new ConcurrentHashMap<>();
    public long storageLockExpiration = Dates.h( 1 );
    public int poolSize = 4;
    public long messagesLimitBytes = 1024 * 1024 * 128; // 128Mb
    public long memorySyncPeriod = 100;
    public long diskSyncPeriod = Dates.m( 1 );
    protected long timeout = 5000;
    protected long connectionTimeout = Dates.s( 30 );
    private ThreadPoolExecutor pool;
    private ConnectionState[] connections;
    private boolean closed = false;
    private Semaphore poolSemaphore;
    private Scheduled diskSyncScheduler;
    private Scheduled memorySyncScheduler;

    public MessageSender( String host, int port, Path directory ) {
        this.host = host;
        this.port = port;
        this.directory = directory;

        Metrics.gauge( "message_memory_current_bytes", Tags.of( "host", host, "port", String.valueOf( port ) ), this, MessageSender::getMessagesMemorySize );
        Metrics.gauge( "message_memory_limit_bytes", Tags.of( "host", host, "port", String.valueOf( port ) ), this, ms -> ms.messagesLimitBytes );
        Metrics.gaugeMapSize( "message_memory_count", Tags.of( "host", host, "port", String.valueOf( port ) ), messages.map );
    }

    private static String getServerStatus( short status ) {
        return switch( status ) {
            case MessageProtocol.STATUS_OK -> "OK";
            case STATUS_UNKNOWN_ERROR -> "UNKNOWN_ERROR";
            case STATUS_ALREADY_WRITTEN -> "ALREADY_WRITTEN";
            case STATUS_UNKNOWN_MESSAGE_TYPE -> "UNKNOWN_MESSAGE_TYPE";
            default -> {
                var str = statusMap.get( status );
                yield str != null ? str : "Unknown status: " + status;
            }
        };
    }

    public static Path lock( Path file, long storageLockExpiration ) {
        var lockFile = Paths.get( FilenameUtils.removeExtension( file.toString() ) + ".lock" );

        if( Files.createFile( lockFile ) ) return lockFile;
        if( storageLockExpiration <= 0 ) return null;

        log.trace( "lock found {}, expiration = {}", lockFile,
            durationToString( Files.getLastModifiedTime( lockFile ) + storageLockExpiration - DateTimeUtils.currentTimeMillis() ) );

        return Files.getLastModifiedTime( lockFile ) + storageLockExpiration < DateTimeUtils.currentTimeMillis() ? lockFile : null;
    }

    public long getMessagesMemorySize() {
        return messages.size.get();
    }

    public final long getClientId() {
        return clientId;
    }

    public void start() {
        log.info( "message server host = {}, port = {}, storage = {}, storageLockExpiration = {}",
            host, port, directory, durationToString( storageLockExpiration ) );
        log.info( "memory sync period = {}, disk sync period = {}",
            durationToString( memorySyncPeriod ), durationToString( diskSyncPeriod ) );
        log.info( "custom status = {}", statusMap );

        pool = new ThreadPoolExecutor( poolSize, poolSize,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat( "message-sender-%d" ).build()
        );

        connections = new ConnectionState[poolSize];
        for( var i = 0; i < poolSize; i++ ) {
            connections[i] = new ConnectionState();
        }
        poolSemaphore = new Semaphore( poolSize, true );

        if( diskSyncPeriod > 0 )
            diskSyncScheduler = Scheduler.scheduleWithFixedDelay( diskSyncPeriod, TimeUnit.MILLISECONDS, this::syncDisk );
        if( memorySyncPeriod > 0 )
            memorySyncScheduler = Scheduler.scheduleWithFixedDelay( memorySyncPeriod, TimeUnit.MILLISECONDS, this::syncMemory );
    }

    public MessageSender sendJson( byte messageType, Object data ) {
        var baos = new FastByteArrayOutputStream();
        Binder.json.marshal( baos, data );
        return sendObject( messageType, baos.array, 0, baos.length );
    }

    public synchronized MessageSender sendObject( byte messageType, byte[] data, int from, int length ) {
        Preconditions.checkNotNull( data );
        Preconditions.checkArgument( ( messageType & 0xFF ) <= 200, "reserved" );

        var md5 = DigestUtils.getMd5Digest().digest( data );
        var message = new Message( clientId, messageType, ByteSequence.of( md5 ), data, from, length );
        messages.put( message.md5, message );

        if( !memoryAvailable() ) {
            log.warn( "Message overhead warning. Limit: {}, Current: ~{}",
                byteCountToDisplaySize( messagesLimitBytes ), byteCountToDisplaySize( getMessagesMemorySize() ) );
        }

        return this;
    }

    private ConnectionState findFreeState() {
        for( var state : connections ) {
            if( state.free.compareAndExchange( true, false ) ) return state;
        }
        throw new IllegalStateException( "no free states" );
    }


    @Override
    public synchronized void close() {
        closed = true;

        pool.shutdownNow();

        for( var state : connections )
            Closeables.close( state );

        Closeables.close( memorySyncScheduler );
        Closeables.close( diskSyncScheduler );

        saveMessagesToDirectory( directory );
    }

    private void saveMessagesToDirectory( Path directory ) {
        while( !messages.isEmpty() ) for( Message msg : messages ) {
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

            messages.remove( msg.md5 );
        }
    }

    private boolean memoryAvailable() {
        return messages.size.get() < messagesLimitBytes;
    }

    private boolean networkAvailable() {
        for( var state : connections ) {
            if( state.networkAvailable ) return true;
        }

        return false;
    }

    public MessageAvailabilityReport availabilityReport( byte messageType ) {
        var operational = memoryAvailable()
            && networkAvailable()
            && !closed
            && lastStatus.getOrDefault( messageType, STATUS_OK )._1 != ERROR;
        return new MessageAvailabilityReport( operational ? OPERATIONAL : FAILED );
    }

    public synchronized MessageSender syncMemory() {
        log.trace( "sync..." );
        if( closed ) return this;

        for( var message : messages ) {
            log.trace( "msg type = {}", ( message.messageType & 0xFF ) );
            try {
                poolSemaphore.acquire();

                try {
                    var state = findFreeState();

                    var status = state.sendMessage( message );
                    log.trace( "response status = {}", status );
                } finally {
                    poolSemaphore.release();
                }
            } catch( InterruptedException e ) {
                LogConsolidated.log( log, Level.WARN, Dates.s( 5 ), e.getMessage(), null );
            }
        }

        return this;
    }

    public synchronized void syncDisk() {
        if( closed ) return;

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

                            if( state.send( msg ) != ERROR ) {
                                Files.delete( msgFile );
                            }
                            Files.delete( lockFile );
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

        public Message( long clientId, byte messageType, ByteSequence md5, byte[] data, int from, int length ) {
            this( clientId, messageType, md5, Arrays.copyOfRange( data, from, from + length ) );
        }

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

        public long getMemorySize() {
            return MessageSender.MEMORY_METER.measureDeep( this );
        }
    }

    private static class Messages implements Iterable<Message> {
        private static final long ENTRY_SIZE = MessageSender.MEMORY_METER.measureDeep( new Pair<>( ByteSequence.of( new byte[0] ), null ) );
        private final ConcurrentHashMap<ByteSequence, Message> map = new ConcurrentHashMap<>();

        private final AtomicLong size = new AtomicLong();

        public void put( ByteSequence md5, Message message ) {
            map.compute( md5, ( key, oldValue ) -> {
                size.addAndGet( message.getMemorySize() + ENTRY_SIZE );
                if( oldValue != null ) size.addAndGet( -oldValue.getMemorySize() - ENTRY_SIZE );
                return message;
            } );
        }

        public void remove( ByteSequence md5 ) {
            var res = map.remove( md5 );
            if( res != null ) size.addAndGet( -res.getMemorySize() - ENTRY_SIZE );
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        @Nonnull
        public Iterator<Message> iterator() {
            var it = map.values().iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Message next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    throw new IllegalStateException();
                }
            };
        }
    }

    private class ConnectionState implements Closeable {
        public MessageSocketConnection connection;
        public AtomicBoolean free = new AtomicBoolean( true );
        public boolean networkAvailable = true;

        private MessageStatus send( Message message ) throws IOException {
            if( !closed ) {
                try {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "trysend" ).increment();

                    refreshConnection();

                    networkAvailable = true;

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

                    log.trace( "sending done, server status: {}", getServerStatus( status ) );

                    switch( status ) {
                        case STATUS_ALREADY_WRITTEN -> {
                            log.trace( "already written {}", message.getHexMd5() );
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "already_written" ).increment();
                            lastStatus.put( message.messageType, __( ALREADY_WRITTEN, status ) );
                            return ALREADY_WRITTEN;
                        }
                        case MessageProtocol.STATUS_OK -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "success" ).increment();
                            lastStatus.put( message.messageType, __( OK, status ) );
                            return OK;
                        }
                        case STATUS_UNKNOWN_ERROR -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error" ).increment();
                            log.error( "unknown error" );
                            lastStatus.put( message.messageType, __( ERROR, status ) );
                            return ERROR;
                        }
                        case STATUS_UNKNOWN_MESSAGE_TYPE -> {
                            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "unknown_message_type" ).increment();
                            log.error( "unknown message type: {}", status );
                            lastStatus.put( message.messageType, __( ERROR, status ) );
                            return ERROR;
                        }
                        default -> {
                            var clientStatus = statusMap.get( status );
                            if( clientStatus != null ) {
                                Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "status_" + status + "(" + clientStatus + ")" ).increment();
                            } else {
                                Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "unknown_status" ).increment();
                                log.error( "unknown status: {}", status );
                            }
                            lastStatus.put( message.messageType, __( ERROR, status ) );
                            return ERROR;
                        }
                    }
                } catch( IOException e ) {
                    networkAvailable = false;

                    Closeables.close( connection );

                    throw e;
                } catch( UncheckedIOException e ) {
                    networkAvailable = false;

                    Closeables.close( connection );

                    throw e.getCause();
                } catch( Exception e ) {
                    networkAvailable = false;

                    LogConsolidated.log( log, Level.WARN, Dates.s( 5 ), e.getMessage(), null );
                    log.trace( e.getMessage(), e );
                    Closeables.close( connection );
                }
            }

            if( !networkAvailable ) log.debug( "logging unavailable" );

            return ERROR;
        }

        public MessageStatus sendMessage( Message message ) {
            try {
                if( closed ) return ERROR;

                try {
                    var status = send( message );
                    if( status != ERROR ) messages.remove( message.md5 );

                    return status;
                } catch( Exception e ) {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error" ).increment();
                    LogConsolidated.log( log, Level.TRACE, Dates.s( 5 ), e.getMessage(), e );
                    return ERROR;
                }
            } finally {
                free.set( true );
            }
        }

        private void refreshConnection() {
            if( this.connection == null || !connection.isConnected() ) {
                Closeables.close( connection );
                log.debug( "opening connection..." );
                this.connection = new MessageSocketConnection( host, port, timeout, connectionTimeout );
                log.debug( "connected! {}", this.connection );
            }
        }

        @Override
        public void close() {
            try {
                if( connection != null ) {
                    log.info( "close {}", connection );
                    connection.out.writeByte( EOF_MESSAGE_TYPE );
                    Closeables.close( connection );
                }
            } catch( IOException ignored ) {
            }
        }
    }
}
