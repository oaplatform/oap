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

package oap.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.id.Id;
import oap.id.Identifier;
import oap.io.Files;
import oap.storage.mongo.MongoClient;
import oap.storage.mongo.MongoFixture;
import oap.storage.mongo.Version;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * It requires MongoDB to be installed on the machine with enabled Replica Set Oplog
 *
 * @see <a href="https://docs.mongodb.com/manual/administration/install-community/">Install MongoDB Community Edition</a>
 * @see <a href="https://docs.mongodb.com/manual/tutorial/deploy-replica-set-for-testing/">Deploy a Replica Set for
 * Testing and Development</a>
 */
@Slf4j
public class MongoPersistenceTest extends Fixtures {

    private final MongoFixture mongoFixture;
    private final Identifier<String, Bean> beanIdentifier =
        Identifier.<Bean>forId( o -> o.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.name )
            .build();
    private final TestDirectoryFixture testDirectoryFixture;

    public MongoPersistenceTest() {
        mongoFixture = fixture( new MongoFixture( "MONGO" ) );
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void store() {
        MemoryStorage<String, Bean> storage1 = new MemoryStorage<>( beanIdentifier, SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage1 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            Bean bean1 = storage1.store( new Bean( "test1" ), Storage.MODIFIED_BY_SYSTEM );
            Bean bean2 = storage1.store( new Bean( "test2" ), Storage.MODIFIED_BY_SYSTEM );
            // rewrite bean2 'test2' with 'test3' name
            bean2 = storage1.store( new Bean( bean2.id, "test3" ), Storage.MODIFIED_BY_SYSTEM );

            log.debug( "bean1 = {}", bean1 );
            log.debug( "bean2 = {}", bean2 );

            assertThat( bean1.id ).isEqualTo( "TST1" );
            assertThat( bean2.id ).isEqualTo( "TST2" );
        }

        // Make sure that for a new connection the objects still present in MongoDB
        MemoryStorage<String, Bean> storage2 = new MemoryStorage<>( beanIdentifier, SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage2 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            assertThat( storage2.select() ).containsOnly(
                new Bean( "TST1", "test1" ),
                new Bean( "TST2", "test3" )
            );
            assertThat( persistence.collection.countDocuments() ).isEqualTo( 2 );
        }
    }

    @Test
    public void delete() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, "test", 50, storage ) ) {
            mongoClient.preStart();
            persistence.preStart();
            Bean bean1 = storage.store( new Bean( "test1" ), Storage.MODIFIED_BY_SYSTEM );
            storage.store( new Bean( "test2" ), Storage.MODIFIED_BY_SYSTEM );

            storage.delete( bean1.id, Storage.MODIFIED_BY_SYSTEM );
            // one bean is removed, one is left
            assertEventually( 100, 100, () -> assertThat( persistence.collection.countDocuments() ).isEqualTo( 1 ) );
        }
    }

    @Test()
    public void update() {
        MemoryStorage<String, Bean> storage1 = new MemoryStorage<>( Identifier.<Bean>forId( o -> o.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.name )
            .build(), SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage1 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            storage1.store( new Bean( "111", "initialName" ), Storage.MODIFIED_BY_SYSTEM );
            storage1.update( "111", bean -> {
                bean.name = "newName";
                return bean;
            }, Storage.MODIFIED_BY_SYSTEM );
        }
        MemoryStorage<String, Bean> storage2 = new MemoryStorage<>( Identifier.<Bean>forId( o -> o.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.name )
            .build(), SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage2 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            assertThat( storage2.select() )
                .containsExactly( new Bean( "111", "newName" ) );
        }
    }

    @Test
    public void storeTooBig() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED, 100 );
        Path crashDumpPath = testDirectoryFixture.testPath( "failures" );
        String table = "test";
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, table, 6000, storage, crashDumpPath ) ) {
            mongoClient.preStart();
            persistence.preStart();
            //this generates 16 MiB of XXXXXXXXXXXXXXXXXXXXXXX
            storage.store( new Bean( "X".repeat( 16793600 + 1 ) ), Storage.MODIFIED_BY_SYSTEM );
        }
        assertThat( Files.wildcard( crashDumpPath.resolve( table ), "*.json.gz" ) ).hasSize( 1 );
    }

    @Test
    public void migration() {
        String table = "beans";
        mongoFixture.insertDocument( getClass(), table, "migration/1.json" );
        mongoFixture.insertDocument( getClass(), table, "migration/2.json" );
        mongoFixture.initializeVersion( new Version( 1 ) );
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Bean> persistence = new MongoPersistence<>( mongoClient, table, 6000, storage ) ) {
            mongoClient.preStart();
            persistence.preStart();
            assertThat( storage.list() ).containsOnly(
                new Bean( "1", "name" ),
                new Bean( "2", "name" ) );
        }

    }

    /**
     * this test works because of {@link oap.json.TypeIdFactory}
     */
    @Test
    public void accidentalPolymorphism() {

        MemoryStorage<String, Object> storage1 = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Object> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage1 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            PolyBeanA bean1 = ( PolyBeanA ) storage1.store( new PolyBeanA( "test1" ), Storage.MODIFIED_BY_SYSTEM );
            PolyBeanB bean2 = ( PolyBeanB ) storage1.store( new PolyBeanB( "test2" ), Storage.MODIFIED_BY_SYSTEM );

            log.debug( "bean1 = {}", bean1 );
            log.debug( "bean2 = {}", bean2 );
        }

        MemoryStorage<String, Object> storage2 = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED, 100 );
        try( MongoClient mongoClient = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" );
             MongoPersistence<String, Object> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage2 ) ) {
            mongoClient.preStart();
            persistence.preStart();
            assertThat( storage2.select() ).containsOnly(
                new PolyBeanA( "test1" ),
                new PolyBeanB( "test2" )
            );
            assertThat( persistence.collection.countDocuments() ).isEqualTo( 2 );
        }

    }

    @EqualsAndHashCode
    @ToString
    public static class PolyBeanA {
        @Id
        String a;

        @JsonCreator
        public PolyBeanA( String a ) {
            this.a = a;
        }
    }

    @EqualsAndHashCode
    @ToString
    public static class PolyBeanB {
        @Id
        String b;

        @JsonCreator
        public PolyBeanB( String b ) {
            this.b = b;
        }
    }
}
