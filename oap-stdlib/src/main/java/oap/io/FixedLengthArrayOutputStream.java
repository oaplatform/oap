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

import java.io.OutputStream;
import java.nio.BufferOverflowException;

public class FixedLengthArrayOutputStream extends OutputStream {
    private final byte[] bytes;
    private int position;

    public FixedLengthArrayOutputStream( byte[] bytes ) {
        this.bytes = bytes;
    }

    @Override
    public void write( int b ) throws BufferOverflowException {
        checkSize( 1 );
        bytes[position++] = ( byte ) b;
    }

    @Override
    public void write( byte[] b ) throws BufferOverflowException {
        int length = b.length;
        checkSize( length );

        System.arraycopy( b, 0, bytes, position, length );
        position += length;
    }

    @Override
    public void write( byte[] b, int off, int len ) throws BufferOverflowException {
        checkSize( len );

        System.arraycopy( b, off, bytes, position, len );
        position += len;
    }

    private void checkSize( int count ) throws BufferOverflowException {
        if( bytes.length < position + count ) {
            throw new BufferOverflowException();
        }
    }

    public int size() {
        return position;
    }

    public byte[] array() {
        return bytes;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
