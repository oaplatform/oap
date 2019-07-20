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

import oap.testng.Env;
import org.bson.Document;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryMigrationTest extends AbstractMongoTest {
    @BeforeMethod
    public void deploy() {
        Env.deployTestData( getClass() );
    }

    @Test
    public void migration() {
        var migration = new DirectoryMigration( Env.deployTestData( getClass() ) );
        migration.variables.put( "testB", "true" );
        migration.variables.put( "testS", "\"true\"" );

        migration.run( mongoClient.database );

        final Document version = mongoClient.database.getCollection( "version" ).find( eq( "_id", "version" ) ).first();
        assertThat( version ).isNotNull();
        assertThat( version.get( "value" ) ).isEqualTo( 10 );

        value( "test", "test", "c", 17 );
        value( "test", "test3", "v", 1 );

        migration.run( mongoClient.database );
        value( "test", "test", "c", 17 );
        value( "test", "test3", "v", 1 );
    }

    public void value( String collection, String id, String actual, int expected ) {
        assertThat( mongoClient.database.getCollection( collection ).find( eq( "_id", id ) ).first().getInteger( actual ) )
            .isEqualTo( expected );
    }
}
