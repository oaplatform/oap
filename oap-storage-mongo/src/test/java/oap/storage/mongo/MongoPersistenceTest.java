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
import oap.concurrent.Threads;
import oap.storage.Identifier;
import oap.storage.MemoryStorage;
import oap.storage.MongoPersistence;
import org.testng.annotations.Test;

import static oap.storage.Storage.Lock.SERIALIZED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * It requires installed MongoDB on the machine
 *
 * @see <a href="https://docs.mongodb.com/manual/administration/install-community/">Install MongoDB Community Edition</a>
 */
@Slf4j
public class MongoPersistenceTest extends AbstractMongoTest {

    @Test
    public void store() {
        try {
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {

                persistence.start();
                Bean bean1 = storage.store( new Bean( "test1" ) );
                Bean bean2 = storage.store( new Bean( "test2" ) );
                // rewrite bean2 'test2' with 'test3' name
                bean2 = storage.store( new Bean( bean2.id, "test3" ) );

                log.debug( "bean1 = {}", bean1 );
                log.debug( "bean2 = {}", bean2 );

                assertThat( bean1.id ).isEqualTo( "TST1XXXXXX" );
                assertThat( bean2.id ).isEqualTo( "TST2XXXXXX" );
            }

            // Make sure that for a new connection the objects still present in MongoDB
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                assertThat( storage.select() ).containsOnly(
                    new Bean( "TST1XXXXXX", "test1" ),
                    new Bean( "TST2XXXXXX", "test3" )
                );
                assertThat( persistence.collection.count() ).isEqualTo( 2 );
            }
        } finally {
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                storage.deleteAll();
            }
        }

    }

    @Test
    public void delete() {
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifierWithoutName, SERIALIZED );
             MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 50, storage ) ) {
            persistence.start();
            Bean bean1 = storage.store( new Bean() );
            storage.store( new Bean() );

            storage.delete( bean1.id );
            Threads.sleepSafely( 100 );

            assertThat( persistence.collection.count() ).isEqualTo( 1 );
        } finally {
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                storage.deleteAll();
            }
        }
    }

    @Test()
    public void update() {
        try {
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                storage.store( new Bean( "111", "initialName" ) );
                storage.update( "111", bean -> {
                    bean.name = "newName";
                    return bean;
                } );
            }

            try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                assertThat( storage.select() )
                    .containsExactly( new Bean( "111", "newName" ) );
            }
        } finally {
            try( MemoryStorage<Bean> storage = new MemoryStorage<>( beanIdentifier, SERIALIZED );
                 MongoPersistence<Bean> persistence = new MongoPersistence<>( mongoClient, "test", 6000, storage ) ) {
                persistence.start();
                storage.deleteAll();
            }
        }
    }
}
