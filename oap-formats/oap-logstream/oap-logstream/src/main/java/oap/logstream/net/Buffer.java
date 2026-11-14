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

import lombok.SneakyThrows;
import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.net.Inet;
import oap.util.FastByteArrayOutputStream;

import java.io.DataOutputStream;
import java.io.Serializable;
import java.util.Map;

public class Buffer implements Serializable {
    public static final int DIGESTION_POSITION = 0;
    public static final int DATE_LENGTH_POSITION = 8;
    public static final int COMPRESSION_POSITION = 12;
    public final ProtocolVersion protocolVersion;
    private final byte[] data;
    public LogId id;
    private int position = DIGESTION_POSITION;
    private volatile boolean closed = false;
    private int dataStart;

    Buffer( int size, LogId id, ProtocolVersion protocolVersion ) {
        this( size, id, protocolVersion, encodeHeader( id, Inet.HOSTNAME ) );
    }

    Buffer( int size, LogId id, ProtocolVersion protocolVersion, byte[] header ) {
        this.id = id;
        this.data = new byte[size];
        this.protocolVersion = protocolVersion;
        initMetadata( header );
    }

    /**
     * {@link ProtocolVersion#ROW_BINARY_V4} header: digestion control (long, reserved), data length (int, reserved),
     * compression (byte, reserved), filePrefixPattern, logType, clientHostname, headers, types, properties.
     * Strings - modified UTF-8 (DataInputStream.readUTF on the server).
     */
    @SneakyThrows
    public static byte[] encodeHeader( LogId id, String clientHostname ) {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream( baos );

        out.writeLong( 0 ); //reserved for digestion control
        out.writeInt( 0 ); //reserved for data length
        out.writeByte( 0 ); //reserved for compression
        out.writeUTF( id.filePrefixPattern );
        out.writeUTF( id.logType );
        out.writeUTF( clientHostname );
        out.writeInt( id.headers.length );
        for( String header : id.headers ) {
            out.writeUTF( header );
        }

        for( byte[] type : id.types ) {
            out.writeByte( type.length );
            out.write( type );
        }

        out.writeByte( id.properties.size() );
        for( Map.Entry<String, String> entry : id.properties.entrySet() ) {
            out.writeUTF( entry.getKey() );
            out.writeUTF( entry.getValue() );
        }
        out.flush();

        return baos.toByteArray();
    }

    private static void writeInt( byte[] array, int pos, int i ) {
        array[pos] = ( byte ) ( i >>> 24 );
        array[pos + 1] = ( byte ) ( i >>> 16 );
        array[pos + 2] = ( byte ) ( i >>> 8 );
        array[pos + 3] = ( byte ) i;
    }

    private static void writeLong( byte[] array, int pos, long v ) {
        array[pos] = ( byte ) ( v >>> 56 );
        array[pos + 1] = ( byte ) ( v >>> 48 );
        array[pos + 2] = ( byte ) ( v >>> 40 );
        array[pos + 3] = ( byte ) ( v >>> 32 );
        array[pos + 4] = ( byte ) ( v >>> 24 );
        array[pos + 5] = ( byte ) ( v >>> 16 );
        array[pos + 6] = ( byte ) ( v >>> 8 );
        array[pos + 7] = ( byte ) v;
    }

    private void initMetadata( byte[] header ) {
        if( header.length > data.length ) throw new IllegalArgumentException( "buffer is too small! Provided " + data.length + " bytes" );

        System.arraycopy( header, 0, data, 0, header.length );
        this.position = header.length;
        this.dataStart = header.length;
    }

    public final boolean put( byte[] buf ) {
        return put( buf, DIGESTION_POSITION, buf.length );
    }

    public final boolean put( byte[] buf, int offset, int length ) {
        if( !checkWrite( length ) ) return false;
        System.arraycopy( buf, offset, this.data, this.position, length );
        this.position += length;
        return true;
    }

    private boolean checkWrite( int length ) {
        if( closed ) throw new IllegalStateException( "buffer is closed" );
        return available( length );
    }

    public final boolean putInt( int i ) {
        if( !checkWrite( 4 ) ) return false;
        writeInt( data, position, i );
        position += 4;
        return true;
    }

    public final boolean putByte( byte i ) {
        if( !checkWrite( 1 ) ) return false;
        data[position++] = i;
        return true;
    }

    public final boolean putLong( long v ) {
        if( !checkWrite( 8 ) ) return false;
        writeLong( data, position, v );
        position += 8;
        return true;
    }

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    public final boolean putUTF( String str ) {
        int strlen = str.length();
        int utflen = 0;
        int c;

        /* use charAt instead of copying String to char array */
        for( int i = 0; i < strlen; i++ ) {
            c = str.charAt( i );
            if( ( c >= 0x0001 ) && ( c <= 0x007F ) ) utflen++;
            else if( c > 0x07FF ) utflen += 3;
            else utflen += 2;
        }

        if( !checkWrite( utflen + 2 ) ) return false;

        byte[] buffer = this.data;
        int count = position;

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

        position = count;
        return true;
    }

    public final boolean available( int length ) {
        return this.position + length <= this.data.length;
    }

    public final byte[] data() {
        return this.data;
    }

    public final void reset( LogId id ) {
        reset( id, encodeHeader( id, Inet.HOSTNAME ) );
    }

    final void reset( LogId id, byte[] header ) {
        this.closed = false;
        this.id = id;
        initMetadata( header );
    }

    public final boolean isEmpty() {
        return dataLength() == DIGESTION_POSITION;
    }

    public final int length() {
        return position;
    }

    /**
     * size of the underlying array (the configured buffer size)
     */
    public final int capacity() {
        return data.length;
    }

    public final void close( long digestionId ) {
        this.closed = true;
        writeLong( this.data, DIGESTION_POSITION, digestionId );
        writeInt( this.data, DATE_LENGTH_POSITION, dataLength() );
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
