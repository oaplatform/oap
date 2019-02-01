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

import oap.json.TypeIdFactory;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.storage.Storage.Lock.SERIALIZED;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

public class FilePersistenceTest extends AbstractTest {
    static {
        TypeIdFactory.register( Bean.class, Bean.class.getName() );
    }

    @Test
    public void fsync() {
        Path path = Env.tmpPath( "storage.json.gz" );
        try( MemoryStorage<Bean> storge = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             FilePersistence<Bean> persistence = new FilePersistence<>( path, 10, storge ) ) {
            persistence.start();
            storge.store( new Bean( "123" ) );

            assertEventually( 10, 200, () -> assertThat( path ).exists() );
        }
    }

    @Test
    public void persist() {
        Path path = Env.tmpPath( "storage.json.gz" );
        try( MemoryStorage<Bean> storge = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             FilePersistence<Bean> persistence = new FilePersistence<>( path, 10, storge ) ) {
            persistence.start();
            storge.store( new Bean( "123" ) );
        }

        try( MemoryStorage<Bean> storge = new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED );
             FilePersistence<Bean> persistence = new FilePersistence<>( path, 10, storge ) ) {
            persistence.start();
            assertThat( storge.select() ).containsExactly( new Bean( "123" ) );
        }
    }


}
