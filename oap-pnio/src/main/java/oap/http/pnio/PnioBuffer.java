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

package oap.http.pnio;

import com.google.common.io.ByteStreams;
import oap.io.FixedLengthArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PnioBuffer {
    private final byte[] buffer;
    public int length;

    public PnioBuffer( int capacity ) {
        this.buffer = new byte[capacity];
        this.length = 0;
    }

    public final void copyFrom( InputStream inputStream ) throws IOException, BufferOverflowException {
        FixedLengthArrayOutputStream to = new FixedLengthArrayOutputStream( buffer );
        ByteStreams.copy( inputStream, to );
        length = to.size();
    }

    public final String string() {
        return new String( buffer, 0, length );
    }

    final byte[] array() {
        return buffer;
    }

    public final boolean isEmpty() {
        return length == 0;
    }

    public OutputStream getOutputStream() {
        return new PniOutputStream();
    }

    public void set( String data ) {
        byte[] bytes = data.getBytes( UTF_8 );
        System.arraycopy( bytes, 0, buffer, 0, bytes.length );
        length = bytes.length;
    }

    public class PniOutputStream extends FixedLengthArrayOutputStream {
        public PniOutputStream() {
            super( buffer );
        }

        @Override
        public void close() {
            super.close();
            length = size();
        }
    }
}
