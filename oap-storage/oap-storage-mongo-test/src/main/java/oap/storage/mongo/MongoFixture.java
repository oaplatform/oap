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

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.ServerVersion;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractEnvFixture;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static oap.testng.Asserts.contentOfTestResource;

@Slf4j
public class MongoFixture extends AbstractEnvFixture<MongoFixture> {
    public final int port;
    public final String database;
    public final String host;
    private MongoClient mongoClient;
    private MongoServer server;

    public MongoFixture() {
        this( "test" );
    }

    public MongoFixture( String database ) {
        this.database = database;

        define( "MONGO_PORT", port = portFor( "MONGO_PORT" ) );
        define( "MONGO_HOST", host = "localhost" );
        define( "MONGO_DATABASE", database );
    }

    @Override
    protected void before() {
        super.before();

        this.server = createMongoServer();
        log.info( "mongo port = {}", port );
        this.server.bind( host, port );
        this.mongoClient = createMongoClient();
    }

    @NotNull
    public MongoClient createMongoClient() {
        return new MongoClient( getConnectionString() );
    }

    @NotNull
    public MongoClient createMongoClient( String migrationPackage ) {
        return new MongoClient( getConnectionString(), migrationPackage );
    }

    @NotNull
    public String getConnectionString() {
        return getConnectionString( database );
    }

    @NotNull
    public String getConnectionString( String database ) {
        return "mongodb://" + host + ":" + port + "/" + database;
    }

    @NotNull
    protected MongoServer createMongoServer() {
        return new MongoServer( new MemoryBackend().version( ServerVersion.MONGO_3_6 ) );
    }

    @Override
    protected void after() {
        this.mongoClient.close();
        this.server.shutdownNow();

        super.after();
    }

    public void insertDocument( Class<?> contextClass, String collection, String resourceName ) {
        this.mongoClient.getCollection( collection ).insertOne( Document.parse( contentOfTestResource( contextClass, resourceName, Map.of() ) ) );
    }

    public void initializeVersion( Version version ) {
        this.mongoClient.updateVersion( version );
    }

    public MongoClient client() {
        return mongoClient;
    }

    public void dropDatabase( String database ) {
        try( MongoClient mongoClient = new MongoClient( getConnectionString( database ) ) ) {
            mongoClient.dropDatabase();
        }
    }
}
