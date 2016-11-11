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

package oap.http.file;

import lombok.extern.slf4j.Slf4j;
import oap.http.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by igor.petrenko on 12.09.2016.
 */
@Slf4j
public class FileDownloader implements Runnable {
   private long lastAccessTime = 0;
   private List<FileDownloaderListener> listeners = new ArrayList<>();
   private String url;

   public FileDownloader( String url ) {
      this.url = url;
   }

   protected void fireDownloaded( Path path ) {
      for( FileDownloaderListener fdl : this.listeners ) fdl.downloaded( path );
   }

   protected void fireNotModified() {
      this.listeners.forEach( FileDownloaderListener::notModified );
   }

   public void addListener( FileDownloaderListener listener ) {
      this.listeners.add( listener );
   }

   public void removeListener( FileDownloaderListener listener ) {
      this.listeners.remove( listener );
   }

   @Override
   public synchronized void run() {
      final Optional<Path> downloaded = Client.DEFAULT.download( url, Optional.of( lastAccessTime ), Optional.empty(), ( i ) -> {} );

      if( downloaded.isPresent() ) {
         final Path path = downloaded.get();
         try {
            final FileTime lastModifiedTime = Files.getLastModifiedTime( path );
            lastAccessTime = lastModifiedTime.toMillis();
         } catch( IOException e ) {
            log.warn( e.getMessage() );
         }
         fireDownloaded( path );
      } else
         fireNotModified();
   }

   public interface FileDownloaderListener {
      void downloaded( Path path );

      default void notModified() {
      }
   }
}
