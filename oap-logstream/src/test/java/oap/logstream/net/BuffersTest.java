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
import oap.util.Pair;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;


public class BuffersTest {
    @Test
    public void persistence() {
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), 4 );
        buffers.put( "x/y", new byte[]{ 1, 2, 3 } );
        buffers.put( "x/z", new byte[]{ 11, 12, 13 } );
        buffers.put( "x/y", new byte[]{ 4, 5, 6 } );
        buffers.put( "x/y", new byte[]{ 7, 8, 9 } );
        buffers.put( "x/z", new byte[]{ 14, 15, 16 } );
        buffers.close();

        ArrayList<Pair<String, byte[]>> expected = Lists.of(
            __( "x/y", new byte[]{ 1, 2, 3 } ),
            __( "x/y", new byte[]{ 4, 5, 6 } ),
            __( "x/z", new byte[]{ 11, 12, 13 } ),
            __( "x/z", new byte[]{ 14, 15, 16 } ),
            __( "x/y", new byte[]{ 7, 8, 9 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );

        Buffers buffers2 = new Buffers( Env.tmpPath( "bfrs" ), 4 );
        assertReadyData( buffers2, expected );

        Buffers buffers3 = new Buffers( Env.tmpPath( "bfrs" ), 4 );
        assertReadyData( buffers3, Lists.empty() );
    }

    private void assertReadyData( Buffers buffers, ArrayList<Pair<String, byte[]>> expectedData ) {
        Iterator<Pair<String, byte[]>> expected = expectedData.iterator();
        buffers.forEachReadyData( bucket -> {
            Pair<String, byte[]> next = expected.next();
            assertEquals( bucket.selector, next._1 );
            assertEquals( Arrays.copyOf( bucket.buffer.data(), bucket.buffer.length() ), next._2 );
            return true;
        } );
        assertFalse( expected.hasNext() );
    }


}
