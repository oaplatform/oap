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
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.regex;

/**
 * Created by igor.petrenko on 13.02.2018.
 */
@Slf4j
public class OplogService implements Runnable, Closeable {
    private final MongoClient mongoClient;
    private final ListMultimap<String, OplogListener> listeners = MultimapBuilder.hashKeys().arrayListValues().build();
    private MongoCursor<Document> cursor;


    public OplogService( MongoClient mongoClient ) {
        this.mongoClient = mongoClient;
    }

    public void addListener( String table, OplogListener listener ) {
        listeners.put( table, listener );
    }

    public void start() {
        val oplogRs = mongoClient.mongoClient.getDatabase( "local" ).getCollection( "oplog.rs" );
        cursor = oplogRs
            .find( and(
                in( "op", "i", "u", "d" ),
                gt( "ts", new BsonTimestamp( ( int ) ( DateTimeUtils.currentTimeMillis() / 1000 ), 0 ) ),
                regex( "ns", "^" + mongoClient.database.getName() + "\\." )
            ) )
            .sort( new Document( "$natural", 1 ) )
            .cursorType( CursorType.TailableAwait )
            .noCursorTimeout( true )
            .iterator();
    }

    @Override
    public void close() {
        cursor.close();
    }

    @Override
    public void run() {
        try {
            while( cursor.hasNext() ) {
                val document = cursor.next();
                log.trace( "oplog {}", document );

                val operation = document.getString( "op" ).charAt( 0 );
                val tableName = getTableName( document );
                switch( operation ) {
                    case 'i': {
                        val objO = ( Document ) document.get( "o" );
                        val id = objO.get( "_id" );
                        val l = listeners.get( tableName );
                        l.forEach( ll -> ll.inserted( tableName, id ) );
                        break;
                    }
                    case 'u': {
                        val objO2 = ( Document ) document.get( "o2" );
                        val id = objO2.get( "_id" );
                        val l = listeners.get( tableName );
                        l.forEach( ll -> ll.updated( tableName, id ) );
                        break;
                    }
                    case 'd': {
                        val objO = ( Document ) document.get( "o" );
                        val id = objO.get( "_id" );
                        val l = listeners.get( tableName );
                        l.forEach( ll -> ll.deleted( tableName, id ) );
                        break;
                    }
                    default:
                }
            }
        } catch( IllegalStateException ise ) {
            log.debug( ise.getMessage() );
        }
    }

    private String getTableName( Document document ) {
        val fns = document.getString( "ns" );
        val tableIndex = fns.lastIndexOf( '.' );

        return fns.substring( tableIndex + 1 );
    }

    public interface OplogListener {
        void updated( String table, Object id );

        void deleted( String table, Object id );

        void inserted( String table, Object id );
    }
}
