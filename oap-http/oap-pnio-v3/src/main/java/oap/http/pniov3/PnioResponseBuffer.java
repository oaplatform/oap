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

package oap.http.pniov3;

import oap.io.FixedLengthArrayOutputStream;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@NotThreadSafe
public class PnioResponseBuffer {
    public int length;
    public byte[] buffer;

    public PnioResponseBuffer( int capacity ) {
        this.buffer = new byte[capacity];
        this.length = 0;
    }

    public String string() {
        return new String( buffer, 0, length, UTF_8 );
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public OutputStream getOutputStream() {
        return new PnioOutputStream();
    }

    public void setAndResize( String data ) {
        byte[] bytes = data.getBytes( UTF_8 );
        setAndResize( bytes );
    }

    public void setAndResize( byte[] bytes, int length ) {
        this.buffer = bytes;
        this.length = length;
    }

    public void setAndResize( byte[] bytes ) {
        setAndResize( bytes, bytes.length );
    }

    public void setEmpty() {
        this.length = 0;
    }

    class PnioOutputStream extends FixedLengthArrayOutputStream {
        PnioOutputStream() {
            super( buffer );
        }

        @Override
        public void close() {
            super.close();
            length = size();
        }
    }
}
