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

package oap.statsdb;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@Slf4j
public class StatsDB extends Node implements RemoteStatsDB {
    public StatsDB() {
        super( -1 );
    }

    private static List<List<String>> merge( Map<String, Node> masterDB, Map<String, Node> remoteDB ) {
        val retList = new ArrayList<List<String>>();
        for( val entry : remoteDB.entrySet() ) {
            val key = entry.getKey();
            val node = entry.getValue();

            val masterNode = masterDB.computeIfAbsent( key, ( k ) -> new Node( DateTimeUtils.currentTimeMillis() ) );

            val ret = masterNode.merge( node );
            if( !ret ) {
                val k = new ArrayList<String>();
                k.add( key );
                retList.add( k );
            }
            val list = merge( masterNode.db, node.db );
            list.forEach( l -> l.add( 0, key ) );

            retList.addAll( list );
        }

        return retList;
    }

    public <TKey extends Key<TKey>, TValue extends Value<TValue>> void update( TKey key, Consumer<TValue> cons ) {
        Node node = this;
        for( val keyItem : key ) {
            node = node.db.computeIfAbsent( keyItem, ( k ) -> new Node( DateTimeUtils.currentTimeMillis() ) );
        }

        node.update( cons );
    }

    @Override
    public synchronized boolean update( Map<String, Node> data, String host ) {
        val failedKeys = merge( db, data );

        if( !failedKeys.isEmpty() ) {
            log.error( "failed keys:" );
            failedKeys.forEach( key -> log.error( "[{}]: {}", host, key ) );
        }

        return true;
    }

    public interface Key<T extends Key> extends Iterable<String> {

    }

    public interface Value<T extends Value> {
        T merge( T other );

        T clone();
    }

}
