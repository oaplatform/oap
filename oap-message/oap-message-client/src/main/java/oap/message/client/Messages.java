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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.message.MessageProtocol;
import oap.util.ByteSequence;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Slf4j
class Messages {
    /**
     * higher priority first, FIFO within the same priority
     */
    private static final Comparator<MessageInfo> ORDER = Comparator
        .comparingInt( ( MessageInfo mi ) -> mi.message.priority ).reversed()
        .thenComparingLong( mi -> mi.seq );

    final PriorityBlockingQueue<MessageInfo> ready = new PriorityBlockingQueue<>( 64, ORDER );
    final LinkedBlockingQueue<RetryInfo> retry = new LinkedBlockingQueue<>();
    final ConcurrentHashMap<ByteSequence, MessageInfo> inProgress = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public void add( Message message ) {
        add( new MessageInfo( message, DateTimeUtils.currentTimeMillis() ) );
    }

    /**
     * a re-added ( retried ) message goes to the tail of its priority
     */
    public void add( MessageInfo messageInfo ) {
        messageInfo.seq = sequence.incrementAndGet();
        ready.add( messageInfo );
    }

    public void retry( MessageInfo messageInfo, long time ) {
        messageInfo.retryCount++;

        log.trace( "retry messageType {} md5 {} retryCount {} wait until {}",
            MessageProtocol.messageTypeToString( messageInfo.message.messageType ),
            messageInfo.message.md5,
            messageInfo.retryCount, Dates.formatDateWithMillis( time ) );
        inProgress.remove( messageInfo.message.md5 );
        retry.add( new RetryInfo( messageInfo, time ) );
    }

    public MessageInfo poll( boolean inProgress ) {
        MessageInfo messageInfo = ready.poll();
        if( messageInfo != null && inProgress ) this.inProgress.put( messageInfo.message.md5, messageInfo );
        return messageInfo;
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public synchronized void retry() {
        if( getRetryMessages() == 0 ) return;
        long now = DateTimeUtils.currentTimeMillis();

        log.trace( "ready {} retry {} now {}",
            getReadyMessages(), getRetryMessages(), Dates.formatDateWithMillis( now ) );

        Iterator<RetryInfo> it = retry.iterator();
        while( it.hasNext() ) {
            RetryInfo retryInfo = it.next();
            if( retryInfo.startTime < now ) {
                it.remove();
                add( retryInfo.messageInfo );
                log.trace( "retrying " + retryInfo.messageInfo.message.md5 + " ..." );
            }
        }
    }

    /**
     * removes the ready and retry messages matching the filter ( not the in progress ones )
     */
    public synchronized List<MessageInfo> drain( Predicate<MessageInfo> filter ) {
        ArrayList<MessageInfo> drained = new ArrayList<>();
        ready.removeIf( mi -> filter.test( mi ) && drained.add( mi ) );
        retry.removeIf( ri -> filter.test( ri.messageInfo ) && drained.add( ri.messageInfo ) );
        return drained;
    }

    public boolean isEmpty() {
        return ready.isEmpty();
    }

    public void clear() {
        ready.clear();
    }

    public RetryInfo pollRetry() {
        return retry.poll();
    }

    public int getReadyMessages() {
        return ready.size();
    }

    public int getRetryMessages() {
        return retry.size();
    }

    public int getInProgressMessages() {
        return inProgress.size();
    }

    public MessageInfo pollInProgress() {
        try {
            Iterator<Map.Entry<ByteSequence, MessageInfo>> iterator = inProgress.entrySet().iterator();
            MessageInfo messageInfo = null;
            if( iterator.hasNext() ) {
                messageInfo = iterator.next().getValue();
                iterator.remove();
            }
            return messageInfo;
        } catch( NoSuchElementException e ) {
            return null;
        }
    }

    public void removeInProgress( MessageInfo messageInfo ) {
        inProgress.remove( messageInfo.message.md5, messageInfo );
    }

    /**
     * @return number of messages held in memory: ready + retry + inprogress
     */
    public int size() {
        return ready.size() + retry.size() + inProgress.size();
    }

    public void reset() {
        retry.clear();
        inProgress.clear();
        retry.clear();
    }

    @ToString
    static class MessageInfo {
        public final Message message;
        public final long startTime;
        public int retryCount;
        /**
         * order within the priority, assigned by {@link Messages#add(MessageInfo)} before the message enters the ready queue
         */
        long seq;
        /**
         * the spool segment the message was read from ( replay ), null - not from the spool
         */
        SpoolSegment segment;

        MessageInfo( Message message, long startTime ) {
            this.message = message;
            this.startTime = startTime;
            this.retryCount = 0;
        }
    }

    @ToString
    static class RetryInfo {
        public final MessageInfo messageInfo;
        public final long startTime;

        RetryInfo( MessageInfo messageInfo, long startTime ) {
            this.messageInfo = messageInfo;
            this.startTime = startTime;
        }
    }
}
