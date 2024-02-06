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
import oap.id.Identifier;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Stream;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
public class LazyFileStorage<T> extends MemoryStorage<String, T> implements AutoCloseable {
    private final Path path;
    private boolean closed = true;

    public LazyFileStorage( Path path, Identifier<String, T> identifier, Lock lock ) {
        super( identifier, lock );
        this.path = path;
    }

    @Override
    public Stream<T> select() {
        open();
        return super.select();
    }

    @Override
    public T store( @Nonnull T object ) {
        open();
        return super.store( object );
    }

    @Override
    public Optional<T> get( @Nonnull String id ) {
        open();
        return super.get( id );
    }

    @Override
    public void deleteAll() {
        open();
        super.deleteAll();
    }

    @Override
    public Optional<T> delete( @Nonnull String id ) {
        open();
        return super.delete( id );
    }

    private synchronized void open() {
        if( size() > 0 ) return;
        Files.ensureFile( path );

        Binder.json.unmarshal( new TypeRef<List<Metadata<T>>>() {}, path )
            .orElse( List.of() )
            .forEach( m -> memory.put( identifier.get( m.object ), m ) );
        closed = false;
        log.info( size() + " object(s) loaded." );
    }

    @Override
    @SneakyThrows
    public synchronized void close() {
        if( closed ) return;
        fsync();
        closed = true;
    }

    @SneakyThrows
    public void fsync() {
        if( size() > 0 ) {
            try( OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), IoStreams.DEFAULT_BUFFER, false, true ) ) {
                Binder.json.marshal( out, memory.selectLive().toList() );
            }
            log.debug( "storing {}... done", path );
        } else {
            java.nio.file.Files.deleteIfExists( path );
            log.debug( "removing {}... done", path );
        }
    }

}
