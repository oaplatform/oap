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

import com.google.common.base.Preconditions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import io.mongock.driver.mongodb.sync.v4.driver.MongoSync4Driver;
import io.mongock.runner.standalone.MongockStandalone;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@ToString( exclude = { "migrations", "shell", "mongoClient", "database" } )
public class MongoClient implements Closeable {
    final com.mongodb.client.MongoClient mongoClient;
    private final MongoDatabase database;
    private ConnectionString connectionString;
    private final String migrationPackage;

    public MongoClient( String connectionString ) {
        this( connectionString, null );
    }

    public MongoClient( String connectionString, @Nonnull String migrationPackage ) {
        this.connectionString = new ConnectionString( connectionString );
        this.migrationPackage = migrationPackage;

        Preconditions.checkNotNull( this.connectionString.getDatabase(), "database is required" );

        final MongoClientSettings.Builder settingsBuilder = defaultBuilder()
            .applyConnectionString( this.connectionString );
        this.mongoClient = MongoClients.create( settingsBuilder.build() );
        this.database = mongoClient.getDatabase( this.connectionString.getDatabase() );
        log.debug( "creating connectionString {} migrationPackage {}",
            this.connectionString, migrationPackage );
    }

    private MongoClientSettings.Builder defaultBuilder() {
        return MongoClientSettings.builder()
            .codecRegistry( CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs( new JodaTimeCodec() ),
                MongoClientSettings.getDefaultCodecRegistry() ) );
    }

    /**
     * Runs action with given collection if it exists, skipping action otherwise.
     *
     * @param collectionName name of collection in MongoDB database
     * @param consumer       lamda action to be performed
     * @param <R>
     * @return result of function or null otherwise
     */
    public <R> Optional<R> doWithCollectionIfExist( String collectionName, Function<MongoCollection<Document>, R> consumer ) {
        Objects.requireNonNull( collectionName );
        if( collectionExists( collectionName ) ) {
            var collection = this.getCollection( collectionName );
            return Optional.of( consumer.apply( collection ) );
        }
        return Optional.empty();
    }

    public boolean collectionExists( String collection ) {
        Objects.requireNonNull( collection );
        return database
            .listCollectionNames()
            .into( new ArrayList<>() )
            .contains( collection );
    }

    public void preStart() {
        try {
            MongoSync4Driver driver = MongoSync4Driver.withDefaultLock( mongoClient, database.getName() );
            driver.disableTransaction();

            if( migrationPackage != null ) {
                MongockStandalone
                    .builder()
                    .addMigrationScanPackage( migrationPackage )
                    .setDriver( driver )
                    .buildRunner()
                    .execute();

            }
        } catch( Exception ex ) {
            log.error( "Cannot perform migration" );
            log.error( ex.getMessage(), ex );
        }
    }

    public CodecRegistry getCodecRegistry() {
        return database.getCodecRegistry();
    }

    public <T> MongoCollection<T> getCollection( String collection, Class<T> clazz ) {
        return database.getCollection( collection, clazz );
    }

    public MongoCollection<Document> getCollection( String collection ) {
        return database.getCollection( collection );
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    public void updateVersion( Version version ) {
        getCollection( "version" ).replaceOne( new Document( "_id", "version" ),
            new Document( Map.of( "main", version.main, "ext", version.ext ) ),
            new ReplaceOptions().upsert( true ) );
    }

    public void dropDatabase() {
        log.debug( "dropping database {}", this );
        this.database.drop();
    }
}
