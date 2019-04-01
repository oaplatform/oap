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
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

@Slf4j
public abstract class AbstractMongoTest extends AbstractTest {
    protected String dbName;
    protected MongoClientWrapper mongoClient;

    @BeforeMethod
    public void init() {
        dbName = "db" + Env.teamcityBuildPrefix().replace( ".", "_" );

        mongoClient = new MongoClientWrapper( Env.getEnvOrDefault( "MONGO_HOST", "localhost" ), 27017, dbName, Migration.NONE );
        mongoClient.database.drop();
        log.debug( "drop database {}", mongoClient.database.getName() );
    }

    @AfterMethod
    public void done() {
        mongoClient.database.drop();
        mongoClient.close();
    }
}
