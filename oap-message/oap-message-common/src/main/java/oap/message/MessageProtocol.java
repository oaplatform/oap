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
import oap.io.Resources;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

public final class MessageProtocol {
    public static final short PROTOCOL_VERSION_1 = 1;
    public static final byte[] RESERVED = new byte[8];
    public static final int RESERVED_LENGTH = RESERVED.length;
    public static final int MD5_LENGTH = 16;

    public static final byte EOF_MESSAGE_TYPE = ( byte ) 0xFF;

    public static final short STATUS_OK = 0;
    public static final short STATUS_UNKNOWN_ERROR = 1;
    public static final short STATUS_UNKNOWN_ERROR_NO_RETRY = 2;

    public static final short STATUS_UNKNOWN_MESSAGE_TYPE = 100;
    public static final short STATUS_ALREADY_WRITTEN = 101;
    private static final HashMap<Short, String> statusMap = new HashMap<>();
    private static final HashMap<Byte, String> typeMap = new HashMap<>();

    static {
        var properties = Resources.readAllProperties( "META-INF/oap-messages.properties" );
        for( var propertyName : properties.stringPropertyNames() ) {
            var key = propertyName.trim();
            if( key.startsWith( "type." ) ) {
                key = key.substring( 5 );

                MessageProtocol.typeMap.put( Byte.decode( properties.getProperty( propertyName ) ), key );
                continue;
            } else if( key.startsWith( "map." ) ) {
                key = key.substring( 4 );
            }

            MessageProtocol.statusMap.put( Short.parseShort( properties.getProperty( propertyName ) ), key );
        }
    }

    public static String messageStatusToString( short status ) {
        return switch( status ) {
            case STATUS_OK -> "OK";
            case STATUS_UNKNOWN_ERROR, STATUS_UNKNOWN_ERROR_NO_RETRY -> "UNKNOWN_ERROR";
            case STATUS_ALREADY_WRITTEN -> "ALREADY_WRITTEN";
            case STATUS_UNKNOWN_MESSAGE_TYPE -> "UNKNOWN_MESSAGE_TYPE";
            default -> {
                var str = statusMap.get( status );
                yield str != null ? str : "Unknown status: " + status;
            }
        };
    }

    public static String messageTypeToString( byte messageType ) {
        var str = typeMap.get( messageType );
        return str != null ? str : String.valueOf( messageType );
    }

    public static String printMapping() {
        return "status '" + statusMap + "' type '" + typeMap + "'";
    }

    public static String getStatus( short status ) {
        return statusMap.get( status );
    }

    @EqualsAndHashCode
    @ToString
    class ClientId implements Serializable {
        @Serial
        private static final long serialVersionUID = -6305024925123030053L;

        public final int messageType;
        public final long clientId;

        ClientId( int messageType, long clientId ) {
            this.messageType = messageType;
            this.clientId = clientId;
        }
    }
}
