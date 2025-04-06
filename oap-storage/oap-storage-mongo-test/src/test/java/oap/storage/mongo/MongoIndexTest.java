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

import com.mongodb.client.MongoCollection;
import oap.testng.Fixtures;
import oap.util.LinkedHashMaps;
import org.bson.Document;
import org.testng.annotations.Test;

import java.util.Map;

import static java.util.List.of;
import static oap.storage.mongo.MongoIndex.IndexConfiguration.Direction.ASC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Created by igor.petrenko on 2020-11-02.
 */
public class MongoIndexTest extends Fixtures {
    private final MongoFixture mongoFixture;

    public MongoIndexTest() {
        fixture( mongoFixture = new MongoFixture( "MONGO" ) );
    }

    @Test
    public void testUpdateCreateNewIndex() {
        try( MongoClient client = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" ) ) {
            MongoCollection<Document> collection = client.getCollection( "test" );
            MongoIndex mongoIndex = new MongoIndex( collection );

            mongoIndex.update( "idx1", of( "a" ), true, 10000L );
            mongoIndex.update( "idx1", of( "a", "b" ), true, null );
            mongoIndex.update( "idx1", of( "a", "b" ), true, null );
            mongoIndex.update( "idx1", of( "a", "b" ), false, null );

            mongoIndex.update( "idx2", of( "c" ), false, 1000L );
            mongoIndex.update( "idx2", of( "c" ), false, 11001L );

            MongoIndex.IndexConfiguration info = mongoIndex.getInfo( "idx1" );
            assertNotNull( info );
            assertFalse( info.unique );
            assertThat( info.keys ).containsExactly( entry( "a", ASC ), entry( "b", ASC ) );
            assertNull( info.expireAfter );

            info = mongoIndex.getInfo( "idx2" );
            assertNotNull( info );
            assertThat( info.expireAfter ).isEqualTo( 11000L );
        }
    }

    @Test
    public void testSync() {
        try( MongoClient client = mongoFixture.createMongoClient( "oap.storage.mongo.mongomigrationtest" ) ) {
            MongoCollection<Document> collection = client.getCollection( "test" );
            MongoIndex mongoIndex = new MongoIndex( collection );

            mongoIndex.update( "idx1", of( "a" ), true, 1000L );
            mongoIndex.update( "idx2", of( "b" ), true, 10000L );


            mongoIndex.update( Map.of( "idx1", new MongoIndex.IndexConfiguration( LinkedHashMaps.of( "c", 1, "d", 1 ), false, null ) ) );

            assertNull( mongoIndex.getInfo( "idx2" ) );
            assertNotNull( mongoIndex.getInfo( "idx1" ) );
            assertFalse( mongoIndex.getInfo( "idx1" ).unique );
            assertNull( mongoIndex.getInfo( "idx1" ).expireAfter );
            assertThat( mongoIndex.getInfo( "idx1" ).keys ).containsExactly( entry( "c", ASC ), entry( "d", ASC ) );
        }
    }
}
