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
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

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

    private void refresh() {
        ArrayList<Document> indexes = new ArrayList<Document>();
        collection.listIndexes().into( indexes );

        log.debug( "refreshing indexes: {} ...", indexes );

        this.indexes.clear();

        for( Document indexDoc : indexes ) {
            IndexConfiguration info = new IndexConfiguration( indexDoc );
            this.indexes.put( info.name, info );
        }

        log.debug( "new indexes: {}", this.indexes );
    }

    public void update( IndexConfiguration... indexConfigurations ) {
        try {
            Set<String> expected = Stream.of( indexConfigurations )
                .map( ic -> ic.name )
                .collect( toSet() );

            for( String name : new ArrayList<>( indexes.keySet() ) ) {
                if( !"_id_".equals( name ) && !expected.contains( name ) ) {
                    collection.dropIndex( name );
                    this.indexes.remove( name );
                }
            }

            for( IndexConfiguration ic : indexConfigurations ) {
                update( ic.name, new ArrayList<>( ic.keys.keySet() ), ic.unique, ic.expireAfter, false );
            }
        } finally {
            refresh();
        }
    }

    public void update( String indexName, List<String> keys, boolean unique, Long expireAfter ) {
        update( indexName, keys, unique, expireAfter, true );
    }

    private void update( String indexName, List<String> keys, boolean unique, Long expireAfter, boolean refresh ) {
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
                    return;
                } else {
                    log.info( "Delete old index {}", info );
                    collection.dropIndex( indexName );
                }
            }
            IndexOptions indexOptions = new IndexOptions().name( indexName ).unique( unique );
            if( expireAfter != null ) indexOptions.expireAfter( expireAfter, TimeUnit.MILLISECONDS );
            collection.createIndex( Indexes.ascending( keys ), indexOptions );
            log.info( "Creating index {}, keys={}, unique={}, expireAfter={}...... Done",
                indexName, keys, unique,
                expireAfter != null ? Dates.durationToString( expireAfter ) : "-" );
        } finally {
            if( refresh ) {
                refresh();
            }
        }
    }

    public IndexConfiguration getInfo( String indexName ) {
        return indexes.get( indexName );
    }

    @ToString
    @EqualsAndHashCode
    public static class IndexConfiguration {
        public final String name;
        public final boolean unique;
        public final Long expireAfter;
        public final LinkedHashMap<String, Direction> keys = new LinkedHashMap<>();


        @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
        @JsonCreator
        public IndexConfiguration( String name, Map<String, Integer> keys, boolean unique, Long expireAfter ) {
            this.name = name;
            keys.forEach( ( key, direction ) -> {
                this.keys.put( key, direction == 1 ? Direction.ASC : Direction.DESC );
            } );
            this.unique = unique;

            Preconditions.checkArgument( expireAfter == null || ( expireAfter >= 1000L && ( expireAfter / 1000L * 1000L ) == expireAfter ), "ttl only works with seconds" );

            this.expireAfter = expireAfter;
        }

        public IndexConfiguration( String name, Map<String, Integer> keys, boolean unique ) {
            this( name, keys, unique, null );
        }

        public IndexConfiguration( Document document ) {
            name = document.getString( "name" );
            unique = document.getBoolean( "unique", false );
            Long expireAfterSeconds = document.getLong( "expireAfterSeconds" );
            expireAfter = expireAfterSeconds != null ? expireAfterSeconds * 1000L : null;

            Document keyDocument = document.get( "key", Document.class );
            keyDocument.forEach( ( k, v ) ->
                keys.put( k, ( ( Number ) v ).intValue() == 1 ? Direction.ASC : Direction.DESC ) );
        }

        public boolean equals( List<String> keys, boolean unique, Long expireAfter ) {
            if( unique != this.unique ) {
                return false;
            }

            if( this.keys.size() != keys.size() ) {
                return false;
            }

            if( !Objects.equals( this.expireAfter, expireAfter ) ) {
                return false;
            }

            for( String key : keys ) {
                Direction d = this.keys.get( key );
                if( d != Direction.ASC ) {
                    return false;
                }
            }

            return true;
        }

        public enum Direction {
            ASC, DESC
        }
    }
}
