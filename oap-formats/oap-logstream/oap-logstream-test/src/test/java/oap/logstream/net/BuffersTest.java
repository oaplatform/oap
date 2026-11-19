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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.regex.Pattern;

import static oap.logstream.LogStreamProtocol.ProtocolVersion.BINARY_V2;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;


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
        var buffer = new Buffer( 1024, new LogId( "x/y", Map.of(),
            HEADERS, TYPES ), BINARY_V2 );
        header = buffer.length();
    }

    @Test
    public void length() {
        Buffers.ReadyQueue.digestionIds = Cuid.incremental( 0 );
        try( Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 1 ) ) ) {
            assertThatThrownBy( () -> buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2 } ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "buffer size is too big: 2 for buffer of " + ( header + 1 ) + "; headers = " + header );
        }
    }

    @Test
    public void foreach() {
        Buffers.ReadyQueue.digestionIds = Cuid.incremental( 0 );
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2, 3 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 11, 12, 13 } );
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 4, 5, 6 } );
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 7, 8, 9 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 14, 15 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 16 } );

        List<Buffer> expected = List.of(
            buffer( BINARY_V2, header + 4, 1, new LogId( "x/y", Map.of(),
                HEADERS, TYPES ), new byte[] { 1, 2, 3 } ),
            buffer( BINARY_V2, header + 4, 2, new LogId( "x/y", Map.of(),
                HEADERS, TYPES ), new byte[] { 4, 5, 6 } ),
            buffer( BINARY_V2, header + 4, 3, new LogId( "x/z", Map.of(),
                HEADERS, TYPES ), new byte[] { 11, 12, 13 } ),
            buffer( BINARY_V2, header + 4, 4, new LogId( "x/y", Map.of(),
                HEADERS, TYPES ), new byte[] { 7, 8, 9 } ),
            buffer( BINARY_V2, header + 4, 5, new LogId( "x/z", Map.of(),
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
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 11, 12, 13 } );
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 2 } );
        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 3 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 14, 15 } );

        List<Buffer> expected = List.of(
            buffer( BINARY_V2, header + 2, 1, new LogId( "x/y", Map.of(), HEADERS, TYPES ), new byte[] { 1, 2 } ),
            buffer( BINARY_V2, header + 4, 2, new LogId( "x/z", Map.of(), HEADERS, TYPES ), new byte[] { 11, 12, 13 } ),
            buffer( BINARY_V2, header + 4, 3, new LogId( "x/y", Map.of(), HEADERS, TYPES ), new byte[] { 3 } ),
            buffer( BINARY_V2, header + 2, 4, new LogId( "x/z", Map.of(), HEADERS, TYPES ), new byte[] { 14, 15 } )
        );
        assertReadyData( buffers, expected );
        assertReadyData( buffers, Lists.empty() );
    }

    /**
     * (logType, data, capacity) of the ready buffers, the buffers are released to the pool
     */
    private static List<org.assertj.core.groups.Tuple> readyData( Buffers buffers ) {
        List<org.assertj.core.groups.Tuple> result = new ArrayList<>();
        buffers.forEachReadyData( b -> result.add( tuple( b.id.logType,
            Arrays.copyOfRange( b.data(), b.headerLength(), b.length() ), b.capacity() ) ) );
        return result;
    }

    @Test
    public void setBufferSizeByName() {
        Buffers buffers = new Buffers( BufferConfigurationMap.custom(
            c( "x_y", ".+y", header + 2 ),
            c( "x_z", ".+z", header + 4 )
        ) );
        LogId y = new LogId( "x/y", Map.of(), HEADERS, TYPES );
        LogId z = new LogId( "x/z", Map.of(), HEADERS, TYPES );

        buffers.put( y, BINARY_V2, new byte[] { 1 } );
        buffers.put( z, BINARY_V2, new byte[] { 11 } );

        buffers.setBufferSize( "x_y", header + 4 );
        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "x_y", header + 4 ), Map.entry( "x_z", header + 4 ) );

        // the current x/y buffer has the old size: made ready, the next one has the new size
        buffers.put( y, BINARY_V2, new byte[] { 2, 3, 4 } );
        buffers.put( z, BINARY_V2, new byte[] { 12 } );

        assertThat( readyData( buffers ) ).containsExactlyInAnyOrder(
            tuple( "x/y", new byte[] { 1 }, header + 2 ),
            tuple( "x/y", new byte[] { 2, 3, 4 }, header + 4 ),
            tuple( "x/z", new byte[] { 11, 12 }, header + 4 )
        );
    }

    @Test
    public void setBufferSizeAll() {
        Buffers buffers = new Buffers( BufferConfigurationMap.custom(
            c( "x_y", ".+y", header + 2 ),
            c( "x_z", ".+z", header + 4 )
        ) );

        buffers.setBufferSize( header + 8 );
        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "x_y", header + 8 ), Map.entry( "x_z", header + 8 ) );

        buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 } );
        buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 11, 12, 13, 14, 15, 16, 17 } );

        assertThat( readyData( buffers ) ).containsExactlyInAnyOrder(
            tuple( "x/y", new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, header + 8 ),
            tuple( "x/z", new byte[] { 11, 12, 13, 14, 15, 16, 17 }, header + 8 )
        );
    }

    @Test
    public void setBufferSizeValidation() {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );

        assertThatThrownBy( () -> buffers.setBufferSize( "unknown", header + 4 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> buffers.setBufferSize( 0 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> buffers.setBufferSize( "DEFAULT", -1 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "DEFAULT", header + 4 ) );

        buffers.setBufferSize( header + 1 );
        assertThatThrownBy( () -> buffers.put( new LogId( "x/y", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1, 2 } ) )
            .isInstanceOf( IllegalArgumentException.class );
    }

    /**
     * the first configuration in the order of insertion wins ( "OTHER" goes before "LOG" in a HashMap )
     */
    @Test
    public void firstMatchingConfigurationWins() {
        Buffers buffers = new Buffers( BufferConfigurationMap.custom(
            c( "LOG", "^log$", header + 2 ),
            c( "OTHER", ".*", header + 4 )
        ) );

        assertThat( buffers.bufferSizes().keySet() ).containsExactly( "LOG", "OTHER" );

        buffers.put( new LogId( "log", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 1 } );
        buffers.put( new LogId( "other", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 2 } );

        assertThat( readyData( buffers ) ).containsExactlyInAnyOrder(
            tuple( "log", new byte[] { 1 }, header + 2 ),
            tuple( "other", new byte[] { 2 }, header + 4 )
        );
    }

    /**
     * (logType, data, priority) of the ready buffers, the buffers are released to the pool
     */
    private static List<org.assertj.core.groups.Tuple> readyPriorities( Buffers buffers ) {
        List<org.assertj.core.groups.Tuple> result = new ArrayList<>();
        buffers.forEachReadyData( b -> result.add( tuple( b.id.logType,
            Arrays.copyOfRange( b.data(), b.headerLength(), b.length() ), buffers.priority( b ) ) ) );
        return result;
    }

    @Test
    public void priority() {
        Buffers buffers = new Buffers( BufferConfigurationMap.custom(
            __( "x_y", new BufferConfigurationMap.BufferConfiguration( header + 2, Pattern.compile( ".+y" ), 5 ) ),
            c( "x_z", ".+z", header + 4 )
        ) );
        LogId y = new LogId( "x/y", Map.of(), HEADERS, TYPES );
        LogId z = new LogId( "x/z", Map.of(), HEADERS, TYPES );

        assertThat( buffers.priorities() ).containsOnly( Map.entry( "x_y", 5 ), Map.entry( "x_z", 0 ) );

        buffers.put( y, BINARY_V2, new byte[] { 1, 2 } );
        buffers.put( y, BINARY_V2, new byte[] { 3 } ); // x/y {1, 2} is ready
        buffers.put( z, BINARY_V2, new byte[] { 11 } );

        // applied to the ready and the current buffers, the buffers are not re-created
        buffers.setPriority( "x_z", 10 );
        buffers.put( z, BINARY_V2, new byte[] { 12 } );
        assertThat( buffers.priorities() ).containsOnly( Map.entry( "x_y", 5 ), Map.entry( "x_z", 10 ) );
        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "x_y", header + 2 ), Map.entry( "x_z", header + 4 ) );

        assertThat( readyPriorities( buffers ) ).containsExactlyInAnyOrder(
            tuple( "x/y", new byte[] { 1, 2 }, 5 ),
            tuple( "x/y", new byte[] { 3 }, 5 ),
            tuple( "x/z", new byte[] { 11, 12 }, 10 )
        );

        buffers.setPriority( -1 );
        assertThat( buffers.priorities() ).containsOnly( Map.entry( "x_y", -1 ), Map.entry( "x_z", -1 ) );

        // a priority change keeps the buffer size, a buffer size change keeps the priority
        buffers.setBufferSize( "x_y", header + 8 );
        assertThat( buffers.priorities() ).containsOnly( Map.entry( "x_y", -1 ), Map.entry( "x_z", -1 ) );
        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "x_y", header + 8 ), Map.entry( "x_z", header + 4 ) );

        assertThatThrownBy( () -> buffers.setPriority( "unknown", 1 ) ).isInstanceOf( IllegalArgumentException.class );
    }

    /**
     * the configuration map passed to the constructor is copied
     */
    @Test
    public void configurationIsCopied() {
        BufferConfigurationMap configurations = BufferConfigurationMap.defaultMap( header + 4 );
        Buffers buffers = new Buffers( configurations );

        configurations.put( "DEFAULT", new BufferConfigurationMap.BufferConfiguration( header + 100, Pattern.compile( ".*" ) ) );

        assertThat( buffers.bufferSizes() ).containsOnly( Map.entry( "DEFAULT", header + 4 ) );
    }

    @Test
    public void sentBufferIsReused() {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );
        LogId id = new LogId( "x/y", Map.of(), HEADERS, TYPES );

        buffers.put( id, BINARY_V2, new byte[] { 1 } );
        assertThat( readyData( buffers ) ).hasSize( 1 );
        assertThat( buffers.cache.size( header + 4 ) ).isEqualTo( 1 );

        buffers.put( id, BINARY_V2, new byte[] { 2 } );
        assertThat( buffers.cache.size() ).isZero();

        assertThat( readyData( buffers ) ).containsExactly( tuple( "x/y", new byte[] { 2 }, header + 4 ) );
        assertThat( buffers.cache.size() ).isEqualTo( 1 );
    }

    @Test
    public void bufferOfOldSizeIsNotPooled() {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );
        LogId id = new LogId( "x/y", Map.of(), HEADERS, TYPES );

        buffers.put( id, BINARY_V2, new byte[] { 1 } );
        assertThat( readyData( buffers ) ).hasSize( 1 );
        assertThat( buffers.cache.size( header + 4 ) ).isEqualTo( 1 );

        buffers.put( id, BINARY_V2, new byte[] { 2 } );
        buffers.setBufferSize( header + 8 );
        assertThat( buffers.cache.size() ).isZero();

        assertThat( readyData( buffers ) ).containsExactly( tuple( "x/y", new byte[] { 2 }, header + 4 ) );
        assertThat( buffers.cache.size() ).isZero();

        buffers.put( id, BINARY_V2, new byte[] { 3 } );
        assertThat( readyData( buffers ) ).containsExactly( tuple( "x/y", new byte[] { 3 }, header + 8 ) );
        assertThat( buffers.cache.size( header + 8 ) ).isEqualTo( 1 );
    }

    @Test
    public void putAfterCloseThrows() {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ) );
        LogId id = new LogId( "x/y", Map.of(), HEADERS, TYPES );

        buffers.put( id, BINARY_V2, new byte[] { 1 } );
        buffers.close();

        assertThatThrownBy( () -> buffers.put( id, BINARY_V2, new byte[] { 2 } ) )
            .isInstanceOf( IllegalStateException.class );
        assertThatThrownBy( () -> buffers.put( new LogId( "x/z", Map.of(), HEADERS, TYPES ), BINARY_V2, new byte[] { 3 } ) )
            .isInstanceOf( IllegalStateException.class );

        // the data put before close() is flushed, nothing else is left in the slots
        assertThat( readyData( buffers ) ).containsExactly( tuple( "x/y", new byte[] { 1 }, header + 4 ) );
        assertThat( readyData( buffers ) ).isEmpty();
    }

    /**
     * concurrent put ( per slot lock ) + concurrent consumer: every byte is delivered exactly once
     */
    @Test
    public void concurrentPut() throws Exception {
        int threads = 8;
        int puts = 20_000;
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 64 ) );
        LogId[] ids = new LogId[] {
            new LogId( "x/1", Map.of(), HEADERS, TYPES ),
            new LogId( "x/2", Map.of(), HEADERS, TYPES ),
            new LogId( "x/3", Map.of(), HEADERS, TYPES ),
            new LogId( "x/4", Map.of(), HEADERS, TYPES )
        };

        // delivered bytes per writer thread; updated only by the single consumer ( under Buffers.lock )
        long[] delivered = new long[threads];
        Consumer<Buffer> consumer = b -> {
            byte[] data = b.data();
            for( int i = b.headerLength(); i < b.length(); i++ ) delivered[data[i]]++;
        };

        AtomicBoolean done = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool( threads + 1 );
        try {
            Future<?> drain = executor.submit( () -> {
                while( !done.get() ) buffers.forEachReadyData( consumer );
            } );

            List<Future<?>> writers = new ArrayList<>();
            for( int t = 0; t < threads; t++ ) {
                byte value = ( byte ) t;
                writers.add( executor.submit( () -> {
                    for( int i = 0; i < puts; i++ ) {
                        buffers.put( ids[i % ids.length], BINARY_V2, new byte[] { value, value, value } );
                    }
                } ) );
            }
            for( Future<?> writer : writers ) writer.get( 1, TimeUnit.MINUTES );

            done.set( true );
            drain.get( 1, TimeUnit.MINUTES );
        } finally {
            executor.shutdownNow();
        }

        buffers.forEachReadyData( consumer );

        long[] expected = new long[threads];
        Arrays.fill( expected, puts * 3L );
        assertThat( delivered ).isEqualTo( expected );
    }

    /**
     * 32 writers into 2 LogIds with striped slots + parallel consumer: every byte is delivered exactly once, into a buffer of its LogId
     */
    @Test
    public void stripedConcurrentPut() throws Exception {
        int threads = 32;
        int puts = 20_000;
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 64 ), 16 );
        LogId[] ids = new LogId[] {
            new LogId( "x/1", Map.of(), HEADERS, TYPES ),
            new LogId( "x/2", Map.of(), HEADERS, TYPES )
        };

        // delivered bytes per (writer thread, LogId); the consumer runs in parallel
        AtomicLongArray delivered = new AtomicLongArray( threads * ids.length );
        AtomicBoolean wrongLogId = new AtomicBoolean();
        ObjIntConsumer<Buffer> consumer = ( b, worker ) -> {
            int id = b.id.logType.equals( "x/1" ) ? 0 : 1;
            byte[] data = b.data();
            for( int i = b.headerLength(); i < b.length(); i += 2 ) {
                if( data[i + 1] != id ) wrongLogId.set( true );
                delivered.incrementAndGet( data[i] * ids.length + id );
            }
        };

        AtomicBoolean done = new AtomicBoolean();
        ExecutorService workers = Executors.newFixedThreadPool( 3 );
        ExecutorService executor = Executors.newFixedThreadPool( threads + 1 );
        try {
            Future<?> drain = executor.submit( () -> {
                while( !done.get() ) buffers.forEachReadyData( workers, 4, consumer );
            } );

            List<Future<?>> writers = new ArrayList<>();
            for( int t = 0; t < threads; t++ ) {
                byte value = ( byte ) t;
                writers.add( executor.submit( () -> {
                    for( int i = 0; i < puts; i++ ) {
                        int id = i % ids.length;
                        buffers.put( ids[id], BINARY_V2, new byte[] { value, ( byte ) id } );
                    }
                } ) );
            }
            for( Future<?> writer : writers ) writer.get( 1, TimeUnit.MINUTES );

            done.set( true );
            drain.get( 1, TimeUnit.MINUTES );

            buffers.forEachReadyData( workers, 4, consumer );
        } finally {
            executor.shutdownNow();
            workers.shutdownNow();
        }

        assertThat( wrongLogId ).isFalse();
        for( int i = 0; i < delivered.length(); i++ ) {
            assertThat( delivered.get( i ) ).as( "writer %d, LogId %d", i / ids.length, i % ids.length ).isEqualTo( puts / ids.length );
        }
        assertThat( buffers.readyBuffers() ).isZero();
    }

    /**
     * a consumer exception stops the workers: the failed and not consumed buffers stay ready for the next call
     */
    @Test
    public void parallelConsumerFailure() throws Exception {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 1 ) );
        LogId id = new LogId( "x/y", Map.of(), HEADERS, TYPES );
        for( int i = 0; i < 64; i++ ) buffers.put( id, BINARY_V2, new byte[] { ( byte ) i } );

        List<Byte> delivered = Collections.synchronizedList( new ArrayList<>() );
        ExecutorService workers = Executors.newFixedThreadPool( 3 );
        try {
            assertThatThrownBy( () -> buffers.forEachReadyData( workers, 4, ( b, worker ) -> {
                byte value = b.data()[b.headerLength()];
                if( value == 10 ) throw new IllegalStateException( "send failed" );
                delivered.add( value );
            } ) ).isInstanceOf( IllegalStateException.class ).hasMessage( "send failed" );

            assertThat( buffers.readyBuffers() ).isEqualTo( 64 - delivered.size() );
            assertThat( buffers.cache.size() ).isEqualTo( delivered.size() );

            buffers.forEachReadyData( workers, 4, ( b, worker ) -> delivered.add( b.data()[b.headerLength()] ) );
        } finally {
            workers.shutdownNow();
        }

        List<Byte> expected = new ArrayList<>();
        for( int i = 0; i < 64; i++ ) expected.add( ( byte ) i );
        assertThat( delivered ).containsExactlyInAnyOrderElementsOf( expected );
        assertThat( buffers.readyBuffers() ).isZero();
    }

    /**
     * relative to the number of cores, rounded up to a power of 2
     */
    @Test
    public void stripes() {
        int cores = Runtime.getRuntime().availableProcessors();
        BufferConfigurationMap configurations = BufferConfigurationMap.defaultMap( header + 4 );

        assertThat( new Buffers( configurations, 1 ).getStripes() ).isEqualTo( 1 );
        assertThat( new Buffers( configurations, 3 ).getStripes() ).isEqualTo( 4 );
        assertThat( new Buffers( configurations, 16 ).getStripes() ).isEqualTo( 16 );
        assertThat( new Buffers( configurations, 0 ).getStripes() ).isEqualTo( powerOf2( cores ) );
        assertThat( new Buffers( configurations ).getStripes() ).isEqualTo( powerOf2( cores ) );
        assertThat( new Buffers( configurations, -1 ).getStripes() ).isEqualTo( powerOf2( cores - 1 ) );
        assertThat( new Buffers( configurations, -1000 ).getStripes() ).isEqualTo( 1 );

        Buffers buffers = new Buffers( configurations, 1 );
        buffers.setStripes( 3 );
        assertThat( buffers.getStripes() ).isEqualTo( 4 );
        buffers.setStripes( -1000 );
        assertThat( buffers.getStripes() ).isEqualTo( 1 );
        buffers.setStripes( 0 );
        assertThat( buffers.getStripes() ).isEqualTo( powerOf2( cores ) );
    }

    /**
     * runtime change: a LogId switches on its next put, the current buffer of the replaced slot is made ready
     */
    @Test
    public void setStripes() {
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 4 ), 1 );
        LogId id = new LogId( "x/y", Map.of(), HEADERS, TYPES );

        buffers.put( id, BINARY_V2, new byte[] { 1 } );
        assertThat( buffers.readyBuffers() ).isZero();

        buffers.setStripes( 4 );
        assertThat( buffers.getStripes() ).isEqualTo( 4 );
        assertThat( buffers.readyBuffers() ).isZero();

        buffers.put( id, BINARY_V2, new byte[] { 2 } );
        assertThat( buffers.readyBuffers() ).isEqualTo( 1 );

        // shrink
        buffers.setStripes( 1 );
        buffers.put( id, BINARY_V2, new byte[] { 3 } );
        assertThat( buffers.readyBuffers() ).isEqualTo( 2 );

        assertThat( readyData( buffers ) ).containsExactly(
            tuple( "x/y", new byte[] { 1 }, header + 4 ),
            tuple( "x/y", new byte[] { 2 }, header + 4 ),
            tuple( "x/y", new byte[] { 3 }, header + 4 )
        );

        // the same number of stripes - the slot is kept
        buffers.setStripes( 1 );
        buffers.put( id, BINARY_V2, new byte[] { 4 } );
        buffers.put( id, BINARY_V2, new byte[] { 5 } );
        assertThat( buffers.readyBuffers() ).isZero();
        assertThat( readyData( buffers ) ).containsExactly( tuple( "x/y", new byte[] { 4, 5 }, header + 4 ) );
    }

    /**
     * stripedConcurrentPut + the number of stripes is changed concurrently: every byte is delivered exactly once
     */
    @Test
    public void stripedConcurrentPutWithStripesChange() throws Exception {
        int threads = 32;
        int puts = 20_000;
        Buffers buffers = new Buffers( BufferConfigurationMap.defaultMap( header + 64 ), 16 );
        LogId[] ids = new LogId[] {
            new LogId( "x/1", Map.of(), HEADERS, TYPES ),
            new LogId( "x/2", Map.of(), HEADERS, TYPES )
        };

        AtomicLongArray delivered = new AtomicLongArray( threads * ids.length );
        AtomicBoolean wrongLogId = new AtomicBoolean();
        ObjIntConsumer<Buffer> consumer = ( b, worker ) -> {
            int id = b.id.logType.equals( "x/1" ) ? 0 : 1;
            byte[] data = b.data();
            for( int i = b.headerLength(); i < b.length(); i += 2 ) {
                if( data[i + 1] != id ) wrongLogId.set( true );
                delivered.incrementAndGet( data[i] * ids.length + id );
            }
        };

        AtomicBoolean done = new AtomicBoolean();
        ExecutorService workers = Executors.newFixedThreadPool( 3 );
        ExecutorService executor = Executors.newFixedThreadPool( threads + 2 );
        try {
            Future<?> drain = executor.submit( () -> {
                while( !done.get() ) buffers.forEachReadyData( workers, 4, consumer );
            } );
            Future<?> switcher = executor.submit( () -> {
                double[] values = new double[] { 1, 4, 16, 2 };
                int i = 0;
                while( !done.get() ) {
                    buffers.setStripes( values[i++ % values.length] );
                    Thread.yield();
                }
            } );

            List<Future<?>> writers = new ArrayList<>();
            for( int t = 0; t < threads; t++ ) {
                byte value = ( byte ) t;
                writers.add( executor.submit( () -> {
                    for( int i = 0; i < puts; i++ ) {
                        int id = i % ids.length;
                        buffers.put( ids[id], BINARY_V2, new byte[] { value, ( byte ) id } );
                    }
                } ) );
            }
            for( Future<?> writer : writers ) writer.get( 1, TimeUnit.MINUTES );

            done.set( true );
            drain.get( 1, TimeUnit.MINUTES );
            switcher.get( 1, TimeUnit.MINUTES );

            buffers.forEachReadyData( workers, 4, consumer );
        } finally {
            executor.shutdownNow();
            workers.shutdownNow();
        }

        assertThat( wrongLogId ).isFalse();
        for( int i = 0; i < delivered.length(); i++ ) {
            assertThat( delivered.get( i ) ).as( "writer %d, LogId %d", i / ids.length, i % ids.length ).isEqualTo( puts / ids.length );
        }
        assertThat( buffers.readyBuffers() ).isZero();
    }

    private static int powerOf2( int n ) {
        int result = 1;
        while( result < n ) result <<= 1;
        return result;
    }
}
