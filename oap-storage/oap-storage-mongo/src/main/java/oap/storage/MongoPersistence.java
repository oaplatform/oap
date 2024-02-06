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

import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.mongo.JsonCodec;
import oap.storage.mongo.MongoClient;
import oap.util.Pair;
import oap.util.Stream;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;
import static oap.concurrent.Threads.synchronizedOn;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.util.Pair.__;

@Slf4j
public class MongoPersistence<I, T> extends AbstractPersistance<I, T> implements Closeable, AutoCloseable {

    private static final ReplaceOptions REPLACE_OPTIONS_UPSERT = new ReplaceOptions().upsert( true );
    final MongoCollection<Metadata<T>> collection;
    private final MongoClient mongoClient;

    /**
     * Creates a persistence for Mongo DB
     *
     * @param mongoClient
     * @param collectionName it's a name of 'table'
     * @param delay          in millis
     * @param storage
     */
    public MongoPersistence( MongoClient mongoClient, String collectionName, long delay, MemoryStorage<I, T> storage ) {
        this( mongoClient, collectionName, delay, storage, DEFAULT_CRASH_DUMP_PATH );
    }

    public MongoPersistence( MongoClient mongoClient, String collectionName, long delay, MemoryStorage<I, T> storage, Path crashDumpPath ) {
        super( storage, collectionName, delay, crashDumpPath );
        this.mongoClient = mongoClient;
        TypeRef<Metadata<T>> ref = new TypeRef<>() {};
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonCodec<>( ref,
                m -> this.storage.identifier.get( m.object ),
                this.storage.identifier::toString ) ),
            mongoClient.getCodecRegistry()
        );
        this.collection = mongoClient
            .getCollection( collectionName, ref.clazz() )
            .withCodecRegistry( codecRegistry );

        storage.addDataListener( new Storage.DataListener<I, T>() {
            @Override
            public void permanentlyDeleted( IdObject<I, T> object ) {
                log.debug( "permanentlyDeleted collection {} id {}", tableName, object.id );
                collection.deleteOne( eq( "_id", object.id ) );
            }
        } );
    }

    @Override
    protected void processRecords( CountDownLatch cdl ) {
        var changeStreamDocuments = mongoClient.getCollection( tableName ).withReadConcern( ReadConcern.MAJORITY ).watch();
        cdl.countDown();
        changeStreamDocuments.forEach( ( Consumer<? super ChangeStreamDocument<Document>> ) csd -> {
            log.trace( "mongo notification: {} ", csd );
            var op = csd.getOperationType();
            var key = csd.getDocumentKey();
            if( key == null ) return;

            var bid = key.getString( "_id" );
            if( bid == null ) return;

            var id = bid.getValue();
            switch( op ) {
                case DELETE -> deleteById( id );
                case INSERT, UPDATE -> refreshById( id );
            }
        } );
    }

    @Override
    protected void load() {
        log.debug( "loading data from {}", collection.getNamespace() );
        Consumer<Metadata<T>> cons = metadata -> storage.memory.put( storage.identifier.get( metadata.object ), metadata );
        log.info( "Loading documents from [{}] MongoDB table", collection.getNamespace() );
        collection.find().forEach( cons );
        log.info( storage.size() + " object(s) loaded." );
    }

    @Override
    public void fsync() {
        var time = DateTimeUtils.currentTimeMillis();
        synchronizedOn( lock, () -> {
            if( stopped ) return;
            log.trace( "fsyncing, last: {}, objects in storage: {}", lastExecuted, storage.size() );
            var list = new ArrayList<WriteModel<Metadata<T>>>( batchSize );
            var deletedIds = new ArrayList<I>( batchSize );
            AtomicInteger updated = new AtomicInteger();
            storage.memory.selectUpdatedSince( lastExecuted ).forEach( ( id, m ) -> {
                updated.incrementAndGet();
                if( m.isDeleted() ) {
                    deletedIds.add( id );
                    list.add( new DeleteOneModel<>( eq( "_id", id ) ) );
                } else {
                    list.add( new ReplaceOneModel<>( eq( "_id", id ), m, REPLACE_OPTIONS_UPSERT ) );
                }
                if( list.size() >= batchSize ) {
                    persist( deletedIds, list );
                }
            } );
            log.trace( "fsyncing, last: {}, updated objects in storage: {}, total in storage: {}", lastExecuted, updated.get(), storage.size() );
            persist( deletedIds, list );
            lastExecuted = time;
        } );
    }

    private void persist( List<I> deletedIds, List<WriteModel<Metadata<T>>> list ) {
        if( list.isEmpty() ) return;
        try {
            collection.bulkWrite( list, new BulkWriteOptions().ordered( false ) );
            deletedIds.forEach( storage.memory::removePermanently );
            list.clear();
            deletedIds.clear();
        } catch( Exception e ) {
            Path filename = crashDumpPath.resolve( CRASH_DUMP_PATH_FORMAT_MILLIS.print( DateTimeUtils.currentTimeMillis() ) + ".json.gz" );
            log.error( "cannot persist. Dumping to " + filename + "...", e );
            List<Pair<String, Metadata<T>>> dump = Stream.of( list )
                .filter( model -> model instanceof ReplaceOneModel )
                .map( model -> __( "replace", ( ( ReplaceOneModel<Metadata<T>> ) model ).getReplacement() ) )
                .toList();
            Files.writeString( filename, GZIP, Binder.json.marshal( dump ) );
        }
    }

    private void refreshById( String mongoId ) {
        var m = collection.find( eq( "_id", mongoId ) ).first();
        if( m == null ) return;
        storage.lock.synchronizedOn( mongoId, () -> {
            var id = storage.identifier.fromString( mongoId );
            var old = storage.memory.get( id );
            if( old.isEmpty() || m.modified > old.get().modified ) {
                log.debug( "refresh from mongo {}", mongoId );
                storage.memory.put( id, m );
                if( old.isEmpty() ) storage.fireAdded( id, m.object );
                else storage.fireUpdated( id, m.object );
            } else log.debug( "[{}] m.modified <= oldM.modified", mongoId );
        } );
    }
}
