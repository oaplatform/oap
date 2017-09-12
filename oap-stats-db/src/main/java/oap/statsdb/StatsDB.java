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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.util.Throwables;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.GZIP;

/**
 * Created by igor.petrenko on 05.09.2017.
 */
@Slf4j
public abstract class StatsDB<T extends StatsDB.Database> extends Node implements Closeable {
    protected final Path directory;
    private final TypeReference<T> typeReference;

    public StatsDB( Path directory, TypeReference<T> typeReference ) {
        super( -1 );
        this.directory = directory;
        this.typeReference = typeReference;
    }

    protected abstract T toDatabase( ConcurrentHashMap<String, Node> db );

    public <TKey extends Iterable<String>, TValue extends Value<TValue>> void update( TKey key, Consumer<TValue> update, Supplier<TValue> create ) {
        Node node = this;
        for( val keyItem : key ) {
            node = node.db.computeIfAbsent( keyItem, ( k ) -> new Node( DateTimeUtils.currentTimeMillis() ) );
        }

        node.updateValue( update, create );
    }

    public synchronized void fsync() {
        val db = directory.resolve( "stats.db.gz" );

        try( OutputStream outputStream = IoStreams.out( db, GZIP, DEFAULT_BUFFER, false, true ) ) {
            Binder.json.marshal( outputStream, toDatabase( this.db ) );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            throw Throwables.propagate( e );
        }
    }

    public void start() {
        val db = directory.resolve( "stats.db.gz" );
        if( Files.exists( db ) ) {
            try( InputStream inputStream = IoStreams.in( db, GZIP ) ) {
                val database = Binder.json.unmarshal( typeReference, inputStream );
                start( database );
                if( database.db != null ) this.db.putAll( database.db );
            } catch( IOException e ) {
                log.error( e.getMessage(), e );
                throw Throwables.propagate( e );
            }
        }

    }

    protected void start( T database ) {

    }

    @Override
    public void close() {
        fsync();
    }

    public synchronized void removeAll() {
        db.clear();
        fsync();
    }

    public static class Database implements Serializable {
        private static final long serialVersionUID = 20816260507748956L;

        public ConcurrentHashMap<String, Node> db;

        public Database() {
        }

        public Database( ConcurrentHashMap<String, Node> db ) {
            this.db = db;
        }
    }
}
