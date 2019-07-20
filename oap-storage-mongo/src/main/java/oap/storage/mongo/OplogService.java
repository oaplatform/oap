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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.mongodb.CursorType;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.regex;

/**
 * This service runs within a separate {@link Thread} and detects the changes on the Mongo collection via
 * {@link OplogListener}
 */
@Slf4j
public class OplogService implements Runnable, Closeable {
    private final MongoClient mongoClient;
    private final ListMultimap<String, OplogListener> listeners = MultimapBuilder.hashKeys().arrayListValues().build();
    private MongoCursor<Document> cursor;
    private Thread thread;
    private boolean running;


    public OplogService( MongoClient mongoClient ) {
        this.mongoClient = mongoClient;
    }

    public synchronized void addListener( String table, OplogListener listener ) {
        listeners.put( table, listener );
        if( cursor != null ) {
            close();
            start();
        }
    }

    /**
     * Starts Mongo OpLog Service
     */
    public synchronized void start() {
        log.debug( "starting oplog listening {} for {}", this, mongoClient.database.getName() );
        running = true;
        var oplogRs = mongoClient.mongoClient.getDatabase( "local" ).getCollection( "oplog.rs" );
        final Bson filter = and(
            in( "op", "i", "u", "d" ),
            gt( "ts", new BsonTimestamp( ( int ) ( DateTimeUtils.currentTimeMillis() / 1000 ), 0 ) ),
            regex( "ns", "^" + mongoClient.database.getName() + "\\." )
        );
        cursor = oplogRs
            .find( filter )
            .sort( new Document( "$natural", 1 ) )
            .cursorType( CursorType.TailableAwait )
            .noCursorTimeout( true )
            .iterator();

        log.debug( "filter = {}", filter );

        thread = new Thread( this );
        thread.start();
    }

    @Override
    public void close() {
        stop();
    }

    public synchronized void stop() {
        log.debug( "stopping oplog listening {} for {}", this, mongoClient.database.getName() );
        if( thread != null ) {
            thread.interrupt();
            running = false;
            thread = null;
        }
        if( cursor != null ) cursor.close();
    }

    @Override
    public void run() {
        try {
            while( cursor.hasNext() && running ) {
                var document = cursor.next();
                log.trace( "oplog {}", document );

                var operation = document.getString( "op" ).charAt( 0 );
                var tableName = getTableName( document );
                switch( operation ) {
                    case 'i': {
                        var objO = ( Document ) document.get( "o" );
                        var id = objO.get( "_id" ).toString();
                        var l = listeners.get( tableName );
                        l.forEach( ll -> ll.inserted( tableName, id ) );
                        break;
                    }
                    case 'u': {
                        var objO2 = ( Document ) document.get( "o2" );
                        var id = objO2.get( "_id" ).toString();
                        var l = listeners.get( tableName );
                        l.forEach( ll -> ll.updated( tableName, id ) );
                        break;
                    }
                    case 'd': {
                        var objO = ( Document ) document.get( "o" );
                        var id = objO.get( "_id" ).toString();
                        var l = listeners.get( tableName );
                        l.forEach( ll -> ll.deleted( tableName, id ) );
                        break;
                    }
                    default:
                }
            }
        } catch( MongoInterruptedException ignored ) {
        } catch( IllegalStateException ise ) {
            log.debug( ise.getMessage() );
        }
    }

    private String getTableName( Document document ) {
        var fns = document.getString( "ns" );
        var tableIndex = fns.lastIndexOf( '.' );

        return fns.substring( tableIndex + 1 );
    }

    /**
     * Listener interface, which listens changes on the MongoStorage and do the appropriate changes (update, delete,
     * insert) in the other storages, for example internal {@link oap.storage.MemoryStorage}
     */
    public interface OplogListener {
        void updated( String table, String id );

        void deleted( String table, String id );

        void inserted( String table, String id );
    }
}
