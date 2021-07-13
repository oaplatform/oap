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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MessageListenerMock implements MessageListener {
    public static final byte MESSAGE_TYPE = ( byte ) 0x7F;
    public static final byte MESSAGE_TYPE2 = ( byte ) 0x7E;
    public final AtomicLong accessCount = new AtomicLong();
    private final CopyOnWriteArrayList<TestMessage> messages = new CopyOnWriteArrayList<>();
    private final String infoPrefix;
    private final byte messageType;
    public int throwUnknownError = 0;
    public short status = MessageProtocol.STATUS_OK;
    public boolean noRetry = false;

    public MessageListenerMock( byte messageType ) {
        this( "mock-message-listener-", messageType );
    }

    public MessageListenerMock( String infoPrefix, byte messageType ) {
        this.infoPrefix = infoPrefix;
        this.messageType = messageType;
    }

    @Override
    public final byte getId() {
        return messageType;
    }

    @Override
    public final String getInfo() {
        return infoPrefix + ( messageType & 0xFF );
    }

    @Override
    public short run( int version, String hostName, int size, byte[] data ) {
        accessCount.incrementAndGet();
        if( throwUnknownError > 0 ) {
            throwUnknownError -= 1;
            if( noRetry )
                throw new RuntimeException( "unknown error" );
            else
                return MessageProtocol.STATUS_UNKNOWN_ERROR;
        }

        if( status == MessageProtocol.STATUS_OK )
            messages.add( new TestMessage( version, new String( data, UTF_8 ) ) );

        return status;
    }

    public List<TestMessage> getMessages() {
        return messages;
    }

    public void throwUnknownError( int count, boolean noRetry ) {
        throwUnknownError = count;
        this.noRetry = noRetry;
    }

    public void setStatusOk() {
        setStatus( MessageProtocol.STATUS_OK );
    }

    public void setStatus( int status ) {
        this.status = ( short ) status;
    }

    @ToString
    @EqualsAndHashCode
    public static class TestMessage {
        public final int version;
        public final String data;

        public TestMessage( int version, String data ) {
            this.version = version;
            this.data = data;
        }
    }
}
