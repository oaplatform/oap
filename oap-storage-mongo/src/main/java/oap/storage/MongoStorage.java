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

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

/**
 * Created by igor.petrenko on 15.09.2017.
 */
@Slf4j
public class MongoStorage<T> extends MemoryStorage<T> implements Runnable {
    public static final UpdateOptions UPDATE_OPTIONS_UPSERT = new UpdateOptions().upsert( true );
    public final MongoCollection<Metadata<T>> collection;
    public final MongoDatabase database;
    public int bulkSize = 1000;
    private long lastFsync = -1;

    @SuppressWarnings( "unchecked" )
    public MongoStorage( oap.storage.MongoClient mongoClient, String database, String table,
                         LockStrategy lockStrategy, Class<T> clazz ) {
        super( IdentifierBuilder
            .annotation()
            .suggestion( ar -> ObjectId.get().toString() )
            .size( 24 )
            .idOptions()
            .build(), lockStrategy );
        this.database = mongoClient.getDatabase( database );


        final Object o = new TypeReference<Metadata<T>>() {};
        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonCodec<>( ( TypeReference<Metadata> ) o,
                Metadata.class, clazz, ( m ) -> identifier.get( ( T ) m.object ) ) ),
            this.database.getCodecRegistry()
        );

        final Object metadataMongoCollection = this.database
            .getCollection( table, Metadata.class )
            .withCodecRegistry( codecRegistry );
        this.collection = ( MongoCollection<Metadata<T>> ) metadataMongoCollection;

        load();
    }

    private void load() {
        lastFsync = DateTimeUtils.currentTimeMillis();

        final Consumer<Metadata<T>> cons = metadata -> {
            val id = identifier.get( metadata.object );
            data.put( id, metadata );
        };

        log.info( "[{}] total {}", collection.getNamespace(), collection.count() );

        collection.find().forEach( cons );
    }

    @Override
    public void close() {
        fsync();

        super.close();
    }

    @Override
    public void fsync() {
        super.fsync();

        val count = new MutableInt();

        Stream.of( data.values()
            .stream()
            .filter( m -> m.modified >= lastFsync ) )
            .grouped( bulkSize )
            .forEach( list -> {
                count.add( list.size() );

                final List<? extends WriteModel<Metadata<T>>> bulk = Lists.map( list,
                    metadata -> {
                        val id = identifier.get( metadata.object );
                        return new ReplaceOneModel<>( eq( "_id", new ObjectId( id ) ), metadata, UPDATE_OPTIONS_UPSERT );
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
        collection.deleteOne( eq( "_id", new ObjectId( id ) ) );

        return super.delete( id );
    }
}
