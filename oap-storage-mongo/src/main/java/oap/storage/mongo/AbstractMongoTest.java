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
import oap.storage.Identifier;
import oap.testng.Env;
import oap.testng.Teamcity;
import oap.util.Id;
import org.bson.types.ObjectId;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

@Slf4j
public abstract class AbstractMongoTest {
    protected String dbName;
    protected MongoClient mongoClient;
    protected Identifier<Bean> beanIdentifier =
        Identifier.<Bean>forAnnotation()
            .suggestion( o -> o.name )
            .length( 10 )
            .build();
    protected Identifier<Bean> beanIdentifierWithoutName =
        Identifier.<Bean>forAnnotation()
            .suggestion( ar -> ObjectId.get().toString() )
            .length( 10 )
            .options()
            .build();

    @BeforeMethod
    public void init() {
        dbName = "db" + Teamcity.buildPrefix().replace( ".", "_" );

        mongoClient = new MongoClient( Env.getEnvOrDefault( "MONGO_HOST", "localhost" ), 27017, dbName, Migration.NONE );
        mongoClient.database.drop();
        log.debug( "drop database {}", mongoClient.database.getName() );
    }

    @AfterMethod
    public void done() {
        mongoClient.database.drop();
        mongoClient.close();
    }

    @ToString
    @EqualsAndHashCode
    public static class Bean {
        @Id
        public String id;
        public String name;
        public int c;

        Bean( String id, String name ) {
            this( name );
            this.id = id;
        }

        Bean( String name ) {
            this.name = name;
        }

        Bean() {
        }
    }
}
