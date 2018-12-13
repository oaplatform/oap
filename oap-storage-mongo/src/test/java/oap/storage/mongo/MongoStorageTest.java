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

package oap.storage.mongo;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.util.Id;
import org.testng.annotations.Test;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 19.09.2017.
 */
@Slf4j
public class MongoStorageTest extends AbstractMongoTest {

    @Test
    public void store() {
        try( MongoStorage<TestMongoBean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED ) ) {
            storage.start();
            TestMongoBean bean1 = storage.store( new TestMongoBean() );
            TestMongoBean bean2 = storage.store( new TestMongoBean() );
            storage.store( new TestMongoBean( bean1.id, "test" ) );

            log.debug( "bean1 = {}", bean1 );
            log.debug( "bean2 = {}", bean2 );

            assertThat( bean1.id ).isNotBlank();
            assertThat( bean2.id ).isNotBlank();
        }
        try( MongoStorage<TestMongoBean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED ) ) {
            storage.start();
            assertThat( storage.select() ).hasSize( 2 );
            assertThat( storage.collection.count() ).isEqualTo( 2 );

        }
    }

    @Test
    public void delete() {
        try( MongoStorage<TestMongoBean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED ) ) {
            storage.start();
            TestMongoBean bean1 = storage.store( new TestMongoBean() );
            storage.store( new TestMongoBean() );

            storage.delete( bean1.id );
            storage.fsync();

            assertThat( storage.collection.count() ).isEqualTo( 1 );
        }
    }

    @Test()
    public void updateMongo() {
        store();
        try( MongoStorage<TestMongoBean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED );
             val oplogService = new OplogService( mongoClient ) ) {
            oplogService.start();
            storage.oplogService = oplogService;
            storage.start();
            TestMongoBean bean1 = storage.store( new TestMongoBean() );
            TestMongoBean bean2 = storage.store( new TestMongoBean() );
            storage.fsync();

            storage.collection.updateOne(
                eq( "_id", bean1.id ),
                and( set( "object.c", 1 ), inc( "modified", 1 ) )
            );

            assertEventually( 100, 100, () -> assertThat( storage.get( bean1.id ).get().c ).isEqualTo( 1 ) );

            storage.collection.updateOne(
                eq( "_id", bean1.id ),
                and( set( "object.c", -1 ) )
            );
            storage.collection.updateOne(
                eq( "_id", bean2.id ),
                and( set( "object.c", 100 ), inc( "modified", 1 ) )
            );

            assertEventually( 100, 100, () -> assertThat( storage.get( bean2.id ).get().c ).isEqualTo( 100 ) );
            assertThat( storage.get( bean1.id ).get().c ).isEqualTo( 1 );
        }
    }

    @ToString
    @EqualsAndHashCode( of = { "id" } )
    public static class TestMongoBean {
        @Id
        public String id;
        public String name;
        public int c;

        public TestMongoBean( String id, String name ) {
            this.id = id;
            this.name = name;
        }

        public TestMongoBean( String name ) {
            this.name = name;
        }

        public TestMongoBean() {
        }
    }
}
