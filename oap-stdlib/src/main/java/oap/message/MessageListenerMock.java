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

import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public class MessageListenerMock implements MessageListener {
    public static final byte MESSAGE_TYPE = ( byte ) 0xFF;
    public static final byte MESSAGE_TYPE2 = ( byte ) 0xFE;
    public final ArrayList<TestMessage> messages = new ArrayList<>();
    private final byte messageType;
    private int throwUnknownError = 0;

    public MessageListenerMock( byte messageType ) {
        this.messageType = messageType;
    }

    @Override
    public final byte getId() {
        return messageType;
    }

    @Override
    public final String getInfo() {
        return "mock-message-listener-" + messageType;
    }

    @Override
    public void run( int version, String hostName, int size, byte[] data ) {
        messages.add( new TestMessage( version, new String( data, UTF_8 ) ) );

        if( throwUnknownError > 0 ) {
            throwUnknownError -= 1;
            throw new RuntimeException( "unknown error" );
        }
    }

    public void throwUnknownError( int count ) {
        throwUnknownError = count;
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
