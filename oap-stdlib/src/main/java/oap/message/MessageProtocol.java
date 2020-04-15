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

import java.io.Serializable;

public interface MessageProtocol {
    short PROTOCOL_VERSION_1 = 1;
    byte[] RESERVED = new byte[8];
    int RESERVED_LENGTH = RESERVED.length;
    int MD5_LENGTH = 16;

    short STATUS_OK = 0;
    short STATUS_UNKNOWN_ERROR = 1;

    short STATUS_UNKNOWN_MESSAGE_TYPE = 100;
    short STATUS_ALREADY_WRITTEN = 101;

    static String statusToString( short status ) {
        return switch( status ) {
            case STATUS_OK -> "OK";
            case STATUS_ALREADY_WRITTEN -> "ALREADY_WRITTEN";
            case STATUS_UNKNOWN_ERROR -> "UNKNOWN_ERROR";
            case STATUS_UNKNOWN_MESSAGE_TYPE -> "UNKNOWN_MESSAGE_TYPE";
            default -> String.valueOf( status );
        };
    }

    @EqualsAndHashCode
    @ToString
    class ClientId implements Serializable {
        private static final long serialVersionUID = -6305024925123030053L;

        public final int messageType;
        public final long clientId;

        public ClientId( int messageType, long clientId ) {
            this.messageType = messageType;
            this.clientId = clientId;
        }
    }
}
