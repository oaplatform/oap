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
import oap.util.Cuid;
import oap.util.Lists;
import oap.util.Pair;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;


public class BuffersTest {

    public static final int HEADER = 17;

    private static Pair<String, BufferConfigurationMap.BufferConfiguration> c( String name, String pattern, int size ) {
        return __( name, new BufferConfigurationMap.BufferConfiguration( size, Pattern.compile( pattern ) ) );
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
            assertThat( Arrays.copyOf( b.data(), b.length() ) ).isEqualTo( Arrays.copyOf( next.data(), next.length() ) );
            return true;
        } );
        assertThat( expected ).isEmpty();
    }

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "buffer size is too big: 2 for buffer of 18" )
    public void testLength() {
        Buffers.ReadyQueue.digestionIds = new Cuid.SeedCounter( 0 );
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.DEFAULT( HEADER + 1 ) );
        buffers.put( "x/y", new byte[] { 1, 2 } );
    }

    @Test
    public void foreach() {
        Buffers.ReadyQueue.digestionIds = new Cuid.SeedCounter( 0 );
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.DEFAULT( HEADER + 4 ) );
        buffers.put( "x/y", new byte[] { 1, 2, 3 } );
        buffers.put( "x/z", new byte[] { 11, 12, 13 } );
        buffers.put( "x/y", new byte[] { 4, 5, 6 } );
        buffers.put( "x/y", new byte[] { 7, 8, 9 } );
        buffers.put( "x/z", new byte[] { 14, 15 } );
        buffers.put( "x/z", new byte[] { 16 } );

        ArrayList<Buffer> expected = Lists.of(
            buffer( HEADER + 4, 1, "x/y", new byte[] { 1, 2, 3 } ),
            buffer( HEADER + 4, 2, "x/y", new byte[] { 4, 5, 6 } ),
            buffer( HEADER + 4, 3, "x/z", new byte[] { 11, 12, 13 } ),
            buffer( HEADER + 4, 4, "x/z", new byte[] { 14, 15, 16 } ),
            buffer( HEADER + 4, 5, "x/y", new byte[] { 7, 8, 9 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );
    }

    @Test
    public void foreach_pattern() {
        Buffers.ReadyQueue.digestionIds = new Cuid.SeedCounter( 0 );
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.custom(
            c( "x_y", ".+y", HEADER + 2 ),
            c( "x_z", ".+z", HEADER + 4 )
        ) );
        buffers.put( "x/y", new byte[] { 1 } );
        buffers.put( "x/z", new byte[] { 11, 12, 13 } );
        buffers.put( "x/y", new byte[] { 2 } );
        buffers.put( "x/y", new byte[] { 3 } );
        buffers.put( "x/z", new byte[] { 14, 15 } );

        ArrayList<Buffer> expected = Lists.of(
            buffer( HEADER + 2, 1, "x/y", new byte[] { 1, 2 } ),
            buffer( HEADER + 4, 2, "x/z", new byte[] { 11, 12, 13 } ),
            buffer( HEADER + 4, 3, "x/z", new byte[] { 14, 15 } ),
            buffer( HEADER + 2, 4, "x/y", new byte[] { 3 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );
    }


    @Test
    public void persistence() {
        Buffers.ReadyQueue.digestionIds = new Cuid.SeedCounter( 0 );
        Buffers buffers = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.DEFAULT( HEADER + 4 ) );
        buffers.put( "x/y", new byte[] { 1, 2, 3 } );
        buffers.put( "x/z", new byte[] { 11, 12, 13 } );
        buffers.put( "x/y", new byte[] { 4, 5, 6 } );
        buffers.put( "x/y", new byte[] { 7, 8, 9 } );
        buffers.put( "x/z", new byte[] { 14, 15, 16 } );
        buffers.close();

        ArrayList<Buffer> expected = Lists.of(
            buffer( HEADER + 4, 1, "x/y", new byte[] { 1, 2, 3 } ),
            buffer( HEADER + 4, 2, "x/y", new byte[] { 4, 5, 6 } ),
            buffer( HEADER + 4, 3, "x/z", new byte[] { 11, 12, 13 } ),
            buffer( HEADER + 4, 4, "x/z", new byte[] { 14, 15, 16 } ),
            buffer( HEADER + 4, 5, "x/y", new byte[] { 7, 8, 9 } )
        );
        Buffers buffers2 = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.DEFAULT( HEADER + 4 ) );
        assertReadyData( buffers2, expected );

        Buffers buffers3 = new Buffers( Env.tmpPath( "bfrs" ), BufferConfigurationMap.DEFAULT( HEADER + 4 ) );
        assertReadyData( buffers3, Lists.empty() );
    }


}
