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

import com.google.common.collect.Iterators;
import oap.util.Try;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FileWalkerCache {
   private final HashMap<Path, ArrayList<Path>> map = new HashMap<>();
   private final HashMap<Path, Boolean> isDirectory = new HashMap<>();
   private final HashMap<Path, Boolean> exists = new HashMap<>();
   private HashMap<Path, FileTime> lastModifiedTime = new HashMap<>();

   public DirectoryStream<Path> newDirectoryStream( Path dir,
                                                    DirectoryStream.Filter<? super Path> filter ) throws IOException {
      final ArrayList<Path> list = map.get( dir );
      if( list == null ) {
         final ArrayList<Path> paths = map.computeIfAbsent( dir, ( d ) -> new ArrayList<>() );

         return java.nio.file.Files.newDirectoryStream( dir, ( file ) -> {
            paths.add( file );
            exists.put( file, true );
            return filter.accept( file );
         } );
      }

      return new DirectoryStream<Path>() {
         @Override
         public Iterator<Path> iterator() {
            return Iterators.filter( list.iterator(), ( p ) -> {
               try {
                  return filter.accept( p );
               } catch( IOException e ) {
                  throw new DirectoryIteratorException( e );
               }
            } );
         }

         @Override
         public void close() throws IOException {

         }
      };
   }

   public boolean isDirectory( Path path ) {
      return isDirectory.computeIfAbsent( path, ( p ) -> java.nio.file.Files.isDirectory( p ) );
   }

   public boolean exists( Path path ) {
      return exists.computeIfAbsent( path, ( p ) -> java.nio.file.Files.exists( p ) );
   }

   public FileTime getLastModifiedTime( Path path ) {
      return lastModifiedTime.computeIfAbsent( path, Try.map( p -> java.nio.file.Files.getLastModifiedTime( p ) ) );
   }
}
