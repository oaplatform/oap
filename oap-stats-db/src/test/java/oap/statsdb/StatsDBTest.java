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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.val;
import oap.storage.MemoryStorage;
import oap.storage.SingleFileStorage;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import static oap.application.ApplicationUtils.service;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 08.09.2017.
 */
public class StatsDBTest extends AbstractTest {
    private static final KeySchema schema = new KeySchema( "n1", "n2" );
    private Path masterDbPath;
    private Path nodeDbPath;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        masterDbPath = Env.tmpPath( "master.db" );
        nodeDbPath = Env.tmpPath( "node.db" );
    }

    @Test
    public void testPersistMaster() {
        try( val storage = service( new SingleFileStorage<>( masterDbPath, NodeIdentifier.identifier, 10000 ) );
             val master = service( new StatsDBMaster( schema, storage ) ) ) {
            master.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
        }

        try( val storage = service( new SingleFileStorage<>( masterDbPath, NodeIdentifier.identifier, 10000 ) );
             val master = service( new StatsDBMaster( schema, storage ) ) ) {
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNotNull();
            assertThat( master.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void testPersistNode() {
        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema );
        try( val storage = service( new SingleFileStorage<>( nodeDbPath, NodeIdentifier.identifier, 10000 ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) ) {
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
        }

        try( val storage = service( new SingleFileStorage<>( nodeDbPath, NodeIdentifier.identifier, 10000 ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) ) {
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNotNull();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void testSync() {
        try( val storage = service( new SingleFileStorage<>( masterDbPath, NodeIdentifier.identifier, 10000 ) );
             val master = service( new StatsDBMaster( schema, storage ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), new MemoryStorage<>( NodeIdentifier.identifier ) ) ) ) {

            DateTimeUtils.setCurrentMillisFixed( 1 );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.ci = 10, () -> new MockChild( 10 ) );
            node.update( new MockKey2( "k1", "k3" ), ( c ) -> c.ci = 1, () -> new MockChild( 1 ) );
            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 20, () -> new MockValue( 20 ) );

            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
            assertThat( master.<MockKey2, MockChild>get( new MockKey2( "k1", "k2" ) ).ci ).isEqualTo( 10 );
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 20 );

            DateTimeUtils.setCurrentMillisFixed( 2 );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.ci = 10, () -> new MockChild( 10 ) );
            node.update( new MockKey1( "k1" ), ( c ) -> c.i2 = 21, () -> new MockValue( 21 ) );

            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
            assertThat( master.<MockKey2, MockChild>get( new MockKey2( "k1", "k2" ) ).ci ).isEqualTo( 20 );
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).i2 ).isEqualTo( 41 );
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).sum ).isEqualTo( 21L );
        }
    }

    @Test( dependsOnMethods = "testSync" )
    public void testCalculatedValuesAfterRestart() {
        testSync();

        try( val storage = service( new SingleFileStorage<>( masterDbPath, NodeIdentifier.identifier, 10000 ) );
             val master = service( new StatsDBMaster( schema, storage ) ) ) {
            assertThat( master.<MockKey1, MockValue>get( new MockKey1( "k1" ) ).sum ).isEqualTo( 21L );
        }
    }

    @Test
    public void testSyncFailed() {
        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema );

        try( val storage = service( new SingleFileStorage<>( nodeDbPath, NodeIdentifier.identifier, 10000 ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) ) {

            master.syncWithException( ( sync ) -> new RuntimeException( "sync" ) );
            node.update( new MockKey2( "k1", "k2" ), ( c ) -> c.i2 = 10, () -> new MockValue( 10 ) );
            node.sync();
            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
        }

        assertThat( master.syncs ).isEmpty();

        try( val storage = service( new SingleFileStorage<>( nodeDbPath, NodeIdentifier.identifier, 10000 ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), storage ) ) ) {

            master.syncWithoutException();
            node.sync();

            assertThat( node.<MockKey2, MockValue>get( new MockKey2( "k1", "k2" ) ) ).isNull();
        }

        assertThat( master.syncs ).hasSize( 1 );
    }

    @Test
    public void testVersion() {
        try( val master = service( new StatsDBMaster( schema, new MemoryStorage<>( NodeIdentifier.identifier ) ) );
             val node = service( new StatsDBNode( master, Env.tmpPath( "node" ), new MemoryStorage<>( NodeIdentifier.identifier ) ) ) ) {

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
    public static class MockValue implements Node.Container<MockValue, MockChild> {
        public long l1;
        public int i2;

        @JsonIgnore
        public long sum;

        public MockValue() {
            this( 0 );
        }

        public MockValue( int i2 ) {
            this.i2 = i2;
        }

        @Override
        public MockValue merge( Stream<MockChild> children ) {
            sum = children.mapToLong( c -> c.ci ).sum();

            return this;
        }

        @Override
        public MockValue merge( MockValue other ) {
            l1 += other.l1;
            i2 += other.i2;

            return this;
        }
    }

    @ToString
    public static class MockChild implements Node.Value<MockChild> {
        public long ci;

        public MockChild() {
        }

        public MockChild( long ci ) {
            this.ci = ci;
        }

        @Override
        public MockChild merge( MockChild other ) {
            ci += other.ci;
            return this;
        }
    }
}
