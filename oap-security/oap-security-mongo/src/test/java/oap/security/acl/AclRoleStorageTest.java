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

package oap.security.acl;

import lombok.val;
import oap.storage.mongo.Migration;
import oap.storage.mongo.MongoClient;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.mongodb.client.model.Filters.eq;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 22.12.2017.
 */
public class AclRoleStorageTest extends AbstractTest {
    private AclRoleStorage storage;
    private MongoClient mongoClient;

    @BeforeMethod
    public void init() {
        val dbName = "db" + Env.teamcityBuildPrefix().replace( ".", "_" );

        mongoClient = new MongoClient( Env.getEnvOrDefault( "MONGO_HOST", "localhost" ), 27017, dbName, Migration.NONE );
        mongoClient.database.drop();

        storage = new AclRoleStorage( mongoClient, "roles" );
    }

    @Test
    public void testId() {
        val role = storage.store( new AclRole( "role1", singletonList( "test.permission" ) ) );
        storage.fsync();
        val role2 = storage.collection.find( eq( "_id", role.id ) ).first();

        assertThat( role2.object ).isEqualTo( role );
    }

    @AfterMethod
    public void done() {
        mongoClient.database.drop();
        storage.close();
        mongoClient.close();
    }
}
