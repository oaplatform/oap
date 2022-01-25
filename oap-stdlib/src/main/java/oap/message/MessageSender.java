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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.http.client.CallbackFuture;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.util.ByteSequence;
import oap.util.Cuid;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import oap.util.Pair;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageProtocol.PROTOCOL_VERSION_1;
import static oap.message.MessageProtocol.STATUS_ALREADY_WRITTEN;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_ERROR_NO_RETRY;
import static oap.message.MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE;
import static oap.message.MessageStatus.ALREADY_WRITTEN;
import static oap.message.MessageStatus.ERROR;
import static oap.message.MessageStatus.OK;
import static oap.util.Dates.durationToString;
import static oap.util.Pair.__;

@Slf4j
@ToString
public class MessageSender implements Closeable, Runnable {
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

    private final Object syncMemoryLock = new Object();
    private final Object syncDiskLock = new Object();
    private final String host;
    private final int port;
    private final Path directory;
    private final MessageNoRetryStrategy messageNoRetryStrategy;
    private final long clientId = Cuid.UNIQUE.nextLong();
    private final Messages messages = new Messages();
    private final ConcurrentHashMap<Byte, Pair<MessageStatus, Short>> lastStatus = new ConcurrentHashMap<>();
    private final String messageUrl;
    public long storageLockExpiration = Dates.h( 1 );
    public int poolSize = 4;
    public long diskSyncPeriod = Dates.m( 1 );
    public String httpPrefix = "/messages";
    public long retryTimeout = Dates.s( 1 );
    protected long timeout = 5000;
    protected long connectionTimeout = Dates.s( 30 );
    private boolean closed = false;
    private Scheduled diskSyncScheduler;
    private boolean networkAvailable = true;
    private OkHttpClient httpClient;

    public MessageSender( String host, int port, Path directory ) {
        this( host, port, directory, MessageNoRetryStrategy.DROP );
    }

    public MessageSender( String host, int port, Path directory, MessageNoRetryStrategy messageNoRetryStrategy ) {
        this.host = host;
        this.port = port;
        messageUrl = "http://" + host + ":" + port + httpPrefix;
        this.directory = directory;
        this.messageNoRetryStrategy = messageNoRetryStrategy;

        Metrics.gaugeCollectionSize( "message_memory_count", Tags.of( "host", host, "port", String.valueOf( port ) ), messages.ready );
        Metrics.gaugeCollectionSize( "message_memory_retry_count", Tags.of( "host", host, "port", String.valueOf( port ) ), messages.retry );
    }

    private static String getServerStatus( short status ) {
        return switch( status ) {
            case MessageProtocol.STATUS_OK -> "OK";
            case STATUS_UNKNOWN_ERROR, STATUS_UNKNOWN_ERROR_NO_RETRY -> "UNKNOWN_ERROR";
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

    public final long getClientId() {
        return clientId;
    }

    public void start() {
        log.info( "message server messageUrl {} storage {} storageLockExpiration {}",
            messageUrl, directory, durationToString( storageLockExpiration ) );
        log.info( " connection timeout {} rw timeout {} pool size {}",
            Dates.durationToString( connectionTimeout ), Dates.durationToString( timeout ), poolSize );
        log.info( "retry timeout {} disk sync period {}",
            durationToString( retryTimeout ), durationToString( diskSyncPeriod ) );
        log.info( "custom status = {}", statusMap );

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost( poolSize );

        httpClient = new OkHttpClient.Builder()
            .connectTimeout( connectionTimeout, TimeUnit.MILLISECONDS )
            .readTimeout( timeout, TimeUnit.MILLISECONDS )
            .writeTimeout( timeout, TimeUnit.MILLISECONDS )
            .dispatcher( dispatcher )
            .build();

        if( diskSyncPeriod > 0 )
            diskSyncScheduler = Scheduler.scheduleWithFixedDelay( diskSyncPeriod, TimeUnit.MILLISECONDS, this::syncDisk );
    }

    @Deprecated
    public MessageSender sendJson( byte messageType, Object data ) {
        return send( messageType, data, ContentWriter.ofJson() );
    }

    @Deprecated
    public MessageSender sendObject( byte messageType, byte[] data, int from, int length ) {
        return send( messageType, data, from, length );
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
                break;
            }
        }

        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();

        saveMessagesToDirectory( directory );
    }

    private void saveMessagesToDirectory( Path directory ) {
        while( true ) {
            var messageInfo = messages.poll();
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
            log.debug( "writing unsent message to {}", tmpMsgPath );
            try {
                Files.write( tmpMsgPath, message.data, ContentWriter.ofBytes() );
                var msgPath = parentDirectory.resolve( message.getHexMd5() + ".bin" );
                Files.rename( tmpMsgPath, msgPath );
            } catch( Exception e ) {
                log.error( "type: {}, md5: {}, data: {}",
                    message.messageType, message.getHexMd5(), message.getHexData() );
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
        try {
            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "trysend" ).increment();

            log.debug( "sending data [type = {}] to server...", message.messageType );

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
                CallbackFuture callbackFuture = new CallbackFuture();
                call.enqueue( callbackFuture );

                return callbackFuture.future
                    .thenApply( response -> onOkRespone( messageInfo, response, now ) )
                    .exceptionally( throwable -> errorResponse( messageInfo, throwable, now ) );
            }
        } catch( IOException e ) {
            if( log.isTraceEnabled() ) log.trace( e.getMessage(), e );
            LogConsolidated.log( log, Level.ERROR, Dates.s( 10 ), e.getMessage(), e );
            messages.retry( messageInfo, now + retryTimeout );

            return CompletableFuture.completedFuture( messageInfo );
        }
    }

    private Messages.MessageInfo errorResponse( Messages.MessageInfo messageInfo, Throwable throwable, long now ) {
        LogConsolidated.log( log, Level.ERROR, Dates.s( 10 ), throwable.getMessage(), throwable );
        messages.retry( messageInfo, now + retryTimeout );
        return messageInfo;
    }

    private Messages.MessageInfo onOkRespone( Messages.MessageInfo messageInfo, Response response, long now ) {
        Message message = messageInfo.message;

        ResponseBody body = response.body();
        if( body == null ) {
            Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "io_error" ).increment();
            log.error( "unknown error (BODY == null)" );
            lastStatus.put( message.messageType, __( ERROR, ( short ) -1 ) );
            messages.retry( messageInfo, now + retryTimeout );
            return messageInfo;
        }

        try( var in = new DataInputStream( body.byteStream() ) ) {
            var version = in.readByte();
            if( version != PROTOCOL_VERSION_1 ) {
                log.error( "Version mismatch, expected: {}, received: {}", PROTOCOL_VERSION_1, version );
                throw new MessageException( "Version mismatch" );
            }
            in.readLong(); // clientId
            in.skipNBytes( MessageProtocol.MD5_LENGTH ); // digestionId
            in.skipNBytes( MessageProtocol.RESERVED_LENGTH );
            var status = in.readShort();

            log.trace( "sending done, server status: {}", getServerStatus( status ) );

            MessageSender.this.networkAvailable = true;

            switch( status ) {
                case STATUS_ALREADY_WRITTEN -> {
                    log.trace( "already written {}", message.getHexMd5() );
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "already_written" ).increment();
                    lastStatus.put( message.messageType, __( ALREADY_WRITTEN, status ) );
                }
                case MessageProtocol.STATUS_OK -> {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "success" ).increment();
                    lastStatus.put( message.messageType, __( OK, status ) );
                }
                case STATUS_UNKNOWN_ERROR -> {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error" ).increment();
                    log.error( "unknown error" );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                    messages.retry( messageInfo, retryTimeout );
                }
                case STATUS_UNKNOWN_ERROR_NO_RETRY -> {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "error_no_retry" ).increment();
                    log.error( "unknown error -> no retry" );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
                }
                case STATUS_UNKNOWN_MESSAGE_TYPE -> {
                    Metrics.counter( "oap.messages", "type", String.valueOf( message.messageType ), "status", "unknown_message_type" ).increment();
                    log.error( "unknown message type: {}", status );
                    lastStatus.put( message.messageType, __( ERROR, status ) );
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
                    messages.retry( messageInfo, retryTimeout );
                }
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return messageInfo;
    }

    public void run() {
        log.trace( "sync..." );

        long now = DateTimeUtils.currentTimeMillis();
        long period = currentPeriod( now );

        Messages.MessageInfo messageInfo = null;

        messages.retry();

        do {
            try {
                now = DateTimeUtils.currentTimeMillis();

                if( messageInfo != null ) {
                    var future = send( messageInfo, now );
                    future.handle( ( mi, e ) -> {
                        messages.removeInProgress( mi );
                        return null;
                    } );
                }

                var currentPeriod = currentPeriod( now );
                if( currentPeriod != period ) {
                    messages.retry();
                    period = currentPeriod;
                }

                messageInfo = messages.poll( diff( now, nextPeriod( now ) ) );
            } catch( InterruptedException e ) {
                log.trace( e.getMessage() );
            }
        } while( messageInfo != null );
    }

    private long currentPeriod( long time ) {
        return time / retryTimeout;
    }

    private long nextPeriod( long now ) {
        var currentPeriod = currentPeriod( now );
        return ( currentPeriod + 1 ) * retryTimeout;
    }

    @SneakyThrows
    public MessageSender syncDisk() {
        if( closed ) return this;

        var messageFiles = Files.fastWildcard( directory, "*/*/*.bin" );

        for( var msgFile : messageFiles ) {
            Path lockFile;

            synchronized( syncDiskLock ) {
                if( closed ) return this;

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

                        var data = Files.read( msgFile, ContentReader.ofBytes() );

                        log.debug( "client id = {}, message type = {}, md5 = {}", msgClientId, messageType, md5Hex );

                        var message = new Message( clientId, messageType, md5, data );
                        messages.add( message );
                    } catch( Exception e ) {
                        LogConsolidated.log( log, Level.ERROR, Dates.s( 5 ), msgFile + ": " + e.getMessage(), e );

                        Files.delete( lockFile );
                    }
                }
            }
        }

        Files.deleteEmptyDirectories( directory, false );

        return this;
    }

    public void clear() {
        messages.clear();
    }

    private long diff( long now, long next ) {
        return next - now;
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
}
