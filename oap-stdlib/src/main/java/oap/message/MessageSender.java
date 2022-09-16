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
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.application.ServiceName;
import oap.concurrent.Executors;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.util.ByteSequence;
import oap.util.Cuid;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import oap.util.Pair;
import oap.util.Throwables;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTimeUtils;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR_NO_RETRY;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;
import static oap.message.MessageProtocol.messageStatusToString;
import static oap.message.MessageProtocol.messageTypeToString;
import static oap.message.MessageStatus.ALREADY_WRITTEN;
import static oap.message.MessageStatus.ERROR;
import static oap.message.MessageStatus.OK;
import static oap.util.Dates.durationToString;
import static oap.util.Pair.__;

@Slf4j
@ToString
public class MessageSender implements Closeable, AutoCloseable {
    private static final Pair<MessageStatus, Short> STATUS_OK = __( OK, MessageProtocol.STATUS_OK );

    private final Object syncDiskLock = new Object();
    private final String host;
    private final int port;
    private final Path directory;
    private final MessageNoRetryStrategy messageNoRetryStrategy;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final Messages messages = new Messages();
    private final ConcurrentMap<Byte, Pair<MessageStatus, Short>> lastStatus = new ConcurrentHashMap<>();
    private final String messageUrl;
    private final long memorySyncPeriod;
    @ServiceName
    public String name = "oap-http-message-sender";
    public long storageLockExpiration = Dates.h( 1 );
    public int poolSize = 4;
    public long diskSyncPeriod = Dates.m( 1 );
    public long globalIoRetryTimeout = Dates.s( 1 );
    public long retryTimeout = Dates.s( 1 );
    public long keepAliveDuration = Dates.d( 30 );
    protected long timeout = Dates.s( 5 );
    protected long connectionTimeout = Dates.s( 30 );
    private volatile boolean closed = false;
    private Scheduled diskSyncScheduler;
    private boolean networkAvailable = true;
    private OkHttpClient httpClient;
    private ExecutorService executor;
    private long ioExceptionStartRetryTimeout = -1;

    public MessageSender( String host, int port, String httpPrefix, Path persistenceDirectory, long memorySyncPeriod ) {
        this( host, port, httpPrefix, persistenceDirectory, memorySyncPeriod, MessageNoRetryStrategy.DROP );
    }

    public MessageSender( String host, int port, String httpPrefix, Path persistenceDirectory, long memorySyncPeriod, MessageNoRetryStrategy messageNoRetryStrategy ) {
        this.host = host;
        this.port = port;
        this.memorySyncPeriod = memorySyncPeriod;
        messageUrl = "http://" + host + ":" + port + httpPrefix;
        this.directory = persistenceDirectory;
        this.messageNoRetryStrategy = messageNoRetryStrategy;

        Metrics.gaugeCollectionSize( "message_count", Tags.of( "host", host, "type", "ready", "port", String.valueOf( port ) ), messages.ready );
        Metrics.gaugeCollectionSize( "message_count", Tags.of( "host", host, "type", "retry", "port", String.valueOf( port ) ), messages.retry );
        Metrics.gaugeMapSize( "message_count", Tags.of( "host", host, "type", "inprogress", "port", String.valueOf( port ) ), messages.inProgress );
    }

    public static Path lock( Path file, long storageLockExpiration ) {
        var lockFile = Paths.get( FilenameUtils.removeExtension( file.toString() ) + ".lock" );

        if( Files.createFile( lockFile ) ) return lockFile;
        if( storageLockExpiration <= 0 ) return null;

        log.trace( "lock found {}, expiration = {}", lockFile,
            durationToString( Files.getLastModifiedTime( lockFile ) + storageLockExpiration - DateTimeUtils.currentTimeMillis() ) );

        return Files.getLastModifiedTime( lockFile ) + storageLockExpiration < DateTimeUtils.currentTimeMillis() ? lockFile : null;
    }

    public final long getClientId() {
        return clientId;
    }

    public void start() {
        log.info( "[{}] message server messageUrl {} storage {} storageLockExpiration {}",
            name, messageUrl, directory, durationToString( storageLockExpiration ) );
        log.info( "[{}] connection timeout {} rw timeout {} pool size {} keepAliveDuration {}",
            name, Dates.durationToString( connectionTimeout ), Dates.durationToString( timeout ), poolSize,
            Dates.durationToString( keepAliveDuration ) );
        log.info( "[{}] retry timeout {} disk sync period '{}' memory sync period '{}'",
            name, durationToString( retryTimeout ), durationToString( diskSyncPeriod ), durationToString( memorySyncPeriod ) );
        log.info( "custom status = {}", MessageProtocol.printMapping() );

        executor = Executors.newFixedBlockingThreadPool(
            poolSize > 0 ? poolSize : Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat( name + "-%d" ).build() );

        Dispatcher dispatcher = new Dispatcher();
        if( poolSize > 0 )
            dispatcher.setMaxRequestsPerHost( poolSize );

        ConnectionPool connectionPool = poolSize > 0
            ? new ConnectionPool( poolSize, keepAliveDuration, TimeUnit.MILLISECONDS )
            : new ConnectionPool();

        httpClient = new OkHttpClient.Builder()
            .connectTimeout( connectionTimeout, TimeUnit.MILLISECONDS )
            .readTimeout( timeout, TimeUnit.MILLISECONDS )
            .writeTimeout( timeout, TimeUnit.MILLISECONDS )
            .dispatcher( dispatcher )
            .connectionPool( connectionPool )
            .build();

        if( diskSyncPeriod > 0 )
            diskSyncScheduler = Scheduler.scheduleWithFixedDelay( diskSyncPeriod, TimeUnit.MILLISECONDS, this::syncDisk );

        if( memorySyncPeriod > 0 )
            diskSyncScheduler = Scheduler.scheduleWithFixedDelay( memorySyncPeriod, TimeUnit.MILLISECONDS, this::syncMemory );
        log.info( "[{}] message server started", name );
    }

    public <T> MessageSender send( byte messageType, T data, ContentWriter<T> writer ) {
        byte[] bytes = writer.write( data );
        return send( messageType, bytes, 0, bytes.length );
    }

    public MessageSender send( byte messageType, byte[] data, int offset, int length ) {
        Preconditions.checkNotNull( data );
        Preconditions.checkArgument( ( messageType & 0xFF ) <= 200, "reserved" );

        var md5 = DigestUtils.getMd5Digest().digest( data );
        var message = new Message( clientId, messageType, ByteSequence.of( md5 ), data, offset, length );
        messages.add( message );

        return this;
    }


    @Override
    public void close() {
        synchronized( syncDiskLock ) {
            closed = true;

            Closeables.close( diskSyncScheduler );
        }

        int count = 0;
        while( count < 100 && !messages.inProgress.isEmpty() ) {
            try {
                Thread.sleep( 100 );
                count++;
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();

        saveMessagesToDirectory( directory );
    }

    private void saveMessagesToDirectory( Path directory ) {
        while( true ) {
            var messageInfo = messages.poll( false );
            if( messageInfo == null ) {
                messageInfo = messages.pollInProgress();
            }
            if( messageInfo == null ) {
                Messages.RetryInfo retryInfo = messages.pollRetry();
                messageInfo = retryInfo != null ? retryInfo.messageInfo : null;
            }

            if( messageInfo == null ) break;
            Message message = messageInfo.message;

            var parentDirectory = directory
                .resolve( Long.toHexString( clientId ) )
                .resolve( String.valueOf( Byte.toUnsignedInt( message.messageType ) ) );
            var tmpMsgPath = parentDirectory.resolve( message.getHexMd5() + ".bin.tmp" );
            log.debug( "[{}] writing unsent message to {}", name, tmpMsgPath );
            try {
                Files.write( tmpMsgPath, message.data, ContentWriter.ofBytes() );
                var msgPath = parentDirectory.resolve( message.getHexMd5() + ".bin" );
                Files.rename( tmpMsgPath, msgPath );
            } catch( Exception e ) {
                log.error( "[{}] type: {}, md5: {}, data: {}",
                    name, message.messageType, message.getHexMd5(), message.getHexData(), e );
            }
        }
    }

    public MessageAvailabilityReport availabilityReport( byte messageType ) {
        var operational = networkAvailable
            && !closed
            && lastStatus.getOrDefault( messageType, STATUS_OK )._1 != ERROR;
        return new MessageAvailabilityReport( operational ? OPERATIONAL : FAILED );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public CompletableFuture<Messages.MessageInfo> send( Messages.MessageInfo messageInfo, long now ) {
        Message message = messageInfo.message;

        log.debug( "[{}] sending data [type = {}] to server...", name, messageTypeToString( message.messageType ) );

        return CompletableFuture.supplyAsync( () -> {
            Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "trysend" ).increment();

            try( FastByteArrayOutputStream buf = new FastByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream( buf ) ) {
                out.writeByte( message.messageType );
                out.writeShort( PROTOCOL_VERSION_1 );
                out.writeLong( message.clientId );

                out.write( message.md5.bytes );

                out.write( MessageProtocol.RESERVED, 0, MessageProtocol.RESERVED_LENGTH );
                out.writeInt( message.data.length );
                out.write( message.data );

                RequestBody requestBody = RequestBody.create( buf.array, null, 0, buf.length );

                Request request = new Request.Builder()
                    .url( messageUrl )
                    .post( requestBody )
                    .build();
                Call call = httpClient.newCall( request );
                var response = call.execute();
                if ( response.code() >= 300 || response.code() < 200 ) {
                    throw new IOException( "Not OK (" + response.code() + ") response code returned for url: " + messageUrl );
                }
                return onOkRespone( messageInfo, response, now );

            } catch( UnknownHostException e ) {
                processException( messageInfo, now, message, e, true );

                ioExceptionStartRetryTimeout = now;

                throw Throwables.propagate( e );
            } catch( Throwable e ) {
                processException( messageInfo, now, message, e, false );

                throw Throwables.propagate( e );
            }
        }, executor );
    }

    private void processException( Messages.MessageInfo messageInfo, long now, Message message, Throwable e, boolean globalRetryTimeout ) {
        Metrics.counter( "oap.messages",
            "type", messageTypeToString( message.messageType ),
            "status", "send_io_error" + ( globalRetryTimeout ? "_gr" : "" ) ).increment();
        LogConsolidated.log( log, Level.ERROR, Dates.s( 10 ), e.getMessage(), e );
        messages.retry( messageInfo, now + retryTimeout );
    }

    private Messages.MessageInfo onOkRespone( Messages.MessageInfo messageInfo, Response response, long now ) {
        Message message = messageInfo.message;

        ResponseBody body = response.body();
        if( body == null ) {
            Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "io_error" ).increment();
            log.error( "[{}] unknown error (BODY == null)", name );
            lastStatus.put( message.messageType, __( ERROR, ( short ) -1 ) );
            messages.retry( messageInfo, now + retryTimeout );
            return messageInfo;
        }

        try( var in = new DataInputStream( body.byteStream() ) ) {
            var version = in.readByte();
            if( version != PROTOCOL_VERSION_1 ) {
                log.error( "[{}] Version mismatch, expected: {}, received: {}", name, PROTOCOL_VERSION_1, version );
                throw new MessageException( "Version mismatch" );
            }
            in.readLong(); // clientId
            in.skipNBytes( MessageProtocol.MD5_LENGTH ); // digestionId
            in.skipNBytes( MessageProtocol.RESERVED_LENGTH );
            var status = in.readShort();

            log.trace( "[{}] sending done, server status: {}", name, messageStatusToString( status ) );

            MessageSender.this.networkAvailable = true;

            switch( status ) {
                case STATUS_ALREADY_WRITTEN -> {
                    log.trace( "[{}] already written {}", name, message.getHexMd5() );
                    Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "already_written" ).increment();
                    lastStatus.put( message.messageType, __( ALREADY_WRITTEN, status ) );
                }
                case MessageProtocol.STATUS_OK -> {
                    Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "success" ).increment();
                    lastStatus.put( message.messageType, __( OK, status ) );
                }
                case STATUS_UNKNOWN_ERROR -> {
                    Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "error" ).increment();
                    log.error( "[{}] unknown error", name );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                    messages.retry( messageInfo, now + retryTimeout );
                }
                case STATUS_UNKNOWN_ERROR_NO_RETRY -> {
                    Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "error_no_retry" ).increment();
                    log.error( "[{}] unknown error -> no retry", name );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                }
                case STATUS_UNKNOWN_MESSAGE_TYPE -> {
                    Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "unknown_message_type" ).increment();
                    log.error( "[{}] unknown message type: {}", name, status );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                }
                default -> {
                    var clientStatus = MessageProtocol.getStatus( status );
                    if( clientStatus != null ) {
                        log.trace( "[{}] retry: {}", name, clientStatus );
                        Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "status_" + status + "(" + clientStatus + ")" ).increment();
                    } else {
                        Metrics.counter( "oap.messages", "type", messageTypeToString( message.messageType ), "status", "unknown_status" ).increment();
                        log.error( "[{}] unknown status: {}", name, status );
                    }
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                    messages.retry( messageInfo, now + retryTimeout );
                }
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return messageInfo;
    }

    public void syncMemory() {
        log.trace( "[{}] sync ready {} retry {} inprogress {} ...",
            name, getReadyMessages(), getRetryMessages(), getInProgressMessages() );

        long now = DateTimeUtils.currentTimeMillis();

        if( isGlobalIoRetryTimeout( now ) ) return;

        long period = currentPeriod( now );

        Messages.MessageInfo messageInfo = null;

        messages.retry();

        do {
            now = DateTimeUtils.currentTimeMillis();

            if( messageInfo != null ) {
                log.trace( "[{}] message {}...", name, messageInfo.message.md5 );
                var future = send( messageInfo, now );
                future.handle( ( mi, e ) -> {
                    messages.removeInProgress( mi );
                    log.trace( "[{}] message {}... done", name, mi.message.md5 );
                    return null;
                } );
            }

            if( isGlobalIoRetryTimeout( now ) ) {
                break;
            }

            var currentPeriod = currentPeriod( now );
            if( currentPeriod != period ) {
                messages.retry();
                period = currentPeriod;
            }

            messageInfo = messages.poll( true );
        } while( messageInfo != null );
    }

    private boolean isGlobalIoRetryTimeout( long now ) {
        return globalIoRetryTimeout > 0 && ioExceptionStartRetryTimeout + globalIoRetryTimeout > now;
    }

    private long currentPeriod( long time ) {
        return time / retryTimeout;
    }

    @SneakyThrows
    public MessageSender syncDisk() {
        if( closed ) return this;

        try( DirectoryStream<Path> clientIdStream = java.nio.file.Files.newDirectoryStream( directory ) ) {
            for( var clientIdPath : clientIdStream ) {
                if( !isValidClientId( clientIdPath ) ) {
                    log.warn( "invalid client id {}", clientIdPath );
                    Files.deleteSafely( clientIdPath );
                    continue;
                }

                var msgClientId = Long.parseLong( FilenameUtils.getName( clientIdPath.toString() ), 16 );
                try( DirectoryStream<Path> messageTypeStream = java.nio.file.Files.newDirectoryStream( clientIdPath ) ) {
                    for( var messageTypePath : messageTypeStream ) {
                        if( !isValidMessageType( messageTypePath ) ) {
                            log.warn( "invalid message type {}", messageTypePath );
                            Files.deleteSafely( messageTypePath );
                            continue;
                        }

                        var messageType = ( byte ) Integer.parseInt( FilenameUtils.getName( messageTypePath.toString() ) );

                        try( DirectoryStream<Path> messageStream = java.nio.file.Files.newDirectoryStream( messageTypePath ) ) {
                            for( var messagePath : messageStream ) {
                                if( !isValidMessage( messagePath ) ) {
                                    log.warn( "invalid message {}", messagePath );
                                    Files.deleteSafely( messagePath );
                                    continue;
                                }

                                if( messagePath.toString().endsWith( ".lock" ) ) {
                                    var binFile = Paths.get( FilenameUtils.removeExtension( messagePath.toString() ) + ".bin" );
                                    if( !java.nio.file.Files.exists( binFile ) ) {
                                        log.warn( "invalid lock file {}", messagePath );
                                        Files.deleteSafely( messagePath );
                                    }

                                    continue;
                                }

                                String md5Hex = FilenameUtils.removeExtension( FilenameUtils.getName( messagePath.toString() ) );
                                var md5 = ByteSequence.of( Hex.decodeHex( md5Hex ) );

                                Path lockFile;

                                synchronized( syncDiskLock ) {
                                    if( closed ) return this;

                                    if( ( lockFile = lock( messagePath, storageLockExpiration ) ) != null ) {
                                        log.info( "[{}] reading unsent message {}", name, messagePath );
                                        try {
                                            var data = Files.read( messagePath, ContentReader.ofBytes() );

                                            log.info( "[{}] client id {} message type {} md5 {}",
                                                name, msgClientId, messageTypeToString( messageType ), md5Hex );

                                            var message = new Message( clientId, messageType, md5, data );
                                            messages.add( message );

                                            Files.delete( messagePath );
                                        } catch( Exception e ) {
                                            LogConsolidated.log( log, Level.ERROR, Dates.s( 5 ), "[" + name + "] " + messagePath + ": " + e.getMessage(), e );
                                        } finally {
                                            Files.delete( lockFile );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Files.deleteEmptyDirectories( directory, false );

        return this;
    }

    private boolean isValidMessage( Path messagePath ) {
        try {
            return !java.nio.file.Files.isDirectory( messagePath )
                && ( messagePath.toString().endsWith( ".bin" ) || messagePath.toString().endsWith( ".lock" ) )
                && ByteSequence.of( Hex.decodeHex( FilenameUtils.removeExtension( FilenameUtils.getName( messagePath.toString() ) ) ) ) != null;
        } catch( DecoderException e ) {
            return false;
        }
    }

    private boolean isValidMessageType( Path messageTypePath ) {
        try {
            return java.nio.file.Files.isDirectory( messageTypePath ) && ( byte ) Integer.parseInt( FilenameUtils.getName( messageTypePath.toString() ) ) > 0;
        } catch( NumberFormatException e ) {
            return false;
        }
    }

    private boolean isValidClientId( Path clientIdPath ) {
        try {
            return java.nio.file.Files.isDirectory( clientIdPath ) && Long.parseLong( FilenameUtils.getName( clientIdPath.toString() ), 16 ) > 0;
        } catch( NumberFormatException e ) {
            return false;
        }
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int getReadyMessages() {
        return messages.getReadyMessages();
    }

    public int getRetryMessages() {
        return messages.getRetryMessages();
    }

    public int getInProgressMessages() {
        return messages.getInProgressMessages();
    }

    public void reset() {
        messages.reset();
    }
}
