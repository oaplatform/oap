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

package oap.message.client;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.http.Http;
import oap.http.client.OapHttpClient;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.message.MessageProtocol;
import oap.message.MessageStatus;
import oap.message.client.MessageAvailabilityReport.State;
import oap.util.ByteSequence;
import oap.util.Cuid;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import oap.util.Pair;
import oap.util.Throwables;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpMethod;
import org.joda.time.DateTimeUtils;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
@ToString
public class MessageSender implements Closeable, AutoCloseable {
    public static final String FAILURE_IO_STATE = "IO";
    public static final String FAILURE_MESSAGES_STATE = "MESSAGES";
    public static final String FAILURE_SHUTDOWN_STATE = "SHUTDOWN";
    /**
     * the spool is enabled and cannot take messages: a limit is reached or the write failed
     */
    public static final String FAILURE_SPOOL_STATE = "SPOOL";
    /**
     * subdirectory of the persistence directory
     */
    public static final String SPOOL_DIRECTORY = "spool";
    /**
     * the ready messages are sent in the order of priority ( higher first ), FIFO within the same priority
     */
    public static final int DEFAULT_PRIORITY = 0;
    private static final Pair<MessageStatus, Short> STATUS_OK = Pair.__( MessageStatus.OK, MessageProtocol.STATUS_OK );
    public final Object syncDiskLock = new Object();
    public final String host;
    public final int port;
    public final Path directory;
    public final MessageNoRetryStrategy messageNoRetryStrategy;
    public final long clientId = Cuid.UNIQUE.nextLong();
    public final Messages messages = new Messages();
    public final ConcurrentMap<Byte, Pair<MessageStatus, Short>> lastStatus = new ConcurrentHashMap<>();
    public final String messageUrl;
    public final long memorySyncPeriod;
    private final ReentrantLock lock = new ReentrantLock();
    public String uniqueName = Cuid.UNIQUE.next();
    public long storageLockExpiration = Dates.h( 1 );
    public long diskSyncPeriod = Dates.m( 1 );
    public long globalIoRetryTimeout = Dates.s( 1 );
    public long retryTimeout = Dates.s( 1 );
    public long keepAliveDuration = Dates.d( 30 );
    public long timeout = Dates.s( 5 );
    public long connectionTimeout = Dates.s( 30 );
    public volatile boolean closed = false;
    public Scheduled diskSyncScheduler;
    public Scheduled memorySyncScheduler;
    public AtomicLong networkAvailable = new AtomicLong( 0 );
    public int networkAvailableMaxErrors = 2;
    public volatile long ioExceptionStartRetryTimeout = -1;
    public long timeBetweenLogs = Dates.m( 1 );
    /**
     * Number of parallel workers delivering messages to the server within one {@link #syncMemory()} run.
     */
    public int concurrency = 1;
    /**
     * When the number of messages held in memory (ready + retry + inprogress) exceeds this value,
     * {@link #availabilityReport(byte)} reports {@link State#FAILED} ({@link #FAILURE_MESSAGES_STATE}).
     * {@code <= 0} disables the check.
     */
    public int maxMessages = 5000;
    /**
     * runtime parameter: a message that cannot be delivered now ( the server is unavailable, see {@link #FAILURE_IO_STATE},
     * or the memory queue reached {@link #maxMessages} ) is written to the disk spool ( {@code <directory>/spool} ) instead of
     * the memory; {@link #availabilityReport(byte)} stays {@link State#OPERATIONAL} until a spool limit is reached
     * ( {@link #FAILURE_SPOOL_STATE} ). The spool is replayed when the server is available, whether enabled or not.
     */
    public volatile boolean spool = false;
    /**
     * runtime parameter, ms: limit on the age of the oldest spooled message; &lt;= 0 - no limit
     */
    public volatile long spoolMaxAge = 0;
    /**
     * runtime parameter, bytes: limit on the size of the spool; &lt;= 0 - no limit
     */
    public volatile long spoolMaxSize = 0;
    /**
     * runtime parameter, bytes: the spool is full when the usable space of its file store is below; &lt;= 0 - no limit
     */
    public volatile long spoolMinFreeSpace = 0;
    /**
     * runtime parameter, bytes: the active spool segment is closed when it reaches the size
     */
    public volatile long spoolSegmentSize = 64L * 1024 * 1024;
    /**
     * runtime parameter
     */
    public volatile SpoolSync spoolSync = SpoolSync.PERIODIC;
    /**
     * runtime parameter, ms: period of {@link SpoolSync#PERIODIC}
     */
    public volatile long spoolSyncInterval = Dates.s( 1 );
    /**
     * runtime parameter: max number of spooled messages replayed into the memory at once
     * ( 1 while the server is unavailable - the probe )
     */
    public volatile int spoolReplayMessages = 64;
    public Scheduled spoolSyncScheduler;
    private final MessageSpool messageSpool;
    private final Timer spoolSyncTimer;
    private volatile boolean spoolWriteFailed;
    private volatile boolean started;

    public MessageSender( String host, int port, String httpPrefix, Path persistenceDirectory, long memorySyncPeriod ) {
        this( host, port, httpPrefix, persistenceDirectory, memorySyncPeriod, MessageNoRetryStrategy.DROP );
    }

    public MessageSender( String host, int port, String httpPrefix, Path persistenceDirectory,
                          long memorySyncPeriod, MessageNoRetryStrategy messageNoRetryStrategy ) {
        this.host = host;
        this.port = port;
        this.memorySyncPeriod = memorySyncPeriod;
        messageUrl = s( "http://${host}:${port}${httpPrefix}" );
        this.directory = persistenceDirectory;
        this.messageNoRetryStrategy = messageNoRetryStrategy;

        Metrics.gaugeCollectionSize( "message_count", Tags.of( "host", host, "type", "ready", "port", String.valueOf( port ) ), messages.ready );
        Metrics.gaugeCollectionSize( "message_count", Tags.of( "host", host, "type", "retry", "port", String.valueOf( port ) ), messages.retry );
        Metrics.gaugeMapSize( "message_count", Tags.of( "host", host, "type", "inprogress", "port", String.valueOf( port ) ), messages.inProgress );

        Tags tags = Tags.of( "host", host, "port", String.valueOf( port ) );
        messageSpool = persistenceDirectory != null
            ? new MessageSpool( persistenceDirectory.resolve( SPOOL_DIRECTORY ), clientId, uniqueName )
            : null;
        if( messageSpool != null ) {
            Metrics.gauge( "message_spool_bytes", tags, messageSpool, MessageSpool::bytes );
            Metrics.gauge( "message_spool_messages", tags, messageSpool, MessageSpool::messages );
            Metrics.gauge( "message_spool_segments", tags, messageSpool, MessageSpool::segments );
            Metrics.gauge( "message_spool_oldest_age_ms", tags, messageSpool, MessageSpool::oldestAge );
            Metrics.gauge( "message_spool_free_space_bytes", tags, messageSpool, MessageSpool::freeSpace );
        }
        Metrics.gauge( "message_spool_offline", tags, this, s -> s.isOffline() ? 1 : 0 );
        for( SpoolLimit limit : SpoolLimit.values() ) {
            if( limit == SpoolLimit.NONE ) continue;
            Metrics.gauge( "message_spool_limit", tags.and( "limit", limit.name().toLowerCase( Locale.ROOT ) ), this,
                s -> s.getSpoolLimit() == limit ? 1 : 0 );
        }
        spoolSyncTimer = Metrics.timer( "message_spool_sync", tags );
    }

    public static Path lock( String uniqueName, Path file, long storageLockExpiration ) {
        Path lockFile = Paths.get( FilenameUtils.removeExtension( file.toString() ) + ".lock" );

        if( Files.createFile( lockFile ) ) return lockFile;
        if( storageLockExpiration <= 0 ) return null;

        log.trace( "[{}] lock found {}, expiration = {}", uniqueName, lockFile,
            Dates.durationToString( Files.getLastModifiedTime( lockFile ) + storageLockExpiration - DateTimeUtils.currentTimeMillis() ) );

        return Files.getLastModifiedTime( lockFile ) + storageLockExpiration < DateTimeUtils.currentTimeMillis()
            ? lockFile : null;
    }

    public final long getClientId() {
        return clientId;
    }

    @SneakyThrows
    public void start() {
        log.info( "[{}] message server messageUrl {} storage {} storageLockExpiration {}",
            uniqueName, messageUrl, directory, Dates.durationToString( storageLockExpiration ) );
        log.info( "[{}] connection timeout {} rw timeout {} keepAliveDuration {}",
            uniqueName, Dates.durationToString( connectionTimeout ), Dates.durationToString( timeout ),
            Dates.durationToString( keepAliveDuration ) );
        log.info( "[{}] retry timeout {} disk sync period '{}' memory sync period '{}'",
            uniqueName, Dates.durationToString( retryTimeout ), Dates.durationToString( diskSyncPeriod ), Dates.durationToString( memorySyncPeriod ) );
        log.info( "[{}] concurrency {} maxMessages {}", uniqueName, concurrency, maxMessages );
        log.info( "[{}] spool {} maxAge '{}' maxSize {} minFreeSpace {} segmentSize {} sync {} syncInterval '{}' replayMessages {}",
            uniqueName, spool, Dates.durationToString( spoolMaxAge ), spoolMaxSize, spoolMinFreeSpace, spoolSegmentSize,
            spoolSync, Dates.durationToString( spoolSyncInterval ), spoolReplayMessages );
        log.info( "custom status = {}", MessageProtocol.printMapping() );

        adoptSpool();

        lock.lock();
        try {
            if( diskSyncPeriod > 0 )
                diskSyncScheduler = Scheduler.scheduleWithFixedDelay( diskSyncPeriod, TimeUnit.MILLISECONDS, this::syncDisk );

            if( memorySyncPeriod > 0 )
                memorySyncScheduler = Scheduler.scheduleWithFixedDelay( memorySyncPeriod, TimeUnit.MILLISECONDS, this::syncMemory );

            rescheduleSpoolSync();
            started = true;
        } finally {
            lock.unlock();
        }
        log.info( "[{}] message server started", uniqueName );
    }

    /**
     * under lock
     */
    private void rescheduleSpoolSync() {
        Scheduled.cancel( spoolSyncScheduler );
        spoolSyncScheduler = messageSpool != null && spoolSyncInterval > 0 && !closed
            ? Scheduler.scheduleWithFixedDelay( spoolSyncInterval, TimeUnit.MILLISECONDS, this::syncSpool )
            : null;
    }

    /**
     * {@link SpoolSync#PERIODIC}: fdatasync of the active spool segments
     */
    public void syncSpool() {
        if( messageSpool != null && spoolSync == SpoolSync.PERIODIC ) spoolSyncTimer.record( messageSpool::sync );
    }

    public void setSpool( boolean spool ) {
        log.info( "[{}] spool {} -> {}", uniqueName, this.spool, spool );
        this.spool = spool;
    }

    public void setSpoolMaxAge( long spoolMaxAge ) {
        log.info( "[{}] spoolMaxAge '{}' -> '{}'", uniqueName, Dates.durationToString( this.spoolMaxAge ), Dates.durationToString( spoolMaxAge ) );
        this.spoolMaxAge = spoolMaxAge;
    }

    public void setSpoolMaxSize( long spoolMaxSize ) {
        log.info( "[{}] spoolMaxSize {} -> {}", uniqueName, this.spoolMaxSize, spoolMaxSize );
        this.spoolMaxSize = spoolMaxSize;
    }

    public void setSpoolMinFreeSpace( long spoolMinFreeSpace ) {
        log.info( "[{}] spoolMinFreeSpace {} -> {}", uniqueName, this.spoolMinFreeSpace, spoolMinFreeSpace );
        this.spoolMinFreeSpace = spoolMinFreeSpace;
    }

    public void setSpoolSegmentSize( long spoolSegmentSize ) {
        Preconditions.checkArgument( spoolSegmentSize > 0, "spoolSegmentSize must be > 0, but %s", spoolSegmentSize );
        log.info( "[{}] spoolSegmentSize {} -> {}", uniqueName, this.spoolSegmentSize, spoolSegmentSize );
        this.spoolSegmentSize = spoolSegmentSize;
    }

    public void setSpoolSync( SpoolSync spoolSync ) {
        Preconditions.checkNotNull( spoolSync );
        log.info( "[{}] spoolSync {} -> {}", uniqueName, this.spoolSync, spoolSync );
        this.spoolSync = spoolSync;
    }

    /**
     * runtime change: the periodic sync is rescheduled immediately
     */
    public void setSpoolSyncInterval( long spoolSyncInterval ) {
        log.info( "[{}] spoolSyncInterval '{}' -> '{}'", uniqueName,
            Dates.durationToString( this.spoolSyncInterval ), Dates.durationToString( spoolSyncInterval ) );
        lock.lock();
        try {
            this.spoolSyncInterval = spoolSyncInterval;
            if( started ) rescheduleSpoolSync();
        } finally {
            lock.unlock();
        }
    }

    public void setSpoolReplayMessages( int spoolReplayMessages ) {
        Preconditions.checkArgument( spoolReplayMessages > 0, "spoolReplayMessages must be > 0, but %s", spoolReplayMessages );
        log.info( "[{}] spoolReplayMessages {} -> {}", uniqueName, this.spoolReplayMessages, spoolReplayMessages );
        this.spoolReplayMessages = spoolReplayMessages;
    }

    public <T> MessageSender send( byte messageType, T data, ContentWriter<T> writer ) {
        return send( messageType, ( short ) 1, data, writer );
    }

    public <T> MessageSender send( byte messageType, short version, T data, ContentWriter<T> writer ) {
        return send( messageType, version, DEFAULT_PRIORITY, data, writer );
    }

    /**
     * @param priority higher - sent earlier; messages of the same priority are sent in the order of sending
     */
    public <T> MessageSender send( byte messageType, short version, int priority, T data, ContentWriter<T> writer ) {
        byte[] bytes = writer.write( data );
        return send( messageType, version, priority, bytes, 0, bytes.length );
    }

    public MessageSender send( byte messageType, byte[] data, int offset, int length ) {
        return send( messageType, ( short ) 1, data, offset, length );
    }

    public MessageSender send( byte messageType, short version, byte[] data, int offset, int length ) {
        return send( messageType, version, DEFAULT_PRIORITY, data, offset, length );
    }

    /**
     * @param priority higher - sent earlier; messages of the same priority are sent in the order of sending
     */
    public MessageSender send( byte messageType, short version, int priority, byte[] data, int offset, int length ) {
        Preconditions.checkNotNull( data );
        Preconditions.checkArgument( ( messageType & 0xFF ) <= 200, "reserved" );

        MessageDigest digest = DigestUtils.getMd5Digest();
        digest.update( data, offset, length );
        byte[] md5 = digest.digest();

        if( spool && spoolable( messageType ) && toSpool( priority, messageType, version, md5, data, offset, length ) ) return this;

        Message message = new Message( clientId, messageType, version, priority, ByteSequence.of( md5 ), data, offset, length );
        messages.add( message );

        return this;
    }

    /**
     * the message cannot be delivered now and the spool can take it
     */
    private boolean spoolable( byte messageType ) {
        return messageSpool != null && !closed
            && ( isOffline( messageType ) || maxMessages > 0 && messages.size() >= maxMessages )
            && getSpoolLimit() == SpoolLimit.NONE;
    }

    /**
     * @return false - the write failed, the message is not in the spool
     */
    private boolean toSpool( int priority, byte messageType, short version, byte[] md5, byte[] data, int offset, int length ) {
        try {
            int written = messageSpool.append( priority, messageType, version, md5, data, offset, length, spoolSegmentSize, spoolSync );
            spoolWriteFailed = false;
            spoolEvent( messageType, "written", written );
            return true;
        } catch( IOException | RuntimeException e ) {
            spoolWriteFailed = true;
            spoolEvent( messageType, "write_error", 0 );
            LogConsolidated.log( log, Level.ERROR, timeBetweenLogs, "[" + uniqueName + "] spool: " + e.getMessage(), e );
            return false;
        }
    }

    private static void spoolEvent( byte messageType, String event, long bytes ) {
        String type = MessageProtocol.messageTypeToString( messageType );
        Metrics.counter( "oap.messages.spool", "type", type, "event", event ).increment();
        if( bytes > 0 ) Metrics.counter( "oap.messages.spool.bytes", "type", type, "event", event ).increment( bytes );
    }

    private static void spoolEvent( String event, int count ) {
        Metrics.counter( "oap.messages.spool", "type", "-", "event", event ).increment( count );
    }

    /**
     * the server is unavailable: network errors or the last delivery of the message type returned a retryable error
     */
    private boolean isOffline( byte messageType ) {
        return isOffline() || lastStatus.getOrDefault( messageType, STATUS_OK )._1 == MessageStatus.ERROR;
    }

    private boolean isOffline() {
        return networkAvailable.get() > networkAvailableMaxErrors;
    }

    /**
     * the spool limit reached ( whether the spool is enabled or not )
     */
    public SpoolLimit getSpoolLimit() {
        return messageSpool != null ? messageSpool.limit( spoolMaxAge, spoolMaxSize, spoolMinFreeSpace ) : SpoolLimit.NONE;
    }

    /**
     * not delivered spooled messages
     */
    public long getSpoolMessages() {
        return messageSpool != null ? messageSpool.messages() : 0;
    }

    public long getSpoolBytes() {
        return messageSpool != null ? messageSpool.bytes() : 0;
    }

    public int getSpoolSegments() {
        return messageSpool != null ? messageSpool.segments() : 0;
    }

    private void adoptSpool() {
        if( messageSpool == null ) return;
        int adopted = messageSpool.adoptOrphans( storageLockExpiration );
        if( adopted > 0 ) spoolEvent( "adopted", adopted );
    }

    /**
     * the server is unavailable: the messages of the memory queues ( not the ones read from the spool ) go to the spool
     */
    private void spill() {
        if( !spool || messageSpool == null || closed || getSpoolLimit() != SpoolLimit.NONE ) return;

        List<Messages.MessageInfo> drained = messages.drain( mi -> mi.segment == null && isOffline( mi.message.messageType ) );
        if( drained.isEmpty() ) return;

        int spilled = 0;
        for( Messages.MessageInfo mi : drained ) {
            Message m = mi.message;
            if( toSpool( m.priority, m.messageType, m.version, m.md5.bytes, m.data, 0, m.data.length ) ) spilled++;
            else messages.add( mi );
        }
        log.debug( "[{}] spilled {}/{} message(s)", uniqueName, spilled, drained.size() );
    }

    /**
     * reads the spooled messages into the memory: up to {@link #spoolReplayMessages} in flight, 1 while the server is
     * unavailable ( the probe )
     */
    private void replay() {
        if( messageSpool == null || closed ) return;

        int window = isOffline() ? 1 : Math.max( 1, spoolReplayMessages );
        long now = DateTimeUtils.currentTimeMillis();
        while( messageSpool.inFlight() < window ) {
            SpoolSegment.Record record = messageSpool.next( event -> spoolEvent( event, 1 ) );
            if( record == null ) break;

            Message message = new Message( clientId, record.messageType(), record.version(), record.segment().priority,
                ByteSequence.of( record.md5() ), record.data() );
            Messages.MessageInfo messageInfo = new Messages.MessageInfo( message, now );
            messageInfo.segment = record.segment();
            messages.add( messageInfo );
            spoolEvent( record.messageType(), "replayed", record.data().length );
        }
    }

    /**
     * a spooled message got a final status
     */
    private void spoolDone( Messages.MessageInfo messageInfo, String event ) {
        SpoolSegment segment = messageInfo.segment;
        if( segment == null ) return;
        messageInfo.segment = null;
        messageSpool.ack( segment );
        spoolEvent( messageInfo.message.messageType, event, 0 );
    }


    @SneakyThrows
    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;

            Closeables.close( diskSyncScheduler );
            Closeables.close( memorySyncScheduler );
            Scheduled.cancel( spoolSyncScheduler );
            spoolSyncScheduler = null;
        } finally {
            lock.unlock();
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

        try {
            saveMessagesToDirectory( directory );
        } finally {
            if( messageSpool != null ) messageSpool.close();
        }
    }

    /**
     * the unsent messages: to the spool when it is enabled, else a file per message;
     * a message read from the spool stays in its segment
     */
    private void saveMessagesToDirectory( Path directory ) {
        while( true ) {
            Messages.MessageInfo messageInfo = messages.poll( false );
            if( messageInfo == null ) {
                messageInfo = messages.pollInProgress();
            }
            if( messageInfo == null ) {
                Messages.RetryInfo retryInfo = messages.pollRetry();
                messageInfo = retryInfo != null ? retryInfo.messageInfo : null;
            }

            if( messageInfo == null ) break;
            Message message = messageInfo.message;

            if( messageInfo.segment != null ) {
                messageInfo.segment = null;
                messageSpool.release();
                continue;
            }

            if( spool && messageSpool != null && getSpoolLimit() == SpoolLimit.NONE
                && toSpool( message.priority, message.messageType, message.version, message.md5.bytes, message.data, 0, message.data.length ) ) {
                continue;
            }

            Path parentDirectory = directory
                .resolve( Long.toHexString( clientId ) )
                .resolve( String.valueOf( Byte.toUnsignedInt( message.messageType ) ) );
            String fileName = MessageFileName.of( message );
            Path tmpMsgPath = parentDirectory.resolve( fileName + ".bin.tmp" );
            log.debug( "[{}] writing unsent message to {}", uniqueName, tmpMsgPath );
            try {
                Files.write( tmpMsgPath, message.data, ContentWriter.ofBytes() );
                Path msgPath = parentDirectory.resolve( fileName + ".bin" );
                Files.rename( tmpMsgPath, msgPath );
            } catch( Exception e ) {
                log.error( "[{}] type: {}, md5: {}, data: {}",
                    uniqueName, message.messageType, message.getHexMd5(), message.getHexData(), e );
            }
        }
    }

    /**
     * With {@link #spool} enabled and not failed the IO and the memory queue size do not matter ( the messages go to the spool ).
     */
    public MessageAvailabilityReport availabilityReport( byte messageType ) {
        boolean spoolFailed = spool && ( messageSpool == null || spoolWriteFailed || getSpoolLimit() != SpoolLimit.NONE );
        boolean spoolOperational = spool && !spoolFailed;
        boolean ioFailed = !spoolOperational && isOffline( messageType );
        boolean messagesFailed = !spoolOperational && maxMessages > 0 && messages.size() > maxMessages;
        boolean shutdown = closed;

        if( !ioFailed && !messagesFailed && !shutdown && !spoolFailed ) return new MessageAvailabilityReport( State.OPERATIONAL );

        Map<String, State> states = new LinkedHashMap<>();
        states.put( FAILURE_IO_STATE, ioFailed ? State.FAILED : State.OPERATIONAL );
        states.put( FAILURE_MESSAGES_STATE, messagesFailed ? State.FAILED : State.OPERATIONAL );
        states.put( FAILURE_SHUTDOWN_STATE, shutdown ? State.FAILED : State.OPERATIONAL );
        if( spool ) states.put( FAILURE_SPOOL_STATE, spoolFailed ? State.FAILED : State.OPERATIONAL );
        return new MessageAvailabilityReport( State.FAILED, states );
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public Messages.MessageInfo send( Messages.MessageInfo messageInfo, long now ) {
        Message message = messageInfo.message;

        log.debug( "[{}] sending data [type = {}] to server...", uniqueName, MessageProtocol.messageTypeToString( message.messageType ) );

        Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "trysend" ).increment();

        try( FastByteArrayOutputStream buf = new FastByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream( buf ) ) {
            out.writeByte( message.messageType );
            out.writeShort( message.version );
            out.writeLong( message.clientId );

            out.write( message.md5.bytes );

            out.write( MessageProtocol.RESERVED, 0, MessageProtocol.RESERVED_LENGTH );
            out.writeInt( message.data.length );
            out.write( message.data );

            InputStreamResponseListener inputStreamResponseListener = new InputStreamResponseListener();
            OapHttpClient.DEFAULT_HTTP_CLIENT
                .newRequest( messageUrl )
                .method( HttpMethod.POST )
                .timeout( timeout, TimeUnit.MILLISECONDS )
                .body( new BytesRequestContent( Http.ContentType.APPLICATION_OCTET_STREAM, buf.array ) )
                .send( inputStreamResponseListener );

            Response response = inputStreamResponseListener.get( timeout, TimeUnit.MILLISECONDS );

            if( response.getStatus() >= 300 || response.getStatus() < 200 ) {
                throw new IOException( "Not OK (" + response.getStatus() + ") response code returned for url: " + messageUrl );
            }
            return onOkRespone( messageInfo, inputStreamResponseListener, now );

        } catch( UnknownHostException e ) {
            ioExceptionStartRetryTimeout = now;
            networkAvailable.incrementAndGet();

            processException( messageInfo, now, message, e, true );

            throw Throwables.propagate( e );
        } catch( Throwable e ) {
            networkAvailable.incrementAndGet();
            processException( messageInfo, now, message, e, false );

            throw Throwables.propagate( e );
        }
    }

    private void processException( Messages.MessageInfo messageInfo, long now, Message message, Throwable e, boolean globalRetryTimeout ) {
        String status = e instanceof TimeoutException ? "send_timeout" : "send_io_error";

        Metrics.counter( "oap.messages",
            "type", MessageProtocol.messageTypeToString( message.messageType ),
            "status", status + ( globalRetryTimeout ? "_gr" : "" ) ).increment();
        LogConsolidated.log( log, Level.ERROR, timeBetweenLogs, e.getMessage(), e );
        messages.retry( messageInfo, now + retryTimeout );
    }

    private Messages.MessageInfo onOkRespone( Messages.MessageInfo messageInfo, InputStreamResponseListener inputStreamResponseListener, long now ) {
        Message message = messageInfo.message;

        InputStream body = inputStreamResponseListener.getInputStream();

        try( DataInputStream in = new DataInputStream( body ) ) {
            byte version = in.readByte();
            if( version != MessageProtocol.PROTOCOL_VERSION_1 ) {
                log.error( "[{}] Version mismatch, expected: {}, received: {}", uniqueName, MessageProtocol.PROTOCOL_VERSION_1, version );
                throw new MessageException( "Version mismatch" );
            }
            in.readLong(); // clientId
            in.skipNBytes( MessageProtocol.MD5_LENGTH ); // digestionId
            in.skipNBytes( MessageProtocol.RESERVED_LENGTH );
            short status = in.readShort();

            log.trace( "[{}] sending done, server status: {}", uniqueName, MessageProtocol.messageStatusToString( status ) );

            MessageSender.this.networkAvailable.set( 0 );

            switch( status ) {
                case MessageProtocol.STATUS_ALREADY_WRITTEN -> {
                    log.trace( "[{}] already written {}", uniqueName, message.getHexMd5() );
                    Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "already_written" ).increment();
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.ALREADY_WRITTEN, status ) );
                    spoolDone( messageInfo, "delivered" );
                }
                case MessageProtocol.STATUS_OK -> {
                    Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "success" ).increment();
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.OK, status ) );
                    spoolDone( messageInfo, "delivered" );
                }
                case MessageProtocol.STATUS_UNKNOWN_ERROR -> {
                    Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "error" ).increment();
                    log.error( "[{}] unknown error", uniqueName );
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.ERROR, status ) );
                    messages.retry( messageInfo, now + retryTimeout );
                }
                case MessageProtocol.STATUS_UNKNOWN_ERROR_NO_RETRY -> {
                    Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "error_no_retry" ).increment();
                    log.error( "[{}] unknown error -> no retry", uniqueName );
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.ERROR_NO_RETRY, status ) );
                    spoolDone( messageInfo, "dropped" );
                }
                case MessageProtocol.STATUS_UNKNOWN_MESSAGE_TYPE -> {
                    Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "unknown_message_type" ).increment();
                    log.error( "[{}] unknown message type: {}", uniqueName, status );
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.ERROR_NO_RETRY, status ) );
                    spoolDone( messageInfo, "dropped" );
                }
                default -> {
                    String clientStatus = MessageProtocol.getStatus( status );
                    if( clientStatus != null ) {
                        log.trace( "[{}] retry: {}", uniqueName, clientStatus );
                        Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "status_" + status + "(" + clientStatus + ")" ).increment();
                    } else {
                        Metrics.counter( "oap.messages", "type", MessageProtocol.messageTypeToString( message.messageType ), "status", "unknown_status" ).increment();
                        log.error( "[{}] unknown status: {}", uniqueName, status );
                    }
                    lastStatus.put( message.messageType, Pair.__( MessageStatus.ERROR, status ) );
                    messages.retry( messageInfo, now + retryTimeout );
                }
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return messageInfo;
    }

    public boolean syncMemory() {
        log.trace(  "[{}] syncMemory...", uniqueName );
        try {
            if( getReadyMessages() + getRetryMessages() + getInProgressMessages() > 0 )
                log.trace( "[{}] sync ready {} retry {} inprogress {} ...",
                    uniqueName, getReadyMessages(), getRetryMessages(), getInProgressMessages() );

            long now = DateTimeUtils.currentTimeMillis();

            spill();

            if( isGlobalIoRetryTimeout( now ) ) {
                log.trace( "skip, isGlobalIoRetryTimeout" );
                return true;
            }

            messages.retry();
            replay();

            if( concurrency <= 1 ) return syncMemoryWorker();

            List<Future<Boolean>> workers = new ArrayList<>( concurrency );
            try( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor() ) {
                for( int i = 0; i < concurrency; i++ ) workers.add( executor.submit( this::syncMemoryWorker ) );
            }

            boolean result = true;
            for( Future<Boolean> worker : workers ) result &= worker.get();
            return result;
        } catch( Exception e ) {
            return false;
        } finally {
            log.trace(  "[{}] syncMemory... DONE", uniqueName );
        }
    }

    private boolean syncMemoryWorker() {
        try {
            long now = DateTimeUtils.currentTimeMillis();
            long period = currentPeriod( now );

            Messages.MessageInfo messageInfo = messages.poll( true );

            while( messageInfo != null ) {
                now = DateTimeUtils.currentTimeMillis();

                log.trace( "[{}] message {}...", uniqueName, messageInfo.message.md5 );
                Messages.MessageInfo mi = send( messageInfo, now );

                messages.removeInProgress( mi );
                log.trace( "[{}] message {}... done", uniqueName, mi.message.md5 );

                if( isGlobalIoRetryTimeout( now ) ) {
                    log.trace( "skip, isGlobalIoRetryTimeout" );
                    break;
                }

                long currentPeriod = currentPeriod( now );
                if( currentPeriod != period ) {
                    messages.retry();
                    period = currentPeriod;
                }

                messageInfo = messages.poll( true );
            }

            return true;
        } catch( Exception e ) {
            return false;
        }
    }

    private boolean isGlobalIoRetryTimeout( long now ) {
        return ioExceptionStartRetryTimeout > 0 && globalIoRetryTimeout > 0 && ioExceptionStartRetryTimeout + globalIoRetryTimeout > now;
    }

    private long currentPeriod( long time ) {
        return time / retryTimeout;
    }

    @SneakyThrows
    public MessageSender syncDisk() {
        if( closed ) return this;

        adoptSpool();

        if( !java.nio.file.Files.isDirectory( directory ) ) return this;

        try( DirectoryStream<Path> clientIdStream = java.nio.file.Files.newDirectoryStream( directory ) ) {
            for( Path clientIdPath : clientIdStream ) {
                if( messageSpool != null && clientIdPath.toAbsolutePath().normalize().equals( messageSpool.directory ) ) continue;

                if( !isValidClientId( clientIdPath ) ) {
                    log.warn( "invalid client id {}", clientIdPath );
                    Files.deleteSafely( clientIdPath );
                    continue;
                }

                long msgClientId = Long.parseLong( FilenameUtils.getName( clientIdPath.toString() ), 16 );
                try( DirectoryStream<Path> messageTypeStream = java.nio.file.Files.newDirectoryStream( clientIdPath ) ) {
                    for( Path messageTypePath : messageTypeStream ) {
                        if( !isValidMessageType( messageTypePath ) ) {
                            log.warn( "invalid message type {}", messageTypePath );
                            Files.deleteSafely( messageTypePath );
                            continue;
                        }

                        byte messageType = ( byte ) Integer.parseInt( FilenameUtils.getName( messageTypePath.toString() ) );

                        try( DirectoryStream<Path> messageStream = java.nio.file.Files.newDirectoryStream( messageTypePath ) ) {
                            for( Path messagePath : messageStream ) {
                                if( !isValidMessage( messagePath ) ) {
                                    log.warn( "invalid message {}", messagePath );
                                    Files.deleteSafely( messagePath );
                                    continue;
                                }

                                if( messagePath.toString().endsWith( ".lock" ) ) {
                                    Path binFile = Paths.get( FilenameUtils.removeExtension( messagePath.toString() ) + ".bin" );
                                    if( !java.nio.file.Files.exists( binFile ) ) {
                                        log.warn( "invalid lock file {}", messagePath );
                                        Files.deleteSafely( messagePath );
                                    }

                                    continue;
                                }

                                MessageFileName fileName = MessageFileName.parse( messagePath );

                                Path lockFile;

                                lock.lock();
                                try {
                                    if( closed ) return this;

                                    if( ( lockFile = lock( uniqueName, messagePath, storageLockExpiration ) ) != null ) {
                                        log.info( "[{}] reading unsent message {}", uniqueName, messagePath );
                                        try {
                                            byte[] data = Files.read( messagePath, ContentReader.ofBytes() );

                                            log.info( "[{}] client id {} message type {} md5 {} priority {}",
                                                uniqueName, msgClientId, MessageProtocol.messageTypeToString( messageType ),
                                                fileName.md5Hex(), fileName.priority() );

                                            Message message = new Message( clientId, messageType, fileName.version(), fileName.priority(),
                                                ByteSequence.of( Hex.decodeHex( fileName.md5Hex() ) ), data );
                                            messages.add( message );

                                            Files.delete( messagePath );
                                        } catch( Exception e ) {
                                            LogConsolidated.log( log, Level.ERROR, timeBetweenLogs, "[" + uniqueName + "] " + messagePath + ": " + e.getMessage(), e );
                                        } finally {
                                            Files.delete( lockFile );
                                        }
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }
                    }
                }
                // not the whole directory: the spool keeps its empty directories
                Files.deleteEmptyDirectories( clientIdPath, true );
            }
        }

        return this;
    }

    private boolean isValidMessage( Path messagePath ) {
        try {
            if( java.nio.file.Files.isDirectory( messagePath ) ) return false;
            String messageFile = messagePath.toString();
            if( !messageFile.endsWith( ".bin" ) && !messageFile.endsWith( ".lock" ) ) return false;

            Hex.decodeHex( MessageFileName.parse( messagePath ).md5Hex() );

            return true;
        } catch( DecoderException | NumberFormatException e ) {
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

    public enum SpoolLimit {
        NONE,
        /**
         * {@link #spoolMaxSize}
         */
        SIZE,
        /**
         * {@link #spoolMaxAge}
         */
        AGE,
        /**
         * {@link #spoolMinFreeSpace}
         */
        FREE_SPACE
    }
}
