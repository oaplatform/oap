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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.server.nio.NioHttpServer;
import oap.message.MessageSenderUtils;
import oap.message.client.MessageSender;
import oap.message.server.MessageHttpHandler;
import oap.statsdb.node.StatsDBNode;
import oap.statsdb.node.StatsDBTransportMessage;
import oap.storage.mongo.MongoFixture;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Cuid;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static oap.statsdb.NodeSchema.nc;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 08.09.2017.
 */
@Test
public class StatsDBTest extends Fixtures {

    static final NodeSchema schema2 = new NodeSchema(
        nc( "n1", MockChild2.class ),
        nc( "n2", MockValue.class ) );
    static final NodeSchema schema3 = new NodeSchema(
        nc( "n1", MockChild1.class ),
        nc( "n2", MockChild2.class ),
        nc( "n3", MockValue.class ) );
    private static final MongoFixture MONGO_FIXTURE = new MongoFixture( "MONGO" );
    private final TestDirectoryFixture testDirectoryFixture;

    public StatsDBTest() {
        fixture( MONGO_FIXTURE );
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        fixture( new SystemTimerFixture() );
    }

    @Test
    public void testEmptySync() {
        try( var master = new StatsDBMaster( schema3, StatsDBStorage.NULL );
             var node = new StatsDBNode( schema3, new StatsDBTransportMock( master ), null ) ) {

            assertThat( node.lastSyncSuccess ).isFalse();
            node.sync();
            assertThat( node.lastSyncSuccess ).isTrue();
        }
    }

    @Test
    public void children() {
        try( var master = new StatsDBMaster( schema2, StatsDBStorage.NULL ) ) {
            master.<MockValue>update( "k1", "k2", c -> c.v = 10 );
            master.<MockValue>update( "k1", "k3", c -> c.v = 3 );
            master.<MockValue>update( "k2", "k4", c -> c.v = 4 );
            master.<MockChild2>update( "k1", c -> c.vc = 10 );


            assertThat( master.children( "k1" ) )
                .hasSize( 2 )
                .contains( new MockValue( 10 ) )
                .contains( new MockValue( 3 ) );

            assertThat( master.children( "k2" ) )
                .hasSize( 1 )
                .contains( new MockValue( 4 ) );

            assertThat( master.children( "unknown" ) ).isEmpty();
            assertThat( master.children( "k1", "k2" ) ).isEmpty();
        }
    }

    @Test
    public void mergeChild() {
        try( var master = new StatsDBMaster( schema3, StatsDBStorage.NULL );
             var node = new StatsDBNode( schema3, new StatsDBTransportMock( master ) ) ) {

            node.<MockChild1>update( "p1", p -> p.vc += 1 );
            node.<MockChild2>update( "p1", "c2", c -> c.vc += 1 );
            node.<MockValue>update( "p1", "c2", "c3", c -> c.v += 2 );
            node.sync();

            assertThat( master.<MockChild1>get( "p1" ).vc ).isEqualTo( 1 );
            assertThat( master.<MockChild1>get( "p1" ).sum ).isEqualTo( 2 );
            assertThat( master.<MockChild1>get( "p1" ).sum2 ).isEqualTo( 1 );

            node.<MockChild1>update( "p1", p -> p.vc += 1 );
            node.<MockChild2>update( "p1", "c2", c -> c.vc += 2 );
            node.sync();

            node.<MockValue>update( "p1", "c2", "c3", c -> c.v += 2 );
            node.sync();

            assertThat( master.<MockChild1>get( "p1" ).vc ).isEqualTo( 2 );
            assertThat( master.<MockChild1>get( "p1" ).sum ).isEqualTo( 4 );
            assertThat( master.<MockChild1>get( "p1" ).sum2 ).isEqualTo( 3 );

            assertThat( master.<MockChild2>get( "p1", "c2" ).vc ).isEqualTo( 3 );
            assertThat( master.<MockChild2>get( "p1", "c2" ).sum ).isEqualTo( 4 );

            assertThat( master.<MockValue>get( "p1", "c2", "c3" ).v ).isEqualTo( 4 );
        }
    }

    @Test
    public void persistMaster() {
        try( var masterStorage = new StatsDBStorageMongo( MONGO_FIXTURE.client(), "test" );
             StatsDBMaster master = new StatsDBMaster( schema3, masterStorage ) ) {
            master.<MockValue>update( "k1", "k2", "k3", c -> c.v += 8 );
            master.<MockValue>update( "k1", "k2", "k3", c -> c.v += 2 );
            master.<MockValue>update( "k1", "k2", "k33", c -> c.v += 1 );
            master.<MockChild1>update( "k1", c -> c.vc += 111 );
        }

        try( var masterStorage = new StatsDBStorageMongo( MONGO_FIXTURE.client(), "test" );
             StatsDBMaster master = new StatsDBMaster( schema3, masterStorage ) ) {
            assertThat( master.<MockValue>get( "k1", "k2", "k3" ).v ).isEqualTo( 10 );

            assertThat( master.<MockChild1>get( "k1" ).sum ).isEqualTo( 11L );
            assertThat( master.<MockChild1>get( "k1" ).sum2 ).isEqualTo( 0L );
        }
    }

    @Test
    public void sync() {
        try( var masterStorage = new StatsDBStorageMongo( MONGO_FIXTURE.client(), "test" );
             var master = new StatsDBMaster( schema2, masterStorage );
             var node = new StatsDBNode( schema2, new StatsDBTransportMock( master ) ) ) {
            node.sync();

            node.<MockValue>update( "k1", "k2", c -> c.v += 10 );
            node.<MockValue>update( "k1", "k3", c -> c.v += 1 );
            node.<MockChild2>update( "k1", c -> c.vc += 20 );

            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
            assertThat( master.<MockValue>get( "k1", "k2" ).v ).isEqualTo( 10L );
            assertThat( master.<MockChild2>get( "k1" ).vc ).isEqualTo( 20L );
            assertThat( master.<MockChild2>get( "k1" ).sum ).isEqualTo( 11L );

            node.<MockValue>update( "k1", "k2", c -> c.v += 10 );
            node.<MockChild2>update( "k1", c -> c.vc += 21 );

            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
            assertThat( master.<MockValue>get( "k1", "k2" ).v ).isEqualTo( 20 );
            assertThat( master.<MockChild2>get( "k1" ).vc ).isEqualTo( 41 );
            assertThat( master.<MockChild2>get( "k1" ).sum ).isEqualTo( 21L );
        }
    }

    @Test
    public void calculatedValuesAfterRestart() {
        try( var masterStorage = new StatsDBStorageMongo( MONGO_FIXTURE.client(), "test" );
             var master = new StatsDBMaster( schema2, masterStorage );
             var node = new StatsDBNode( schema2, new StatsDBTransportMock( master ) ) ) {
            node.sync();

            node.<MockValue>update( "k1", "k2", c -> c.v += 10 );
            node.<MockValue>update( "k1", "k3", c -> c.v += 1 );
            node.<MockChild2>update( "k1", c -> c.vc += 20 );
        }

        try( var masterStorage = new StatsDBStorageMongo( MONGO_FIXTURE.client(), "test" );
             var master = new StatsDBMaster( schema2, masterStorage ) ) {
            assertThat( master.<MockChild2>get( "k1" ).sum ).isEqualTo( 11L );
        }
    }

    @Test
    public void syncFailed() {
        var transport = new StatsDBTransportMock();

        try( var node = new StatsDBNode( schema2, transport ) ) {
            transport.syncWithException( _ -> new RuntimeException( "sync" ) );
            node.<MockValue>update( "k1", "k2", c -> c.v += 10 );
            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
            transport.syncWithoutException();
            node.<MockValue>update( "k1", "k2", c -> c.v += 10 );
        }

        assertThat( transport.syncs ).hasSize( 1 );
    }

    @Test
    public void version() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var uid = Cuid.incremental( 0 );
        try( var master = new StatsDBMaster( schema2, StatsDBStorage.NULL );
             var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( new StatsDBMessageListener( master ) ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "msend" ), -1 );
             var node = new StatsDBNode( schema2, new StatsDBTransportMessage( client ), uid ) ) {
            server.bind( "/messages", messageHttpHandler );
            client.start();
            server.start();
            messageHttpHandler.preStart();

            uid.reset( 0 );

            node.<MockChild2>update( "k1", c -> c.vc += 20 );
            node.sync();
            client.syncMemory();
            MessageSenderUtils.waitSendAll( client, 5000, 50 );
            assertThat( master.<MockChild2>get( "k1" ).vc ).isEqualTo( 20L );

            uid.reset( 0 );
            node.<MockChild2>update( "k1", c -> c.vc += 20 );
            node.sync();
            client.syncMemory();
            MessageSenderUtils.waitSendAll( client, 5000, 50 );
            assertThat( master.<MockChild2>get( "k1" ).vc ).isEqualTo( 20L );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MockValue implements Node.Value<MockValue> {
        public long v;

        public MockValue() {
        }

        public MockValue( long v ) {
            this.v = v;
        }

        @Override
        public MockValue merge( MockValue other ) {
            v += other.v;

            return this;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MockChild2 implements Node.Container<MockChild2, MockValue> {
        public long vc;
        @JsonIgnore
        public long sum;

        public MockChild2() {
        }

        @Override
        public MockChild2 merge( MockChild2 other ) {
            vc += other.vc;

            return this;
        }

        @Override
        public MockChild2 aggregate( List<MockValue> children ) {
            sum = children.stream().mapToLong( c -> c.v ).sum();
            return this;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MockChild1 implements Node.Container<MockChild1, MockChild2> {
        public long vc;
        @JsonIgnore
        public long sum;
        @JsonIgnore
        public long sum2;

        public MockChild1() {
        }

        @Override
        public MockChild1 merge( MockChild1 other ) {
            vc += other.vc;

            return this;
        }

        @Override
        public MockChild1 aggregate( List<MockChild2> children ) {
            sum = children.stream().mapToLong( c -> c.sum ).sum();
            sum2 = children.stream().mapToLong( c -> c.vc ).sum();
            return this;
        }
    }
}
