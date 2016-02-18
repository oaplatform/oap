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
import oap.testng.AbstractTest;
import oap.testng.Asserts;
import oap.testng.Env;
import oap.util.Stream;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertTrue;

public class FileStorageTest extends AbstractTest {
    @Test
    public void load() {
        try( FileStorage<Bean> storage =
                 new FileStorage<>( Resources.filePath( FileStorageTest.class, "data" ).get(), b -> b.id ) ) {
            Asserts.assertEquals( storage.select(), Stream.of( new Bean( "t1" ), new Bean( "t2" ) ) );
        }
    }

    @Test
    public void persist() {
        try( FileStorage<Bean> storage1 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id, 50 ) ) {
            storage1.store( new Bean( "1" ) );
            storage1.store( new Bean( "2" ) );
            Threads.sleepSafely( 100 );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id ) ) {
            Asserts.assertEquals( storage2.select(), Stream.of( new Bean( "1" ), new Bean( "2" ) ) );
        }
    }

    @Test
    public void storeAndUpdate() {
        try( FileStorage<Bean> storage = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id, 50 ) ) {
            storage.store( new Bean( "111" ) );
            storage.update( "111", u -> u.s = "bbb" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id ) ) {
            Asserts.assertEquals( storage2.select(), Stream.of( new Bean( "111", "bbb" ) ) );
        }
    }

    @Test
    public void delete() {
        try( FileStorage<Bean> storage = new FileStorage<>( Env.tmpPath( "data" ), b -> b.id, 50 ) ) {
            storage.store( new Bean( "111" ) );
            storage.delete( "111" );
            assertTrue( storage.select().toList().isEmpty() );
        }
    }

    @Test
    public void masterSlave() {
        try( FileStorage<Bean> master = new FileStorage<>( Env.tmpPath( "master" ), b -> b.id, 50 );
             FileStorage<Bean> slave = new FileStorage<>( Env.tmpPath( "slave" ), b -> b.id, 50, master, 50 ) ) {
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
            Threads.sleepSafely( 100 );
            Asserts.assertEquals( slave.select(), Stream.of( new Bean( "111" ), new Bean( "222" ) ) );
            Assert.assertEquals( updates.get(), 2 );
            master.store( new Bean( "111", "bbb" ) );
            Threads.sleepSafely( 1000 );
            Asserts.assertEquals( slave.select(), Stream.of( new Bean( "111", "bbb" ), new Bean( "222" ) ) );
            Assert.assertEquals( updates.get(), 1 );
        }
    }
}

