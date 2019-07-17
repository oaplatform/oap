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

package oap.storage;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.reflect.TypeRef;
import oap.storage.mongo.JsonCodec;
import oap.storage.mongo.MongoClient;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

/**
 * Mongo Persistence storage (similar to {@link oap.storage.DirectoryPersistence})
 *
 * @param <T> Type of Metadata
 */
@Slf4j
public class MongoPersistence<T> implements Closeable, Runnable, Storage.DataListener<T> {

    public static final ReplaceOptions REPLACE_OPTIONS_UPSERT = new ReplaceOptions().upsert( true );
    public final MongoCollection<Metadata<T>> collection;
    private long delay;
    private final Lock lock = new ReentrantLock();
    private MemoryStorage<T> storage;
    private PeriodicScheduled scheduled;

    public MongoPersistence( MongoClient mongoClient,
                             String table,
                             long delay,
                             MemoryStorage<T> storage ) {
        this.delay = delay;
        this.storage = storage;

        TypeRef<Metadata<T>> ref = new TypeRef<>() {};

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonCodec<>( ref, m -> this.storage.identifier.get( m.object ) ) ),
            mongoClient.database.getCodecRegistry()
        );

        this.collection = mongoClient.database
            .getCollection( table, ref.clazz() )
            .withCodecRegistry( codecRegistry );
    }

    @Override
    public void run() {
        fsync();
    }

    public void start() {
        Threads.synchronously( lock, () -> {
            this.load();
            this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), delay, this::fsync );
            this.storage.addDataListener( this );
        } );
    }

    /**
     * Load all {@link MongoCollection} documents to the {@link MemoryStorage#data}
     */
    private void load() {
        MongoNamespace namespace = collection.getNamespace();
        log.debug( "loading data from {}", namespace );
        final Consumer<Metadata<T>> cons = metadata -> {
            var id = storage.identifier.get( metadata.object );
            storage.data.put( id, metadata );
        };
        log.info( "Load {} documents from [{}] Mongo namespace", collection.countDocuments(), namespace );
        collection.find().forEach( cons );
        log.info( storage.data.size() + " object(s) loaded." );
    }

/*
// It can be used for synchronous operations executing for in-memory storage and MongoPersistence

    @Override
    public void updated( T object, boolean added ) {
        var id = storage.identifier.get( object );
        Metadata<T> metadata = storage.data.get( id );
        if (added) {
            collection.insertOne( metadata );
        } else {
            collection.replaceOne( eq( "_id", id ), metadata, REPLACE_OPTIONS_UPSERT );
        }
    }

    @Override
    public void updated( Collection<T> objects ) {
        objects.forEach( object -> updated( object, false ) );

//        TODO: replace with collection.updateMany()
//         Create BsonDocument of metadata values
//        Map<String, Metadata<T>> data = objects.stream()
//            .map( object -> storage.identifier.get( object ) )
//            .collect( Collectors.toMap( id -> id, id -> storage.data.get( id ) ) );
//        collection.updateMany( all( "_id", data.keySet() ), new BsonDocument( data.values() ) )
    }

    @Override
    public void updated( Collection<T> objects, boolean added ) {
        if( added ) {
            List<Metadata<T>> metadataValues = objects.stream()
                .map( object -> storage.data.get( storage.identifier.get( object ) ) )
                .collect( Collectors.toList() );
            collection.insertMany( metadataValues );
        } else {
            updated( objects );
        }
    }
*/

    @Override
    public void deleted( T object ) {
        collection.deleteOne( eq( "_id", storage.identifier.get( object ) ) );
    }

    @Override
    public void deleted( Collection<T> objects ) {
        objects.forEach( this::deleted );
        // TODO: rewrite with collection.deleteMany( eq( values ) );
    }

    @Override
    public void fsync() {
        fsync( scheduled.lastExecuted() );
    }

    /**
     * It persists the objects modified earlier than {@code last} to MongoDB
     *
     * @param last executed sync
     * TODO: avoid explicit usage of fsync
     */
    private void fsync( long last ) {
        Threads.synchronously( lock, () -> {
            log.trace( "fsyncing, last: {}, storage length: {}", last, storage.data.size() );
            for( var value : storage.data.values() ) {
                if( value.modified >= last ) persist( value );
            }
        } );
    }

    /**
     * Upsert (replace or insert) metadata objects in MongoDB
     *
     * @param metadata
     */
    @SneakyThrows
    private void persist( Metadata<T> metadata ) {
        var id = storage.identifier.get( metadata.object );
        log.debug( "Replacing {} with modification time {}", id, metadata.modified );
        UpdateResult result = collection.replaceOne( eq( "_id", id ), metadata, REPLACE_OPTIONS_UPSERT );
        log.trace( "Replacing done. {}", result );
    }

    @Override
    public void close() {
        log.debug( "Closing {}...", this );
        if( scheduled != null && storage != null ) {
            Threads.synchronously( lock, () -> {
                Scheduled.cancel( scheduled );
                fsync( scheduled.lastExecuted() );
                storage.close();
                log.debug( "Closed {}", this );
            } );
        } else {
            log.debug( "This {} was't started or already closed", this );
        }
    }
}
