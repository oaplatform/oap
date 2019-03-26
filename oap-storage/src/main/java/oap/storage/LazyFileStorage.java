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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Stream;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class LazyFileStorage<T> extends MemoryStorage<T> {
    private Path path;
    private boolean closed = true;

    public LazyFileStorage( Path path, Identifier<T> identifier, Lock lock ) {
        super( identifier, lock );
        this.path = path;
    }

    @Override
    public Stream<T> select() {
        open();
        return super.select();
    }

    @Override
    public T store( T object ) {
        open();
        return super.store( object );
    }

    @Override
    public Optional<T> update( String id, T object ) {
        open();
        return super.update( id, object );
    }

    @Override
    public Optional<T> update( String id, Predicate<T> predicate,
                               Function<T, T> update,
                               Supplier<T> init ) {
        open();
        return super.update( id, predicate, update, init );
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
        return super.delete( id );
    }

    private synchronized void open() {
        if( data.size() > 0 ) {
            return;
        }
        Files.ensureFile( path );

        if( java.nio.file.Files.exists( path ) ) {
            Binder.json.unmarshal( new TypeRef<List<Metadata<T>>>() {}, path )
                .forEach( m -> {
                    var id = identifier.get( m.object );
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

        if( size() > 0 ) {
            try( OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), IoStreams.DEFAULT_BUFFER, false, true ) ) {
                Binder.json.marshal( out, data.values() );
            }
            log.debug( "storing {}... done", path );
        } else {
            java.nio.file.Files.deleteIfExists( path );
            log.debug( "removing {}... done", path );
        }
    }

}
