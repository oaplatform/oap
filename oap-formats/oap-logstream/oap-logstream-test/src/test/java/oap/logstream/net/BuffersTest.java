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
import oap.logstream.LogStreamProtocol;
import oap.template.Types;
import oap.util.Cuid;
import oap.util.Lists;
import oap.util.Pair;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static oap.logstream.LogStreamProtocol.ProtocolVersion.BINARY_V2;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;


public class BuffersTest {

    private static final String[] HEADERS = new String[] { "h1" };
    private static final byte[][] TYPES = new byte[][] { new byte[] { Types.STRING.id } };
    private int header;

    private static Pair<String, BufferConfigurationMap.BufferConfiguration> c( String name, String pattern, int size ) {
        return __( name, new BufferConfigurationMap.BufferConfiguration( size, Pattern.compile( pattern ) ) );
    }

    private static Buffer buffer( LogStreamProtocol.ProtocolVersion protocolVersion, int size, long id, LogId logId, byte[] data ) {
        Buffer buffer = new Buffer( size, logId, protocolVersion );
        buffer.put( data );
        buffer.close( id );
        return buffer;
    }

    private static void assertReadyData( Buffers buffers, List<Buffer> expectedData ) {
        Iterator<Buffer> expected = expectedData.iterator();
        buffers.forEachReadyData( b -> {
            Buffer next = expected.next();
            assertThat( Arrays.copyOf( b.data(), b.length() ) )
                .isEqualTo( Arrays.copyOf( next.data(), next.length() ) );
        } );
        assertThat( expected ).toIterable().isEmpty();
    }

    @BeforeClass
    public void beforeClass() {
        var buffer = new Buffer( 1024, new LogId( "x/y", "", "", Map.of(),
            HEADERS, TYPES ), BINARY_V2 );
        header = buffer.length();
    }

    @Test( expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "buffer size is too big: 2 for buffer of 33; headers = 32" )
    public void length() {
        Buffers.ReadyQueue.digestionIds = Cuid.incremental( 0 );
        try( Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 1 ) ) ) {
            buffers.put( new LogId( "x/y", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2 } );
        }
    }

    @Test
    public void foreach() {
        Buffers.ReadyQueue.digestionIds = Cuid.incremental( 0 );
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );
        buffers.put( new LogId( "x/y", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2, 3 } );
        buffers.put( new LogId( "x/z", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 11, 12, 13 } );
        buffers.put( new LogId( "x/y", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 4, 5, 6 } );
        buffers.put( new LogId( "x/y", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 7, 8, 9 } );
        buffers.put( new LogId( "x/z", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 14, 15 } );
        buffers.put( new LogId( "x/z", "", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 16 } );

        var expected = List.of(
            buffer( BINARY_V2, header + 4, 1, new LogId( "x/y", "", "", Map.of(),
                HEADERS, TYPES ), new byte[] { 1, 2, 3 } ),
            buffer( BINARY_V2, header + 4, 2, new LogId( "x/y", "", "", Map.of(),
                HEADERS, TYPES ), new byte[] { 4, 5, 6 } ),
            buffer( BINARY_V2, header + 4, 3, new LogId( "x/z", "", "", Map.of(),
                HEADERS, TYPES ), new byte[] { 11, 12, 13 } ),
            buffer( BINARY_V2, header + 4, 4, new LogId( "x/y", "", "", Map.of(),
                HEADERS, TYPES ), new byte[] { 7, 8, 9 } ),
            buffer( BINARY_V2, header + 4, 5, new LogId( "x/z", "", "", Map.of(),
                HEADERS, TYPES ), new byte[] { 14, 15, 16 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );
    }

    @Test
    public void foreachPattern() {
        Buffers.ReadyQueue.digestionIds = Cuid.incremental( 0 );
        Buffers buffers = new Buffers( BufferConfigurationMap.custom(
            c( "x_y", ".+y", header + 2 ),
            c( "x_z", ".+z", header + 4 )
        ) );
        buffers.put( new LogId( "", "x/y", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1 } );
        buffers.put( new LogId( "", "x/z", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 11, 12, 13 } );
        buffers.put( new LogId( "", "x/y", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 2 } );
        buffers.put( new LogId( "", "x/y", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 3 } );
        buffers.put( new LogId( "", "x/z", "", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 14, 15 } );

        var expected = List.of(
            buffer( BINARY_V2, header + 2, 1, new LogId( "", "x/y", "", Map.of(), HEADERS, TYPES ), new byte[] { 1, 2 } ),
            buffer( BINARY_V2, header + 4, 2, new LogId( "", "x/z", "", Map.of(), HEADERS, TYPES ), new byte[] { 11, 12, 13 } ),
            buffer( BINARY_V2, header + 4, 3, new LogId( "", "x/y", "", Map.of(), HEADERS, TYPES ), new byte[] { 3 } ),
            buffer( BINARY_V2, header + 2, 4, new LogId( "", "x/z", "", Map.of(), HEADERS, TYPES ), new byte[] { 14, 15 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );
    }
}
