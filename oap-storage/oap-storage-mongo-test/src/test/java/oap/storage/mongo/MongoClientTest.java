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

import oap.testng.Fixtures;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class MongoClientTest extends Fixtures {
    private final MongoFixture mongoFixture;

    public MongoClientTest() {
        fixture( mongoFixture = new MongoFixture() );
    }

    @Test
    public void instantiationWithoutCredentialsInConnectionString() {
        try {
            new MongoClient( String.format( "mongodb://%s:%s/%s", mongoFixture.host, mongoFixture.port, mongoFixture.database ) );
        } catch( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    public void migration() {
        try( MongoClient client = mongoFixture.createMongoClient( "oap.storage.mongo.mongoclienttest" ) ) {
            client.preStart();

            assertThat( getDocumentField( client, "test", "c" ) ).isEqualTo( 17 );
            assertThat( getDocumentField( client, "test3", "v" ) ).isEqualTo( 1 );
        }
    }

    private static Integer getDocumentField( MongoClient mongoClient, String id, String field ) {
        return mongoClient.doWithCollectionIfExist( "test", collection ->
            Objects.requireNonNull( collection
                .find( eq( "_id", id ) )
                .first() ).getInteger( field ) ).orElseThrow();
    }
}
