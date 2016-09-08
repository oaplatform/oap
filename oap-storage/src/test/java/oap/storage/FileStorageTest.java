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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.deployTestData;
import static oap.testng.Env.tmpPath;
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
      try( FileStorage<Bean> storage = new FileStorage<>( deployTestData( getClass() ), b -> b.id ) ) {
         assertThat( storage.select() )
            .containsExactly( new Bean( "t1" ), new Bean( "t2" ) );
      }
   }

   @Test
   public void persist() {
      try( FileStorage<Bean> storage1 = new FileStorage<>( tmpPath( "data" ), b -> b.id, 50 ) ) {
         storage1.store( new Bean( "1" ) );
         storage1.store( new Bean( "2" ) );
      }

      try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ), b -> b.id ) ) {
         assertThat( storage2.select() )
            .containsExactly( new Bean( "1" ), new Bean( "2" ) );
      }
   }

   @Test
   public void persistFsLayout() {
      Path data = tmpPath( "data" );
      BiFunction<Path, Bean, Path> fsResolve = ( p, o ) -> p.resolve( o.s );
      try( FileStorage<Bean> storage1 = new FileStorage<>( data, fsResolve, b -> b.id, 50 ) ) {
         storage1.store( new Bean( "1" ) );
         storage1.store( new Bean( "2" ) );
      }

      assertFile( data.resolve( "aaa/1.json" ) ).exists();
      assertFile( data.resolve( "aaa/2.json" ) ).exists();

      try( FileStorage<Bean> storage2 = new FileStorage<>( data, fsResolve, b -> b.id, 50 ) ) {
         assertThat( storage2.select() )
            .containsExactly( new Bean( "1" ), new Bean( "2" ) );
      }

   }

   @Test
   public void storeAndUpdate() {
      try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ), b -> b.id, 50 ) ) {
         storage.store( new Bean( "111" ) );
         storage.update( "111", u -> u.s = "bbb" );
      }

      try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ), b -> b.id ) ) {
         assertThat( storage2.select() )
            .containsExactly( new Bean( "111", "bbb" ) );
      }
   }

   @Test
   public void delete() {
      Path data = tmpPath( "data" );
      try( FileStorage<Bean> storage = new FileStorage<>( data, b -> b.id, 50 ) ) {
         storage.store( new Bean( "111" ) );
         assertEventually( 100, 10, () -> assertThat( data.resolve( "111.json" ) ).exists() );
         storage.delete( "111" );
         assertThat( storage.select() ).isEmpty();
         assertThat( data.resolve( "111.json" ) ).doesNotExist();
      }
   }

   @Test
   public void deleteVersion() {
      Path data = tmpPath( "data" );
      try( FileStorage<Bean> storage = new FileStorage<>( data, b -> b.id, 50, 1, emptyList() ) ) {
         storage.store( new Bean( "111" ) );
         assertEventually( 100, 10, () -> assertThat( data.resolve( "111.v1.json" ) ).exists() );
         storage.delete( "111" );
         assertThat( storage.select() ).isEmpty();
         assertThat( data.resolve( "111.v1.json" ) ).doesNotExist();
      }
   }

}
