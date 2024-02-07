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

package oap.storage.mongo.mongoclienttest;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;

import java.util.Map;


@ChangeUnit( id = "MongoClientMigration", order = "1", systemVersion = "1" )
public class MongoClientMigration {
    @Execution
    public void execution( MongoDatabase mongoDatabase ) {
        mongoDatabase
            .getCollection( "test" )
            .insertOne( new Document( Map.of( "_id", "test", "c", 17 ) ) );

        mongoDatabase
            .getCollection( "test" )
            .insertOne( new Document( Map.of( "_id", "test3", "v", 1 ) ) );
    }

    @RollbackExecution
    public void rollback( MongoDatabase mongoDatabase ) {
        mongoDatabase.getCollection( "test" )
            .deleteOne( Filters.eq( "_id", "test" ) );
        mongoDatabase.getCollection( "test" )
            .deleteOne( Filters.eq( "_id", "test3" ) );
    }
}