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
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BufferTest {

    @Test
    public void data() throws IOException {
        Buffer buffer = new Buffer( 200, new LogId( "s", "l", "h", 1, 2 ) );
        assertTrue( buffer.putInt( 10 ) );
        assertTrue( buffer.putLong( 10 ) );
        assertTrue( buffer.putUTF( "aaaa" ) );
        assertTrue( buffer.putInt( 20 ) );
        assertTrue( buffer.putInt( 30 ) );
        buffer.close( 1 );

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( Arrays.copyOf( buffer.data(), buffer.length() ) ) );
        assertEquals( dis.readLong(), 1 );
        assertEquals( dis.readInt(), 26 );
        assertEquals( dis.readUTF(), "s" );
        assertEquals( dis.readUTF(), "l" );
        assertEquals( dis.readUTF(), "h" );
        assertEquals( dis.readInt(), 1 );
        assertEquals( dis.readInt(), 2 );
        assertEquals( dis.readInt(), 10 );
        assertEquals( dis.readLong(), 10 );
        assertEquals( dis.readUTF(), "aaaa" );
        assertEquals( dis.readInt(), 20 );
        assertEquals( dis.readInt(), 30 );
        assertEquals( dis.read(), -1 );
    }
}
