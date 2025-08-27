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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 2020-11-02.
 */
@Slf4j
public class MongoIndex {
    private final MongoCollection<?> collection;
    private final LinkedHashMap<String, IndexConfiguration> indexes = new LinkedHashMap<>();

    public MongoIndex( MongoCollection<?> collection ) {
        this.collection = collection;
        refresh();
    }

    public void refresh() {
        ArrayList<Document> indexes = new ArrayList<>();
        collection.listIndexes().into( indexes );

        log.debug( "refreshing indexes: {} ...", indexes );

        this.indexes.clear();

        for( Document indexDoc : indexes ) {
            Pair<String, IndexConfiguration> info = IndexConfiguration.parse( indexDoc );
            this.indexes.put( info._1, info._2 );
        }

        log.debug( "new indexes: {}", this.indexes );
    }

    public void update( SequencedMap<String, IndexConfiguration> indexConfigurations ) {
        try {
            Set<String> expected = indexConfigurations.keySet();

            for( String name : new ArrayList<>( indexes.keySet() ) ) {
                if( !"_id_".equals( name ) && !expected.contains( name ) ) {
                    collection.dropIndex( name );
                    this.indexes.remove( name );
                }
            }

            indexConfigurations.forEach( ( name, conf ) -> {
                update( name, conf.keys, conf.unique, conf.expireAfter, false );
            } );

        } finally {
            refresh();
        }
    }

    public boolean update( String indexName, SequencedMap<String, IndexConfiguration.Direction> keys, boolean unique, Long expireAfter ) {
        return update( indexName, keys, unique, expireAfter, true );
    }

    private boolean update( String indexName, SequencedMap<String, IndexConfiguration.Direction> keys, boolean unique, Long expireAfter, boolean refresh ) {
        try {
            log.info( "Creating index {}, keys={}, unique={}, expireAfter={}...",
                indexName, keys, unique,
                expireAfter != null ? Dates.durationToString( expireAfter ) : "-" );
            IndexConfiguration info = this.indexes.get( indexName );
            if( info != null ) {
                if( info.equals( keys, unique, expireAfter ) ) {
                    log.info( "Creating index {}, keys={}, unique={}, expireAfter={}...... Already exists",
                        indexName, keys, unique,
                        expireAfter != null ? Dates.durationToString( expireAfter ) : "-" );
                    return false;
                } else {
                    log.info( "Delete old index {}", info );
                    collection.dropIndex( indexName );
                }
            }
            IndexOptions indexOptions = new IndexOptions().name( indexName ).unique( unique );
            if( expireAfter != null ) {
                indexOptions.expireAfter( expireAfter, TimeUnit.MILLISECONDS );
            }
            collection.createIndex( toBson( keys ), indexOptions );
            log.info( "Creating index {}, keys={}, unique={}, expireAfter={}...... Done",
                indexName, keys, unique,
                expireAfter != null ? Dates.durationToString( expireAfter ) : "-" );

            return true;
        } finally {
            if( refresh ) {
                refresh();
            }
        }
    }

    private Bson toBson( SequencedMap<String, IndexConfiguration.Direction> keys ) {
        return Indexes.compoundIndex(
            Lists.map( keys.entrySet(), e -> e.getValue() == IndexConfiguration.Direction.ASC ? Indexes.ascending( e.getKey() ) : Indexes.descending( e.getKey() ) )
        );
    }

    public IndexConfiguration getInfo( String indexName ) {
        return indexes.get( indexName );
    }

    @ToString
    @EqualsAndHashCode
    public static class IndexConfiguration {
        public final boolean unique;
        public final Long expireAfter;
        public final LinkedHashMap<String, Direction> keys = new LinkedHashMap<>();
        public String name;


        @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
        @JsonCreator
        public IndexConfiguration( SequencedMap<String, Integer> keys, boolean unique, Long expireAfter ) {
            keys.forEach( ( key, direction ) -> {
                this.keys.put( key, direction == Direction.ASC.mongo ? Direction.ASC : Direction.DESC );
            } );
            this.unique = unique;

            Preconditions.checkArgument( expireAfter == null || ( expireAfter >= 1000L && ( expireAfter / 1000L * 1000L ) == expireAfter ), "ttl only works with seconds" );

            this.expireAfter = expireAfter;
        }

        public IndexConfiguration( SequencedMap<String, Integer> keys, boolean unique ) {
            this( keys, unique, null );
        }

        public static Pair<String, IndexConfiguration> parse( Document document ) {
            String name = document.getString( "name" );
            boolean unique = document.getBoolean( "unique", false );
            Number expireAfterSeconds = ( Number ) document.get( "expireAfterSeconds" );
            Long expireAfter = expireAfterSeconds != null ? expireAfterSeconds.longValue() * 1000L : null;

            Document keyDocument = document.get( "key", Document.class );

            LinkedHashMap<String, Integer> keys = new LinkedHashMap<>();

            keyDocument.forEach( ( k, v ) ->
                keys.put( k, ( ( Number ) v ).intValue() ) );

            return __( name, new IndexConfiguration( keys, unique, expireAfter ) );
        }

        public boolean equals( SequencedMap<String, Direction> keys, boolean unique, Long expireAfter ) {
            if( unique != this.unique ) {
                return false;
            }

            if( this.keys.size() != keys.size() ) {
                return false;
            }

            if( !Objects.equals( this.expireAfter, expireAfter ) ) {
                return false;
            }

            Iterator<Map.Entry<String, Direction>> thisKeyIterator = this.keys.entrySet().iterator();
            Iterator<Map.Entry<String, Direction>> inKeyIterator = keys.entrySet().iterator();

            while( thisKeyIterator.hasNext() ) {
                Map.Entry<String, Direction> thisEntry = thisKeyIterator.next();
                Map.Entry<String, Direction> inEntry = inKeyIterator.next();

                if( !thisEntry.getKey().equals( inEntry.getKey() ) || thisEntry.getValue() != inEntry.getValue() ) {
                    return false;
                }
            }

            return true;
        }

        public enum Direction {
            ASC( 1 ), DESC( -1 );

            public final int mongo;

            Direction( int mongo ) {
                this.mongo = mongo;
            }
        }
    }
}
