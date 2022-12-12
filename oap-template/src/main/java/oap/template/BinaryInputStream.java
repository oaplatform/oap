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

package oap.template;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.joda.time.DateTimeZone.UTC;

public class BinaryInputStream extends InputStream {
    protected byte[] readBuffer = new byte[8];
    protected byte[] bytearr = new byte[80];
    protected char[] chararr = new char[80];

    protected final InputStream in;

    public BinaryInputStream( InputStream in ) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    public boolean readBoolean() throws IOException {
        checkType( Types.BOOLEAN );

        return _readBoolean();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected boolean _readBoolean() throws IOException {
        int ch = in.read();
        if( ch < 0 )
            throw new EOFException();
        return ch != 0;
    }

    public byte readByte() throws IOException {
        checkType( Types.BYTE );

        return _readByte();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected byte _readByte() throws IOException {
        int ch = in.read();
        if( ch < 0 )
            throw new EOFException();
        return ( byte ) ch;
    }

    public short readShort() throws IOException {
        checkType( Types.SHORT );

        return _readShort();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected short _readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if( ( ch1 | ch2 ) < 0 )
            throw new EOFException();
        return ( short ) ( ( ch1 << 8 ) + ( ch2 << 0 ) );
    }

    public int readInt() throws IOException {
        checkType( Types.INTEGER );

        return _readInt();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected int _readInt() throws IOException {
        readFully( readBuffer, 0, 4 );

        return ( readBuffer[0] << 24 )
            + ( ( readBuffer[1] & 255 ) << 16 )
            + ( ( readBuffer[2] & 255 ) << 8 )
            + ( ( readBuffer[3] & 255 ) << 0 );
    }

    public long readLong() throws IOException {
        checkType( Types.LONG );

        return _readLong();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected long _readLong() throws IOException {
        readFully( readBuffer, 0, 8 );

        return ( ( long ) readBuffer[0] << 56 )
            + ( ( long ) ( readBuffer[1] & 255 ) << 48 )
            + ( ( long ) ( readBuffer[2] & 255 ) << 40 )
            + ( ( long ) ( readBuffer[3] & 255 ) << 32 )
            + ( ( long ) ( readBuffer[4] & 255 ) << 24 )
            + ( ( readBuffer[5] & 255 ) << 16 )
            + ( ( readBuffer[6] & 255 ) << 8 )
            + ( ( readBuffer[7] & 255 ) << 0 );
    }

    public float readFloat() throws IOException {
        checkType( Types.FLOAT );

        return _readFloat();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected float _readFloat() throws IOException {
        return Float.intBitsToFloat( _readInt() );
    }

    public double readDouble() throws IOException {
        checkType( Types.DOUBLE );

        return _readDouble();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected double _readDouble() throws IOException {
        return Double.longBitsToDouble( _readLong() );
    }

    public DateTime readDateTime() throws IOException {
        checkType( Types.DATETIME );

        return _readDateTime();
    }

    @NotNull
    @SuppressWarnings( "checkstyle:MethodName" )
    protected DateTime _readDateTime() throws IOException {
        return new DateTime( _readLong(), UTC );
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    public String readString() throws IOException {
        checkType( Types.STRING );

        return _readString();

    }

    @NotNull
    @SuppressWarnings( { "checkstyle:MethodName", "checkstyle:OperatorWrap" } )
    private String _readString() throws IOException {
        int utflen = readUnsignedShort();
        if( bytearr.length < utflen ) {
            bytearr = new byte[utflen * 2];
            chararr = new char[utflen * 2];
        }

        int c, char2, char3;
        int count = 0;
        int chararrCount = 0;

        readFully( bytearr, 0, utflen );

        while( count < utflen ) {
            c = ( int ) bytearr[count] & 0xff;
            if( c > 127 ) break;
            count++;
            chararr[chararrCount++] = ( char ) c;
        }

        while( count < utflen ) {
            c = ( int ) bytearr[count] & 0xff;
            switch( c >> 4 ) {
                case 0, 1, 2, 3, 4, 5, 6, 7 -> {
                    /* 0xxxxxxx*/
                    count++;
                    chararr[chararrCount++] = ( char ) c;
                }
                case 12, 13 -> {
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if( count > utflen )
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end" );
                    char2 = bytearr[count - 1];
                    if( ( char2 & 0xC0 ) != 0x80 )
                        throw new UTFDataFormatException(
                            "malformed input around byte " + count );
                    chararr[chararrCount++] = ( char ) ( ( ( c & 0x1F ) << 6 ) |
                        ( char2 & 0x3F ) );
                }
                case 14 -> {
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if( count > utflen )
                        throw new UTFDataFormatException(
                            "malformed input: partial character at end" );
                    char2 = bytearr[count - 2];
                    char3 = bytearr[count - 1];
                    if( ( ( char2 & 0xC0 ) != 0x80 ) || ( ( char3 & 0xC0 ) != 0x80 ) )
                        throw new UTFDataFormatException(
                            "malformed input around byte " + ( count - 1 ) );
                    chararr[chararrCount++] = ( char ) ( ( ( c & 0x0F ) << 12 ) |
                        ( ( char2 & 0x3F ) << 6 ) |
                        ( ( char3 & 0x3F ) << 0 ) );
                }
                default ->
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                        "malformed input around byte " + count );
            }
        }
        // The number of chars produced may be less than utflen
        return new String( chararr, 0, chararrCount );
    }

    public List<?> readList() throws IOException {
        checkType( Types.LIST );

        return _readList();
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    @NotNull
    protected ArrayList<Object> _readList() throws IOException {
        ArrayList<Object> ret = new ArrayList<>();

        int size = _readInt();

        for( int i = 0; i < size; i++ ) {
            ret.add( readObject() );
        }

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    public <T> T readObject() throws IOException {
        int type = in.read();

        if( type == Types.BOOLEAN.id ) return ( T ) ( Object ) _readBoolean();
        else if( type == Types.BYTE.id ) return ( T ) ( Object ) _readByte();
        else if( type == Types.SHORT.id ) return ( T ) ( Object ) _readShort();
        else if( type == Types.INTEGER.id ) return ( T ) ( Object ) _readInt();
        else if( type == Types.LONG.id ) return ( T ) ( Object ) _readLong();
        else if( type == Types.FLOAT.id ) return ( T ) ( Object ) _readFloat();
        else if( type == Types.DOUBLE.id ) return ( T ) ( Object ) _readDouble();
        else if( type == Types.STRING.id ) return ( T ) _readString();
        else if( type == Types.DATETIME.id ) return ( T ) _readDateTime();
        else if( type == Types.LIST.id ) return ( T ) _readList();

        throw new IllegalArgumentException( "Unknown type: " + type );
    }

    protected void checkType( Types type ) throws IOException {
        byte readType = ( byte ) in.read();

        if( readType != type.id )
            throw new IOException( "required :" + type.name() + ":" + type.id + ", but found: " + readType );
    }

    @SuppressWarnings( "checkstyle:ArrayTypeStyle" )
    protected void readFully( byte b[], int off, int len ) throws IOException {
        Objects.checkFromIndexSize( off, len, b.length );
        int n = 0;
        while( n < len ) {
            int count = in.read( b, off + n, len - n );
            if( count < 0 )
                throw new EOFException();
            n += count;
        }
    }

    protected int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if( ( ch1 | ch2 ) < 0 )
            throw new EOFException();
        return ( ch1 << 8 ) + ( ch2 << 0 );
    }
}
