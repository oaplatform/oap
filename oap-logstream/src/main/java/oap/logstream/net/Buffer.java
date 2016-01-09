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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@ToString
@EqualsAndHashCode
class Buffer implements Serializable {
    private byte[] data;
    private int position = 0;

    public Buffer( int size ) {
        this.data = new byte[size];
    }

    public boolean put( byte[] buf ) {
        return put( buf, 0, buf.length );
    }

    public boolean put( byte[] buf, int offset, int length ) {
        if( !available( length ) ) return false;
        System.arraycopy( buf, offset, this.data, this.position, length );
        this.position += length;
        return true;
    }

    public boolean available( int length ) {
        return this.position + length <= this.data.length;
    }

    public byte[] data() {
        byte[] bytes = new byte[this.position];
        System.arraycopy( this.data, 0, bytes, 0, this.position );
        return bytes;
    }

    public void reset() {
        this.position = 0;
    }

    public boolean isEmpty() {
        return this.position == 0;
    }
}
