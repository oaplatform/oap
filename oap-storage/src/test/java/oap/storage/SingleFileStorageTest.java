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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 23.09.2016.
 */
public class SingleFileStorageTest extends AbstractTest {
   @BeforeMethod
   @Override
   public void beforeMethod() throws Exception {
      super.beforeMethod();

      TypeIdFactory.register( TestSFS.class, TestSFS.class.getName() );
   }

   @Test
   public void testFsync() {
      final Path path = Env.tmpPath( "file.json" );
      try( final SingleFileStorage<TestSFS> sfs = new SingleFileStorage<>( path, s -> s.id, 100 ) ) {
         sfs.store( new TestSFS( "123" ) );

         assertEventually( 10, 200, () -> assertThat( path ).exists() );
      }
   }

   @Test
   public void testPersist() {
      final Path path = Env.tmpPath( "file.json" );
      try( final SingleFileStorage<TestSFS> sfs = new SingleFileStorage<>( path, s -> s.id, 100 ) ) {
         sfs.store( new TestSFS( "123" ) );
      }

      try( final SingleFileStorage<TestSFS> sfs2 = new SingleFileStorage<>( path, s -> s.id, 100 ) ) {
         assertThat( sfs2.get( "123" ) ).isPresent();
      }
   }


   public static class TestSFS {
      public String id;

      public TestSFS() {
      }

      public TestSFS( String id ) {
         this.id = id;
      }
   }
}