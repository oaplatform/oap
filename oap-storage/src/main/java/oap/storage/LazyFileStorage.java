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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.util.Stream;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 23.09.2016.
 */
@Slf4j
public class LazyFileStorage<T> extends MemoryStorage<T> {
    private Path path;
    private boolean closed = true;

    public LazyFileStorage( Path path, Identifier<T> identifier, LockStrategy lockStrategy ) {
        super( identifier, lockStrategy );
        this.path = path;
    }

    @Override
    public List<Item<T>> updatedSince( long time ) {
        open();
        return super.updatedSince( time );
    }

    @Override
    public <TMetadata> Stream<T> select( Predicate<TMetadata> metadataFilter ) {
        open();
        return super.select( metadataFilter );
    }

    @Override
    public <TMetadata> T store( T object, Function<T, TMetadata> metadata ) {
        open();
        return super.store( object, metadata );
    }

    @Override
    public <TMetadata> Optional<T> update( String id, T object, Function<T, TMetadata> metadata ) {
        open();
        return super.update( id, object, metadata );
    }

    @Override
    public <TMetadata> Optional<T> update( String id, BiPredicate<T, TMetadata> predicate,
                                           BiFunction<T, TMetadata, T> update,
                                           Supplier<T> init,
                                           Function<T, TMetadata> initMetadata ) {
        open();
        return super.update( id, predicate, update, init, initMetadata );
    }

    @Override
    public void update( Collection<String> ids, Predicate<T> predicate, Function<T, T> update, Supplier<T> init ) {
        open();
        super.update( ids, predicate, update, init );
    }

    @Override
    public Optional<T> get( String id ) {
        open();
        return super.get( id );
    }

    @Override
    public void deleteAll() {
        open();
        super.deleteAll();
    }

    @Override
    public Optional<T> delete( String id ) {
        open();
        super.delete( id );
        return null;
    }

    @Override
    public <M> M updateMetadata( String id, Function<M, M> func ) {
        open();
        return super.updateMetadata( id, func );
    }

    @Override
    public <M> M getMetadata( String id ) {
        open();
        return super.getMetadata( id );
    }

    @Override
    public <M> Stream<M> selectMetadata() {
        open();
        return super.selectMetadata();
    }

    private synchronized void open() {
        if( data.size() > 0 ) {
            return;
        }
        Files.ensureFile( path );

        if( java.nio.file.Files.exists( path ) ) {
            Binder.json.unmarshal( new TypeReference<List<Item<T>>>() {}, path )
                .forEach( m -> {
                    val id = identifier.get( m.object );
                    data.put( id, m );
                } );
        }
        closed = false;
        log.info( data.size() + " object(s) loaded." );
    }

    @Override
    @SneakyThrows
    public synchronized void close() {
        if( closed ) return;
        fsync();
        data.clear();
        closed = true;
    }

    @Override
    @SneakyThrows
    public void fsync() {
        super.fsync();
        try( OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), IoStreams.DEFAULT_BUFFER, false, true ) ) {
            Binder.json.marshal( out, data.values() );
        }
        log.debug( "storing {}... done", path );
    }

}
