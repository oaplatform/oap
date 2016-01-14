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

import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;


public class BuffersTest {
    @Test
    public void foreach() {
        Buffers.ReadyQueue.digestionIds = 0;
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), 18 + 4 );
        buffers.put( "x/y", new byte[]{ 1, 2, 3 } );
        buffers.put( "x/z", new byte[]{ 11, 12, 13 } );
        buffers.put( "x/y", new byte[]{ 4, 5, 6 } );
        buffers.put( "x/y", new byte[]{ 7, 8, 9 } );
        buffers.put( "x/z", new byte[]{ 14, 15, 16 } );

        ArrayList<Buffer> expected = Lists.of(
            buffer( 18 + 4, 0, "x/y", new byte[]{ 1, 2, 3 } ),
            buffer( 18 + 4, 1, "x/y", new byte[]{ 4, 5, 6 } ),
            buffer( 18 + 4, 2, "x/z", new byte[]{ 11, 12, 13 } ),
            buffer( 18 + 4, 3, "x/z", new byte[]{ 14, 15, 16 } ),
            buffer( 18 + 4, 4, "x/y", new byte[]{ 7, 8, 9 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );

    }


    @Test
    public void persistence() {
        Buffers.ReadyQueue.digestionIds = 0;
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), 18 + 4 );
        buffers.put( "x/y", new byte[]{ 1, 2, 3 } );
        buffers.put( "x/z", new byte[]{ 11, 12, 13 } );
        buffers.put( "x/y", new byte[]{ 4, 5, 6 } );
        buffers.put( "x/y", new byte[]{ 7, 8, 9 } );
        buffers.put( "x/z", new byte[]{ 14, 15, 16 } );
        buffers.close();

        ArrayList<Buffer> expected = Lists.of(
            buffer( 18 + 4, 0, "x/y", new byte[]{ 1, 2, 3 } ),
            buffer( 18 + 4, 1, "x/y", new byte[]{ 4, 5, 6 } ),
            buffer( 18 + 4, 2, "x/z", new byte[]{ 11, 12, 13 } ),
            buffer( 18 + 4, 3, "x/z", new byte[]{ 14, 15, 16 } ),
            buffer( 18 + 4, 4, "x/y", new byte[]{ 7, 8, 9 } )
        );
        Buffers buffers2 = new Buffers( Env.tmpPath( "bfrs" ), 18 + 4 );
        assertReadyData( buffers2, expected );

        Buffers buffers3 = new Buffers( Env.tmpPath( "bfrs" ), 18 + 4 );
        assertReadyData( buffers3, Lists.empty() );
    }

    private static Buffer buffer( int size, long id, String selector, byte[] data ) {
        Buffer buffer = new Buffer( size, selector );
        buffer.put( data );
        buffer.close( id );
        return buffer;
    }

    private static void assertReadyData( Buffers buffers, ArrayList<Buffer> expectedData ) {
        Iterator<Buffer> expected = expectedData.iterator();
        buffers.forEachReadyData( b -> {
            Buffer next = expected.next();
            assertEquals( Arrays.copyOf( b.data(), b.length() ), Arrays.copyOf( next.data(), next.length() ) );
            return true;
        } );
        assertFalse( expected.hasNext() );
    }


}
