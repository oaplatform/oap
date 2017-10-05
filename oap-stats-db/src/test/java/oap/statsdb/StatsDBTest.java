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

package oap.statsdb;

import com.google.common.collect.Iterators;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.val;
import oap.storage.MemoryStorage;
import oap.storage.SingleFileStorage;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 08.09.2017.
 */
public class StatsDBTest extends AbstractTest {
    private static final KeySchema schema = new KeySchema( "n1", "n2" );

    @Test
    public void testPersistMaster() {
        val masterDbPath = Env.tmpPath( "master.db" );

        try( val storage = new SingleFileStorage<Node>( masterDbPath, NodeIdentifier.identifier, 10000 );
             val master = new StatsDBMaster( schema, storage ) ) {
            master.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
        }

        try( val storage = new SingleFileStorage<Node>( masterDbPath, NodeIdentifier.identifier, 10000 );
             val master = new StatsDBMaster( schema, storage ) ) {
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNotNull();
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void testPersistNode() {
        val nodeDbPath = Env.tmpPath( "node.db" );

        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema );
        try( val storage = new SingleFileStorage<Node>( nodeDbPath, NodeIdentifier.identifier, 10000 );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) {
            node.start();
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
        }

        try( val storage = new SingleFileStorage<Node>( nodeDbPath, NodeIdentifier.identifier, 10000 );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) {
            node.start();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNotNull();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void testSync() {
        try( val master = new StatsDBMaster( schema, new MemoryStorage<>( NodeIdentifier.identifier ) );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), new MemoryStorage<>( NodeIdentifier.identifier ) ) ) {
            node.start();

            DateTimeUtils.setCurrentMillisFixed( 1 );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 20, () -> new MockValue( 20 ) );
            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 10 );
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 20 );

            DateTimeUtils.setCurrentMillisFixed( 2 );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 21, () -> new MockValue( 21 ) );
            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 20 );
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 41 );
        }
    }

    @Test
    public void testSyncFailed() {
        val nodeDbPath = Env.tmpPath( "node.db" );

        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema );

        try( val storage = new SingleFileStorage<Node>( nodeDbPath, NodeIdentifier.identifier, 10000 );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) {
            node.start();

            master.syncWithException( ( sync ) -> new RuntimeException( "sync" ) );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
        }

        assertThat( master.syncs ).isEmpty();

        try( val storage = new SingleFileStorage<Node>( nodeDbPath, NodeIdentifier.identifier, 10000 );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) {
            node.start();

            master.syncWithoutException();
            node.sync();

            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
        }

        assertThat( master.syncs ).hasSize( 1 );
    }

    @Test
    public void testVersion() {
        try( val master = new StatsDBMaster( schema, new MemoryStorage<>( NodeIdentifier.identifier ) );
             val node = new StatsDBNode( master, Env.tmpPath( "node" ), new MemoryStorage<>( NodeIdentifier.identifier ) ) ) {
            node.start();

            DateTimeUtils.setCurrentMillisFixed( 1 );

            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 20, () -> new MockValue( 20 ) );
            node.sync();
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 20 );

            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 21, () -> new MockValue( 21 ) );
            node.sync();
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 20 );
        }
    }

    @ToString
    @AllArgsConstructor
    public static class MockKey2 implements Iterable<String> {
        public final String key1;
        public final String key2;

        @Override
        public Iterator<String> iterator() {
            return Iterators.forArray( key1, key2 );
        }
    }

    @ToString
    @AllArgsConstructor
    public static class MockKey1 implements Iterable<String> {
        public final String key1;

        @Override
        public Iterator<String> iterator() {
            return Iterators.forArray( key1 );
        }
    }

    @ToString
    public static class MockValue implements Node.Value<MockValue> {
        public long l1;
        public int i2;

        public MockValue() {
            this( 0 );
        }

        public MockValue( int i2 ) {
            this.i2 = i2;
        }

        @Override
        public MockValue merge( MockValue other ) {
            l1 += other.l1;
            i2 += other.i2;
            return this;
        }

        @Override
        public MockValue clone() {
            return null;
        }
    }
}
