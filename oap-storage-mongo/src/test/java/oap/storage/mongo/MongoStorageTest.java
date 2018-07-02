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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.util.Id;
import org.bson.types.ObjectId;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static oap.application.ApplicationUtils.service;
import static oap.storage.Storage.LockStrategy.Lock;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 19.09.2017.
 */
@Slf4j
public class MongoStorageTest extends AbstractMongoTest {
    private MongoStorage<TestMongoBean> storage;
    private TestMongoBean bean1;
    private TestMongoBean bean2;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        reopen();
    }

    public void reopen() {
        if( storage != null ) storage.close();
        storage = service( new MongoStorage<>( mongoClient, "test", Lock ) );
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        super.afterMethod();

        storage.close();
    }

    @Test
    public void testStore() {
        bean1 = storage.store( new TestMongoBean() );
        bean2 = storage.store( new TestMongoBean() );
        storage.store( new TestMongoBean( bean1.id ) );

        log.debug( "bean1 = {}", bean1 );
        log.debug( "bean2 = {}", bean2 );

        reopen();

        assertThat( storage ).hasSize( 2 );
        assertThat( storage.collection.count() ).isEqualTo( 2 );

    }

    @Test( dependsOnMethods = { "testStore" } )
    public void testDelete() {
        testStore();

        storage.delete( bean1.id );
        storage.fsync();

        assertThat( storage.collection.count() ).isEqualTo( 1 );
    }

    @Test( dependsOnMethods = { "testStore" } )
    public void testUpdateMongo() {
        testStore();

        storage.fsync();

        try( val oplogService = service( new OplogService( mongoClient ) ) ) {
            storage.oplogService = oplogService;
            storage.start();

            storage.collection.updateOne(
                eq( "_id", new ObjectId( bean1.id ) ),
                and( set( "object.c", 1 ), inc( "modified", 1 ) )
            );

            assertEventually( 100, 100, () -> assertThat( storage.get( bean1.id ).get().c ).isEqualTo( 1 ) );

            storage.collection.updateOne(
                eq( "_id", new ObjectId( bean1.id ) ),
                and( set( "object.c", -1 ) )
            );
            storage.collection.updateOne(
                eq( "_id", new ObjectId( bean2.id ) ),
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
        public int c;

        @JsonCreator
        public TestMongoBean( @JsonProperty String id ) {
            this.id = id;
        }

        public TestMongoBean() {
            this( null );
        }
    }
}