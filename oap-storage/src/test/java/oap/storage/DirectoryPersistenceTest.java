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

import lombok.extern.slf4j.Slf4j;
import oap.json.TypeIdFactory;
import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.function.BiFunction;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Env.deployTestData;
import static oap.testng.Env.tmpPath;
import static oap.util.Lists.empty;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class DirectoryPersistenceTest extends AbstractTest {
    static {
        TypeIdFactory.register( Bean.class, Bean.class.getName() );
    }

    @Test
    public void load() {
        Path path = deployTestData( getClass() );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            assertThat( storage.select() )
                .containsExactly( new Bean( "t1" ), new Bean( "t2" ) );
        }
    }

    @Test
    public void persist() {
        Path path = tmpPath( "data" );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            storage.store( new Bean( "1" ) );
            storage.store( new Bean( "2" ) );
        }

        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            assertThat( storage.select() )
                .containsExactly( new Bean( "1" ), new Bean( "2" ) );
        }
    }

    @Test
    public void persistFsLayout() {
        Path path = tmpPath( "data" );
        BiFunction<Path, Bean, Path> fsResolve = ( p, o ) -> p.resolve( o.s );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, fsResolve, 50, 0, empty(), storage ) ) {
            persistence.start();
            storage.store( new Bean( "1" ) );
            storage.store( new Bean( "2" ) );
        }

        assertThat( path.resolve( "aaa/1.json" ) ).exists();
        assertThat( path.resolve( "aaa/2.json" ) ).exists();

        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, fsResolve, 50, 0, empty(), storage ) ) {
            persistence.start();
            assertThat( storage.select() )
                .containsExactly( new Bean( "1" ), new Bean( "2" ) );
        }

    }

    @Test
    public void restructureFsLayout() {
        Path path = tmpPath( "data" );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            storage.store( new Bean( "1", "aaa" ) );
            storage.store( new Bean( "2", "bbb" ) );
        }

        assertThat( path.resolve( "aaa/1.json" ) ).doesNotExist();
        assertThat( path.resolve( "bbb/2.json" ) ).doesNotExist();

        assertThat( path.resolve( "1.json" ) ).exists();
        assertThat( path.resolve( "2.json" ) ).exists();

        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence =
                 new DirectoryPersistence<>( path, ( p, s ) -> p.resolve( s.s ), 50, 0, empty(), storage ) ) {
            persistence.start();

            assertThat( storage.select() ).containsExactly( new Bean( "1", "aaa" ), new Bean( "2", "bbb" ) );

            assertThat( path.resolve( "aaa/1.json" ) ).exists();
            assertThat( path.resolve( "bbb/2.json" ) ).exists();

            assertThat( path.resolve( "1.json" ) ).doesNotExist();
            assertThat( path.resolve( "2.json" ) ).doesNotExist();
        }
    }

    @Test
    public void storeAndUpdate() {
        Path path = tmpPath( "data" );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            storage.store( new Bean( "111" ) );
            storage.update( "111", u -> {
                u.s = "bbb";
                return u;
            } );
        }

        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, storage ) ) {
            persistence.start();
            assertThat( storage.select() )
                .containsExactly( new Bean( "111", "bbb" ) );
        }
    }

    @Test
    public void delete() {
        Path path = tmpPath( "data" );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, 50, 0, empty(), storage ) ) {
            persistence.start();
            storage.store( new Bean( "111" ) );
            assertEventually( 200, 10, () -> {
                log.debug( "going to assert existence of {}", path.resolve( "111.json" ) );
                assertThat( path.resolve( "111.json" ) ).exists();
            } );
            storage.delete( "111" );
            assertThat( storage.select() ).isEmpty();
            assertThat( path.resolve( "111.json" ) ).doesNotExist();
        }
    }

    @Test
    public void deleteVersion() {
        Path path = tmpPath( "data" );
        try( MemoryStorage<Bean> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             DirectoryPersistence<Bean> persistence = new DirectoryPersistence<>( path, 50, 1, empty(), storage ) ) {
            persistence.start();
            storage.store( new Bean( "111" ) );
            assertEventually( 100, 100, () -> assertThat( path.resolve( "111.v1.json" ) ).exists() );
            storage.delete( "111" );
            assertThat( storage.select() ).isEmpty();
            assertThat( path.resolve( "111.v1.json" ) ).doesNotExist();
        }
    }

}
