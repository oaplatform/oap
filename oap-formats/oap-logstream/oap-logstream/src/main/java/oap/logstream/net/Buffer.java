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
package oap.logstream.net;

import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol.ProtocolVersion;

import java.io.Serializable;

class Buffer implements Serializable {
    public final LogId id;
    public final ProtocolVersion protocolVersion;
    private final byte[] data;
    private int position = 0;
    private volatile boolean closed = false;
    private int dataStart;

    Buffer( int size, LogId id, ProtocolVersion protocolVersion ) {
        this.id = id;
        this.data = new byte[size];
        this.protocolVersion = protocolVersion;
        initMetadata( id );
    }

    private void initMetadata( LogId id ) {
        if( position != 0 ) throw new IllegalStateException( "metadata could be set for empty buffer only!" );
        var result = putLong( 0 ); //reserved for digestion control
        result &= putInt( 0 ); //reserved for data length
        result &= putUTF( id.filePrefixPattern );
        result &= putUTF( id.logType );
        result &= putUTF( id.clientHostname );
        result &= putInt( id.headers.length );
        for( var header : id.headers )
            result &= putUTF( header );

        for( var type : id.types ) {
            result &= putByte( ( byte ) type.length );
            for( var t : type ) {
                result &= putByte( t );
            }
        }
        result &= putByte( ( byte ) id.properties.size() );

        id.properties.forEach( ( key, value ) -> {
            putUTF( key );
            putUTF( value );
        } );
        this.dataStart = this.position;
        if( !result ) throw new IllegalArgumentException( "buffer is too small! Provided " + data.length + " bytes" );
    }

    public final boolean put( byte[] buf ) {
        return put( buf, 0, buf.length );
    }

    public final boolean put( byte[] buf, int offset, int length ) {
        if( closed ) throw new IllegalStateException( "buffer is closed" );
        if( !available( length ) ) return false;
        System.arraycopy( buf, offset, this.data, this.position, length );
        this.position += length;
        return true;
    }

    public final boolean putInt( int i ) {
        return put( encodeInt( i ) );
    }

    public final boolean putByte( byte i ) {
        return put( new byte[] { i } );
    }

    private byte[] encodeInt( int i ) {
        return new byte[] {
            ( byte ) ( ( i >>> 24 ) & 0xFF ),
            ( byte ) ( ( i >>> 16 ) & 0xFF ),
            ( byte ) ( ( i >>> 8 ) & 0xFF ),
            ( byte ) ( i & 0xFF )
        };
    }

    public final boolean putLong( long v ) {
        return put( encodeLong( v ) );
    }

    private byte[] encodeLong( long v ) {
        return new byte[] {
            ( byte ) ( v >>> 56 ),
            ( byte ) ( v >>> 48 ),
            ( byte ) ( v >>> 40 ),
            ( byte ) ( v >>> 32 ),
            ( byte ) ( v >>> 24 ),
            ( byte ) ( v >>> 16 ),
            ( byte ) ( v >>> 8 ),
            ( byte ) v
        };
    }

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    public final boolean putUTF( String str ) {
        int strlen = str.length();
        int utflen = 0;
        int c, count = 0;

        /* use charAt instead of copying String to char array */
        for( int i = 0; i < strlen; i++ ) {
            c = str.charAt( i );
            if( ( c >= 0x0001 ) && ( c <= 0x007F ) ) utflen++;
            else if( c > 0x07FF ) utflen += 3;
            else utflen += 2;
        }

        if( !available( utflen ) ) return false;

        byte[] buffer = new byte[utflen + 2];

        buffer[count++] = ( byte ) ( ( utflen >>> 8 ) & 0xFF );
        buffer[count++] = ( byte ) ( utflen & 0xFF );

        int i;
        for( i = 0; i < strlen; i++ ) {
            c = str.charAt( i );
            if( !( ( c >= 0x0001 ) && ( c <= 0x007F ) ) ) break;
            buffer[count++] = ( byte ) c;
        }

        for( ; i < strlen; i++ ) {
            c = str.charAt( i );
            if( ( c >= 0x0001 ) && ( c <= 0x007F ) ) {
                buffer[count++] = ( byte ) c;
            } else if( c > 0x07FF ) {
                buffer[count++] = ( byte ) ( 0xE0 | ( ( c >> 12 ) & 0x0F ) );
                buffer[count++] = ( byte ) ( 0x80 | ( ( c >> 6 ) & 0x3F ) );
                buffer[count++] = ( byte ) ( 0x80 | ( c & 0x3F ) );
            } else {
                buffer[count++] = ( byte ) ( 0xC0 | ( ( c >> 6 ) & 0x1F ) );
                buffer[count++] = ( byte ) ( 0x80 | ( c & 0x3F ) );
            }
        }
        return put( buffer, 0, utflen + 2 );
    }

    public final boolean available( int length ) {
        return this.position + length <= this.data.length;
    }

    public final byte[] data() {
        return this.data;
    }

    public final void reset( LogId id ) {
        this.closed = false;
        this.position = 0;
        initMetadata( id );
    }

    public final boolean isEmpty() {
        return dataLength() == 0;
    }

    public final int length() {
        return position;
    }

    public final void close( long digestionId ) {
        this.closed = true;
        byte[] digestion = encodeLong( digestionId );
        byte[] length = encodeInt( dataLength() );
        System.arraycopy( digestion, 0, this.data, 0, digestion.length );
        System.arraycopy( length, 0, this.data, 8, length.length );
    }

    public final int dataLength() {
        return position - dataStart;
    }

    public final int headerLength() {
        return dataStart;
    }

    @Override
    public final String toString() {
        return id + "," + position;
    }
}
