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

package oap.io;

import oap.testng.AbstractPerformance;
import oap.testng.Env;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Created by Igor Petrenko on 08.04.2016.
 */
@Test( enabled = false )
public class FilesPerformance extends AbstractPerformance {

   public static final int SAMPLES = 100;

   @BeforeMethod
   @Override
   public void beforeMethod() {
      super.beforeMethod();

      IntStream
         .range( 0, 2000 )
         .forEach( i -> {
            final Path path = Env.tmpPath( "tt/test" + i );
            Files.writeString( path, RandomStringUtils.random( 10 ) );
         } );
   }

   @Test
   public void testLastModificationTime1() {
      final long[] size = { 0 };
      final Path tt = Env.tmpPath( "tt" );

      benchmark( "java.nio.file.Files.getLastModifiedTime()", SAMPLES, 5, ( x ) -> {
         try( DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream( tt ) ) {
            for( Path p : stream ) {
               size[0] += java.nio.file.Files.getLastModifiedTime( p ).to( TimeUnit.NANOSECONDS );
            }
         }
      } );
      System.out.println( size[0] );
   }

   @Test
   public void testLastModificationTime2() {
      final long[] size = { 0 };

      final File tt = Env.tmpPath( "tt" ).toFile();
      benchmark( "java.io.File.lastModified()", SAMPLES, 5, ( x ) -> {

         for( File file : tt.listFiles() ) {
            size[0] += file.lastModified();
         }
      } );
      System.out.println( size[0] );
   }
}
