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

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * It requires installed MongoDB on the machine with enabled Replica Set Oplog
 *
 * @see <a href="https://docs.mongodb.com/manual/administration/install-community/">Install MongoDB Community Edition</a>
 * @see <a href="https://docs.mongodb.com/manual/tutorial/deploy-replica-set-for-testing/">Deploy a Replica Set for
 * Testing and Development</a>
 */
@Slf4j
public class MongoStorageTest extends AbstractMongoTest {

    @Test
    public void store() {
        try( MongoStorage<Bean> storage = new MongoStorage<>( mongoClient, "test", beanIdentifier, SERIALIZED ) ) {
            storage.start();
            Bean bean1 = storage.store( new Bean( "test1" ) );
            Bean bean2 = storage.store( new Bean( "test2" ) );
            // rewrite bean2 'test2' with 'test3' name
            bean2 = storage.store( new Bean( bean2.id, "test3" ) );

            log.debug( "bean1 = {}", bean1 );
            log.debug( "bean2 = {}", bean2 );

            assertThat( bean1.id ).isEqualTo( "TST1" );
            assertThat( bean2.id ).isEqualTo( "TST2" );
        }
        try( MongoStorage<Bean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED ) ) {
            storage.start();
            assertThat( storage.select() ).containsOnly(
                new Bean( "TST1", "test1" ),
                new Bean( "TST2", "test3" )
            );
            assertThat( storage.collection.count() ).isEqualTo( 2 );

        }
    }

    @Test
    public void delete() {
        try( MongoStorage<Bean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED ) ) {
            storage.start();
            Bean bean1 = storage.store( new Bean() );
            storage.store( new Bean() );

            storage.delete( bean1.id );
            storage.fsync();

            assertThat( storage.collection.count() ).isEqualTo( 1 );
        }
    }

    @Test()
    public void update() {
        store();
        try( MongoStorage<Bean> storage = new MongoStorage<>( mongoClient, "test", SERIALIZED );
             var oplogService = new OplogService( mongoClient ) ) {
            oplogService.start();
            storage.oplogService = oplogService;
            storage.start();
            Bean bean1 = storage.store( new Bean() );
            Bean bean2 = storage.store( new Bean() );
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
}
