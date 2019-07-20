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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.TypeRef;
import oap.storage.Identifier;
import oap.storage.MemoryStorage;
import oap.storage.Metadata;
import oap.util.Lists;
import oap.util.Stream;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.joda.time.DateTimeUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

@Deprecated
@Slf4j
public class MongoStorage<T> extends MemoryStorage<T> implements Runnable, OplogService.OplogListener {
    public static final UpdateOptions UPDATE_OPTIONS_UPSERT = new UpdateOptions().upsert( true );
    public final MongoCollection<Metadata<T>> collection;
    public int bulkSize = 1000;
    public OplogService oplogService;
    private long lastFsync = -1;

    public MongoStorage( MongoClient mongoClient, String table, Lock lock ) {
        this( mongoClient, table,
            Identifier.<T>forAnnotation()
                .suggestion( ar -> ObjectId.get().toString() )
                .length( 24 )
                .options()
                .build(),
            lock );
    }

    public MongoStorage( MongoClient mongoClient, String table, Identifier<T> identifier, Lock lock ) {
        super( identifier, lock );

        TypeRef<Metadata<T>> ref = new TypeRef<>() {};

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonCodec<>( ref, m -> this.identifier.get( m.object ) ) ),
            mongoClient.database.getCodecRegistry()
        );

        this.collection = mongoClient.database
            .getCollection( table, ref.clazz() )
            .withCodecRegistry( codecRegistry );
    }

    private void load() {
        lastFsync = DateTimeUtils.currentTimeMillis();

        final Consumer<Metadata<T>> cons = metadata -> {
            var id = identifier.get( metadata.object );
            data.put( id, metadata );
        };

        log.info( "[{}] total {}", collection.getNamespace(), collection.count() );

        collection.find().forEach( cons );
    }

    public void start() {
        load();

        if( oplogService != null ) oplogService.addListener( collection.getNamespace().getCollectionName(), this );
    }

    @Override
    public void close() {
        fsync();

        super.close();
    }

    @Deprecated( forRemoval = true )
    public void fsync() {
        for( DataListener<T> dataListener : this.dataListeners ) dataListener.fsync();

        var count = new MutableInt();

        Stream.of( data.values()
            .stream()
            .filter( m -> m.modified >= lastFsync ) )
            .grouped( bulkSize )
            .forEach( list -> {
                count.add( list.size() );

                final List<? extends WriteModel<Metadata<T>>> bulk = Lists.map( list,
                    metadata -> {
                        var id = identifier.get( metadata.object );
                        return new ReplaceOneModel<>( eq( "_id", id ), metadata, UPDATE_OPTIONS_UPSERT );
                    } );
                collection.bulkWrite( bulk );

            } );

        log.info( "[{}] fsync total: {}, modified: {}", collection.getNamespace(), size(), count.intValue() );
        lastFsync = System.currentTimeMillis();
    }

    @Override
    public void run() {
        fsync();
    }

    @Override
    public Optional<T> delete( String id ) {
        collection.deleteOne( eq( "_id", id ) );

        return super.delete( id );
    }

    @Override
    public void updated( String table, String id ) {
        refresh( id );
    }

    public void refresh( String id ) {
        var m = collection.find( eq( "_id", id ) ).first();
        if( m != null ) {
            lock.synchronizedOn( id, () -> {
                var oldM = data.get( id );
                if( oldM == null || m.modified > oldM.modified ) {
                    log.debug( "refresh from mongo {}", id );
                    data.put( id, m );
                    fireUpdated( m.object, false );
                } else {
                    log.debug( "[{}] m.modified <= oldM.modified", id );
                }
            } );
        }
    }

    @Override
    public void deleted( String table, String id ) {
        delete( id );
    }

    @Override
    public void inserted( String table, String id ) {
        refresh( id );
    }
}
