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

import com.mongodb.MongoNamespace;
import com.mongodb.annotations.ThreadSafe;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.reflect.TypeRef;
import oap.storage.MemoryStorage;
import oap.storage.Metadata;
import oap.storage.Storage;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Mongo Persistence storage (similar to {@link oap.storage.DirectoryPersistence})
 *
 * @param <T> Type of Metadata
 */
@Slf4j
@ThreadSafe
public class MongoPersistence<T> implements Closeable, Storage.DataListener<T> {

    public static final ReplaceOptions REPLACE_OPTIONS_UPSERT = new ReplaceOptions().upsert( true );
    public final MongoCollection<Metadata<T>> collection;
    private long delay;
    private final Lock lock = new ReentrantLock();
    private MemoryStorage<T> storage;
    private PeriodicScheduled scheduled;

    public MongoPersistence( MongoClientWrapper mongoClient,
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

    public void start() {
        this.load();
        this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), delay, this::fsync );
        this.storage.addDataListener( this );
    }

    /**
     * Load all {@link MongoCollection} documents to the {@link MemoryStorage#data}
     */
    private void load() {
        MongoNamespace namespace = collection.getNamespace();
        log.debug( "loading data from {}", namespace );
        Threads.synchronously( lock, () -> {
            final Consumer<Metadata<T>> cons = metadata -> {
                var id = storage.identifier.get( metadata.object );
                storage.data.put( id, metadata );
            };
            log.info( "Load {} documents from [{}] Mongo namespace", collection.countDocuments(), namespace );
            collection.find().forEach( cons );
        } );
        log.info( storage.data.size() + " object(s) loaded." );
    }

    @Override
    public void updated( T object, boolean added ) {
        Threads.synchronously( lock, () -> {
            var id = storage.identifier.get( object );
            Metadata<T> metadata = storage.data.get( id );
            if (added) {
                collection.insertOne( metadata );
            } else {
                collection.replaceOne( eq( "_id", id ), metadata, REPLACE_OPTIONS_UPSERT );
            }
        } );
    }

    @Override
    public void updated( Collection<T> objects ) {
        objects.forEach( object -> updated( object, false ) );

//        TODO: replace with collection.updateMany()
//         Create BsonDocument of metadata values
//        Map<String, Metadata<T>> data = objects.stream()
//            .map( object -> storage.identifier.get( object ) )
//            .collect( Collectors.toMap( id -> id, id -> storage.data.get( id ) ) );
//
//        collection.updateMany( all( "_id", data.keySet() ), new BsonDocument( data.values() ) )
    }

    @Override
    public void updated( Collection<T> objects, boolean added ) {
        if( added ) {
            Threads.synchronously( lock, () -> {
                List<Metadata<T>> metadataValues = objects.stream()
                    .map( object -> storage.data.get( storage.identifier.get( object ) ) )
                    .collect( Collectors.toList() );
                collection.insertMany( metadataValues );
            } );
        } else {
            updated( objects );
        }
    }

    @Override
    public void deleted( T object ) {
        Threads.synchronously( lock, () -> collection.deleteOne( eq( "_id", storage.identifier.get( object ) ) ) );
    }

    @Override
    public void deleted( Collection<T> objects ) {
        objects.forEach( this::deleted );
        // TODO: rewrite with collection.deleteMany( eq( values ) );
    }

    @Override
    public void fsync() {
        Threads.synchronously( lock, () -> fsync( scheduled.lastExecuted() ) );
    }

    /**
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

    @SneakyThrows
    private void persist( Metadata<T> value ) {
        if (collection.countDocuments( eq( "_id", storage.identifier.get( value.object ) ) ) == 0 ) {
            log.debug( "storing {} with modification time {}", value, value.modified );
            collection.insertOne( value );
            log.trace( "storing {} done", value );
        }
    }

    @Override
    public void close() {
        log.debug( "closing {}...", this );
        Threads.synchronously( lock, () -> {
            Scheduled.cancel( scheduled );
            fsync( scheduled.lastExecuted() );
            storage.close();
        } );
        log.debug( "closed {}", this );
    }
}
