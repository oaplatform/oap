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

package oap.io;

import java.io.IOException;
import java.io.OutputStream;

public class DataOutputStream extends java.io.DataOutputStream {
    private final byte[] writeBuffer = new byte[8];

    public DataOutputStream( OutputStream out ) {
        super( out );
    }

    public void writeVarInt( int value ) throws IOException {
        if( ( value & ( 0xFFFFFFFF << 7 ) ) == 0 ) {
            writeByte( value );
        } else if( ( value & ( 0xFFFFFFFF << 14 ) ) == 0 ) {
            int w = ( value & 0x7F | 0x80 ) << 8 | ( value >>> 7 );
            writeShort( w );
        } else if( ( value & ( 0xFFFFFFFF << 21 ) ) == 0 ) {
            int w = ( value & 0x7F | 0x80 ) << 16 | ( ( value >>> 7 ) & 0x7F | 0x80 ) << 8 | ( value >>> 14 );
            writeMedium( w );
        } else if( ( value & ( 0xFFFFFFFF << 28 ) ) == 0 ) {
            int w = ( value & 0x7F | 0x80 ) << 24 | ( ( ( value >>> 7 ) & 0x7F | 0x80 ) << 16 )
                | ( ( value >>> 14 ) & 0x7F | 0x80 ) << 8 | ( value >>> 21 );
            writeInt( w );
        } else {
            int w = ( value & 0x7F | 0x80 ) << 24 | ( ( value >>> 7 ) & 0x7F | 0x80 ) << 16 | ( ( value >>> 14 ) & 0x7F | 0x80 ) << 8
                | ( ( value >>> 21 ) & 0x7F | 0x80 );
            writeInt( w );
            writeByte( value >>> 28 );
        }
    }

    public void writeMedium( int value ) throws IOException {
        writeBuffer[0] = ( byte ) ( value >>> 16 );
        writeBuffer[1] = ( byte ) ( value >>> 8 );
        writeBuffer[2] = ( byte ) value;

        write( writeBuffer, 0, 3 );
    }
}
