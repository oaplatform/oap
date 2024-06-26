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

package oap.util;

import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;

import java.io.InputStream;

public class FastByteArrayOutputStream extends it.unimi.dsi.fastutil.io.FastByteArrayOutputStream {
    public FastByteArrayOutputStream() {
    }

    public FastByteArrayOutputStream( int initialCapacity ) {
        super( initialCapacity );
    }

    public FastByteArrayOutputStream( byte[] a ) {
        super( a );
    }

    public FastByteArrayOutputStream( byte[] a, long position, long length ) {
        super( a );
        this.position( position );
        this.length = ( int ) length;
    }

    public InputStream getInputStream() {
        return new FastByteArrayInputStream( array, 0, length );
    }

    @Override
    public String toString() {
        return new String( array, 0, length );
    }

    public byte[] toByteArray() {
        var ret = new byte[length];
        System.arraycopy( array, 0, ret, 0, length );
        return ret;
    }
}
