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

import oap.io.Files;
import oap.io.Resources;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

public class FileStorageTest extends AbstractTest {
    @Test
    public void load() {
        FileStorage<Bean> storage =
            new FileStorage<>( Resources.filePath( FileStorageTest.class, "data" ).get(), b -> b.id );
        storage.start();
        assertEqualsNoOrder( storage.select().toArray(),
            new Bean[]{ new Bean( "t2" ), new Bean( "t1" ) } );
    }

    @Test
    public void persist() throws InterruptedException {
        Path dataLocation = Files.path( Env.tmp( "data" ) );
        FileStorage<Bean> storage1 = new FileStorage<>( dataLocation, b -> b.id );
        storage1.fsync = 50;
        storage1.start();
        storage1.store( new Bean( "1" ) );
        storage1.store( new Bean( "2" ) );
        Thread.sleep( 100 );
        storage1.stop();
        FileStorage<Bean> storage2 = new FileStorage<>( dataLocation, b -> b.id );
        storage2.start();
        assertEqualsNoOrder( storage2.select().toArray(),
            new Bean[]{ new Bean( "2" ), new Bean( "1" ) } );
        storage2.stop();
    }

    @Test
    public void storeAndUpdate() {
        Path dataLocation = Files.path( Env.tmp( "data" ) );
        FileStorage<Bean> storage = new FileStorage<>( dataLocation, b -> b.id );
        storage.start();
        storage.store( new Bean( "111" ) );
        storage.get( "111" ).ifPresent( b -> {
            b.s = "bbb";
            storage.store( b );
        } );
        assertEquals( storage.get( "111" ).get().s, "bbb" );
        storage.stop();
    }
}

