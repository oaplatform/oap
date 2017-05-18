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

import oap.storage.migration.FileStorageMigration;
import oap.util.Lists;
import oap.util.Try;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static oap.util.Lists.empty;

/**
 * CAUTION: fsResolve should be using STABLE values ONLY. File relocation on the filesystem IS NOT SUPPORTED!
 *
 * @param <T>
 */
public class FileStorage<T> extends MemoryStorage<T> {
    private static final int VERSION = 0;
    private PersistenceBackend<T> persistence;

    /**
     * @deprecated use {@link #FileStorage(Path, Identifier, long, int, List)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, Function<T, String> identify, long fsync, int version, List<String> migrations ) {
        this( path, ( p, object ) -> p, IdentifierBuilder.identify( identify ).build(), fsync, version, migrations );
    }

    public FileStorage( Path path, Identifier<T> identifier, long fsync, int version, List<String> migrations ) {
        this( path, ( p, object ) -> p, identifier, fsync, version, migrations );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, BiFunction, Identifier, long, int, List)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Function<T, String> identify,
                        long fsync, int version, List<String> migrations ) {
        this( path, fsResolve, IdentifierBuilder.identify( identify ).build(), fsync, version, migrations );
    }

    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Identifier<T> identifier,
                        long fsync, int version, List<String> migrations ) {
        super( identifier );
        this.persistence = new FsPersistenceBackend<>( path, fsResolve, fsync, version, Lists.map( migrations,
            Try.map( clazz -> ( FileStorageMigration ) Class.forName( clazz ).newInstance() )
        ), this );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, BiFunction, Identifier, long)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Function<T, String> identify, long fsync ) {
        this( path, fsResolve, IdentifierBuilder.identify( identify ).build(), fsync, VERSION, empty() );
    }

    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Identifier<T> identifier, long fsync ) {
        this( path, fsResolve, identifier, fsync, VERSION, empty() );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, Identifier, long)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, Function<T, String> identify, long fsync ) {
        this( path, IdentifierBuilder.identify( identify ).build(), fsync, VERSION, empty() );
    }

    public FileStorage( Path path, Identifier<T> identifier, long fsync ) {
        this( path, identifier, fsync, VERSION, empty() );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, Identifier)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, Function<T, String> identify ) {
        this( path, IdentifierBuilder.identify( identify ).build(), VERSION, empty() );
    }

    public FileStorage( Path path, Identifier<T> identifier ) {
        this( path, identifier, VERSION, empty() );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, BiFunction, Identifier, int version, List)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Function<T, String> identify, int version, List<String> migrations ) {
        this( path, fsResolve, IdentifierBuilder.identify( identify ).build(), 60000, version, migrations );
    }

    public FileStorage( Path path, BiFunction<Path, T, Path> fsResolve, Identifier<T> identifier, int version, List<String> migrations ) {
        this( path, fsResolve, identifier, 60000, version, migrations );
    }

    /**
     * @deprecated use {@link #FileStorage(Path, Identifier, int, List)}} instead.
     */
    @Deprecated
    public FileStorage( Path path, Function<T, String> identify, int version, List<String> migrations ) {
        this( path, IdentifierBuilder.identify( identify ).build(), 60000, version, migrations );
    }

    public FileStorage( Path path, Identifier<T> identifier, int version, List<String> migrations ) {
        this( path, identifier, 60000, version, migrations );
    }

    @Override
    public synchronized void close() {
        persistence.close();
        data.clear();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + persistence;
    }
}
