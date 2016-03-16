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

import oap.concurrent.Threads;
import oap.io.Resources;
import oap.json.TypeIdFactory;
import oap.storage.Bean.BeanMigration;
import oap.storage.Bean2.Bean2Migration;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

public class FileStorageTest extends AbstractTest {
    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        TypeIdFactory.register( Bean.class, Bean.class.getName() );
        TypeIdFactory.register( Bean2.class, Bean2.class.getName() );
    }

    @Test
    public void load() {
        try( FileStorage<Bean> storage =
                 new FileStorage<>( Resources.filePath( FileStorageTest.class, "data" ).get(), b -> b.id ) ) {
            storage.start();
            assertThat( storage.select() ).containsExactly( new Bean( "t1" ), new Bean( "t2" ) );
        }
    }

    @Test
    public void persist() {
        try( FileStorage<Bean> storage1 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id, 50 ) ) {
            storage1.start();
            storage1.store( new Bean( "1" ) );
            storage1.store( new Bean( "2" ) );
            Threads.sleepSafely( 100 );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id ) ) {
            storage2.start();
            assertThat( storage2.select() ).containsExactly( new Bean( "1" ), new Bean( "2" ) );
        }
    }

    @Test
    public void storeAndUpdate() {
        try( FileStorage<Bean> storage = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id, 50 ) ) {
            storage.start();
            storage.store( new Bean( "111" ) );
            storage.update( "111", u -> u.s = "bbb" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id ) ) {
            storage2.start();
            assertThat( storage2.select() ).containsExactly( new Bean( "111", "bbb" ) );
        }
    }

    @Test
    public void delete() {
        Path data = Env.tmpPath( "data" );
        try( FileStorage<Bean> storage = new FileStorage<>( data, b -> b.id, 50 ) ) {
            storage.start();
            storage.store( new Bean( "111" ) );
            assertEventually( 10, 100, () -> assertThat( data.resolve( "111.json" ).toFile() ).exists() );
            storage.delete( "111" );
            assertThat( storage.select() ).isEmpty();
            assertThat( data.resolve( "111.json" ).toFile() ).exists();
            storage.vacuum();
            assertThat( data.resolve( "111.json" ).toFile() ).doesNotExist();
        }
    }

    @Test
    public void delete_version() {
        Path data = Env.tmpPath( "data" );
        try( FileStorage<Bean> storage = new FileStorage<>( data, b -> b.id, 50, 1, emptyList() ) ) {
            storage.start();
            storage.store( new Bean( "111" ) );
            assertEventually( 10, 100, () -> assertThat( data.resolve( "111.v1.json" ).toFile() ).exists() );
            storage.delete( "111" );
            assertThat( storage.select() ).isEmpty();
            assertThat( data.resolve( "111.v1.json" ).toFile() ).exists();
            storage.vacuum();
            assertThat( data.resolve( "111.v1.json" ).toFile() ).doesNotExist();
        }
    }

    @Test
    public void masterSlave() {
        try( FileStorage<Bean> master = new FileStorage<>( Env.tmpPath( "master" ), b -> b.id, 50 );
             FileStorage<Bean> slave = new FileStorage<>( Env.tmpPath( "slave" ), b -> b.id, 50, master, 50 ) ) {
            master.start();
            slave.start();
            AtomicInteger updates = new AtomicInteger();
            slave.addDataListener( new FileStorage.DataListener<Bean>() {
                public void updated( Collection<Bean> objects ) {
                    updates.set( objects.size() );
                }
            } );
            slave.rsyncSafeInterval = 0;
            Threads.sleepSafely( 100 );
            master.store( new Bean( "111" ) );
            master.store( new Bean( "222" ) );
            assertEventually( 120, 5, () -> {
                assertThat( slave.select() ).containsExactly( new Bean( "111" ), new Bean( "222" ) );
                assertThat( updates.get() ).isEqualTo( 2 );
            } );
            master.store( new Bean( "111", "bbb" ) );
            assertEventually( 120, 5, () -> {
                assertThat( slave.select() ).containsExactly( new Bean( "111", "bbb" ), new Bean( "222" ) );
                assertThat( updates.get() ).isEqualTo( 1 );
            } );
        }
    }

    @Test
    public void testMigration() {
        final Path data = Env.tmpPath( "data" );
        try( FileStorage<Bean> storage1 = new FileStorage<>( data, b -> b.id, -1 ) ) {
            storage1.start();
            storage1.store( new Bean( "1" ) );
            storage1.store( new Bean( "2" ) );
        }

        assertThat( data.resolve( "1.json" ).toFile() ).exists();
        assertThat( data.resolve( "2.json" ).toFile() ).exists();

        try( FileStorage<Bean2> storage2 = new FileStorage<>( data, b -> b.id2, -1, 2, Arrays.asList(
            BeanMigration.class.getName(),
            Bean2Migration.class.getName()
        ) ) ) {
            storage2.start();
            assertThat( storage2.select() ).containsExactly( new Bean2( "11" ), new Bean2( "21" ) );
        }

        assertThat( data.resolve( "1.json" ).toFile() ).doesNotExist();
        assertThat( data.resolve( "2.json" ).toFile() ).doesNotExist();

        assertThat( data.resolve( "1.v1.json" ).toFile() ).doesNotExist();
        assertThat( data.resolve( "2.v1.json" ).toFile() ).doesNotExist();

        assertThat( data.resolve( "1.v2.json" ).toFile() ).exists();
        assertThat( data.resolve( "2.v2.json" ).toFile() ).exists();
    }
}

