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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;

import static org.joda.time.DateTimeZone.UTC;

public class BinaryOutputStream extends OutputStream {
    /**
     * bytearr is initialized on demand by writeUTF
     */
    private byte[] bytearr = null;
    protected final byte[] writeBuffer = new byte[9];

    protected final OutputStream out;

    public BinaryOutputStream( OutputStream out ) {
        this.out = out;
    }

    @Override
    public void write( int i ) throws IOException {
        out.write( i );
    }

    public void writeByte( byte v ) throws IOException {
        writeBuffer[0] = Types.BYTE.id;
        writeBuffer[1] = v;

        out.write( writeBuffer, 0, 2 );
    }

    public void writeBoolean( boolean v ) throws IOException {
        writeBuffer[0] = Types.BOOLEAN.id;
        writeBuffer[1] = v ? ( byte ) 1 : ( byte ) 0;

        out.write( writeBuffer, 0, 2 );
    }

    public void writeShort( short v ) throws IOException {
        writeBuffer[0] = Types.SHORT.id;
        writeBuffer[1] = ( byte ) ( v >>> 8 );
        writeBuffer[2] = ( byte ) ( v >>> 0 );

        out.write( writeBuffer, 0, 3 );
    }

    public void writeInt( int v ) throws IOException {
        writeBuffer[0] = Types.INTEGER.id;

        _writeInt( v );
    }

    public void writeList( Collection<?> v ) throws IOException {
        writeBuffer[0] = Types.LIST.id;
        if( v == null || v.isEmpty() ) _writeInt( 0 );
        else {
            _writeInt( v.size() );

            for( var item : v ) {
                writeObject( item );
            }
        }
    }

    public void writeObject( Object v ) throws IOException {
        if( v instanceof String s ) writeString( s );
        else if( v instanceof Boolean b ) writeBoolean( b );
        else if( v instanceof Byte b ) writeByte( b );
        else if( v instanceof Short s ) writeShort( s );
        else if( v instanceof Integer i ) writeInt( i );
        else if( v instanceof Long l ) writeLong( l );
        else if( v instanceof Float f ) writeFloat( f );
        else if( v instanceof Double d ) writeDouble( d );
        else if( v instanceof DateTime dt ) writeDateTime( dt );
        else if( v instanceof Date d ) writeDateTime( new DateTime( d, UTC ) );
        else if( v instanceof Collection<?> c ) writeList( c );
        else
            throw new IllegalArgumentException( "Unknown type " + v.getClass() );
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected void _writeInt( int v ) throws IOException {
        writeBuffer[1] = ( byte ) ( v >>> 24 );
        writeBuffer[2] = ( byte ) ( v >>> 16 );
        writeBuffer[3] = ( byte ) ( v >>> 8 );
        writeBuffer[4] = ( byte ) ( v >>> 0 );

        out.write( writeBuffer, 0, 5 );
    }

    public void writeLong( long v ) throws IOException {
        writeBuffer[0] = Types.LONG.id;
        _writeLong( v );
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected void _writeLong( long v ) throws IOException {
        writeBuffer[1] = ( byte ) ( v >>> 56 );
        writeBuffer[2] = ( byte ) ( v >>> 48 );
        writeBuffer[3] = ( byte ) ( v >>> 40 );
        writeBuffer[4] = ( byte ) ( v >>> 32 );
        writeBuffer[5] = ( byte ) ( v >>> 24 );
        writeBuffer[6] = ( byte ) ( v >>> 16 );
        writeBuffer[7] = ( byte ) ( v >>> 8 );
        writeBuffer[8] = ( byte ) ( v >>> 0 );

        out.write( writeBuffer, 0, 9 );
    }

    public void writeFloat( float v ) throws IOException {
        writeBuffer[0] = Types.FLOAT.id;

        _writeInt( Float.floatToIntBits( v ) );
    }

    public void writeDouble( double v ) throws IOException {
        writeBuffer[0] = Types.DOUBLE.id;

        _writeLong( Double.doubleToLongBits( v ) );
    }

    public void writeDateTime( DateTime jodaDateTime ) throws IOException {
        writeBuffer[0] = Types.DATETIME.id;

        _writeLong( jodaDateTime.getMillis() );
    }

    public void writeString( @NotNull String str ) throws IOException {
        writeUTFWithType( str, Types.STRING );
    }

    private void writeUTFWithType( @NotNull String str, Types type ) throws IOException {
        final int strlen = str.length();
        int utflen = strlen; // optimized for ASCII

        for( int i = 0; i < strlen; i++ ) {
            int c = str.charAt( i );
            if( c >= 0x80 || c == 0 )
                utflen += ( c >= 0x800 ) ? 2 : 1;
        }

        if( bytearr == null || ( bytearr.length < ( utflen + 2 + 1 ) ) )
            bytearr = new byte[( utflen * 2 ) + 2 + 1];

        bytearr[0] = type.id;
        int count = 1;
        bytearr[count++] = ( byte ) ( ( utflen >>> 8 ) & 0xFF );
        bytearr[count++] = ( byte ) ( ( utflen >>> 0 ) & 0xFF );

        int i;
        for( i = 0; i < strlen; i++ ) { // optimized for initial run of ASCII
            int c = str.charAt( i );
            if( c >= 0x80 || c == 0 ) break;
            bytearr[count++] = ( byte ) c;
        }

        for( ; i < strlen; i++ ) {
            int c = str.charAt( i );
            if( c < 0x80 && c != 0 ) {
                bytearr[count++] = ( byte ) c;
            } else if( c >= 0x800 ) {
                bytearr[count++] = ( byte ) ( 0xE0 | ( ( c >> 12 ) & 0x0F ) );
                bytearr[count++] = ( byte ) ( 0x80 | ( ( c >> 6 ) & 0x3F ) );
                bytearr[count++] = ( byte ) ( 0x80 | ( ( c >> 0 ) & 0x3F ) );
            } else {
                bytearr[count++] = ( byte ) ( 0xC0 | ( ( c >> 6 ) & 0x1F ) );
                bytearr[count++] = ( byte ) ( 0x80 | ( ( c >> 0 ) & 0x3F ) );
            }
        }
        out.write( bytearr, 0, utflen + 3 );
    }
}
