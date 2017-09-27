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
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.json.Binder;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.Document;
import org.joda.time.DateTimeUtils;

import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by igor.petrenko on 15.09.2017.
 */
@Slf4j
public class MongoStorage<T> extends MemoryStorage<T> implements Runnable {
    final MongoDatabase database;
    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;
    private final TypeReference<Metadata<T>> typeReference;
    private long lastFsync;

    public MongoStorage( String host, int port, String database, String table, Identifier<T> identifier,
                         TypeReference<Metadata<T>> typeReference ) {
        super( identifier );
        this.typeReference = typeReference;

        mongoClient = new MongoClient( new ServerAddress( host, port ) );

        this.database = mongoClient.getDatabase( database );
        this.collection = this.database.getCollection( table, Document.class );

        load();
    }


    private void load() {
        lastFsync = DateTimeUtils.currentTimeMillis();

        final Consumer<Document> cons = item -> {
            val metadata = Binder.json.unmarshal( typeReference, item );
            data.put( metadata.id, metadata );
        };

        log.info( "total {}", collection.count() );

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

        lastFsync = System.currentTimeMillis();

        val count = new MutableInt();

        data.values()
            .stream()
            .filter( m -> m.modified >= lastFsync )
            .forEach( metadata -> {
                count.increment();
                collection.replaceOne(
                    eq( "_id", metadata.id ),
                    Document.parse( Binder.json.marshal( metadata ) ),
                    new UpdateOptions().upsert( true ) );
            } );

        log.info( "fsync total: {}, modified: {}", size(), count.intValue() );
    }

    @Override
    public void run() {
        fsync();
    }
}
