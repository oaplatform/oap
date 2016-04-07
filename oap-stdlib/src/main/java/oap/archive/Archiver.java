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
package oap.archive;

import oap.io.IoStreams;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

public class Archiver {
   public void unpack( Path archive, Path dest, ArchiveType type ) {
      switch( type ) {
         case TAR_GZ:
            try( TarArchiveInputStream tar = new TarArchiveInputStream( IoStreams.in( archive, GZIP ) ) ) {
               ArchiveEntry entry;
               while( ( entry = tar.getNextEntry() ) != null ) {
                  Path path = dest.resolve( entry.getName() );
                  if( entry.isDirectory() )
                     path.toFile().mkdirs();
                  else IoStreams.write( path, PLAIN, tar );
               }
               tar.close();
            } catch( IOException e ) {
               throw new UncheckedIOException( e );
            }
      }
   }

   public enum ArchiveType {
      TAR_GZ
   }
}
