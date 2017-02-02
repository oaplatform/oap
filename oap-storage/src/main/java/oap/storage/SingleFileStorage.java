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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 23.09.2016.
 */
@Slf4j
public class SingleFileStorage<T> extends MemoryStorage<T> {
   private final PeriodicScheduled scheduled;
   private Path path;
   private AtomicBoolean modified = new AtomicBoolean( false );

   public SingleFileStorage( Path path, Function<T, String> identify, long fsync ) {
      super( identify );
      this.path = path;

      load();
      addDataListener( new SFSDataListener<>() );
      this.scheduled = Scheduler.scheduleWithFixedDelay( getClass(), fsync, this::fsync );
   }

   private void load() {
      Files.ensureFile( path );

      if( java.nio.file.Files.exists( path ) ) {
         data = Binder.json.unmarshal( new TypeReference<List<Metadata<T>>>() {
         }, path )
            .stream()
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
      }
      log.info( data.size() + " object(s) loaded." );
   }

   @SneakyThrows
   private synchronized void fsync( long last ) {
      log.trace( "fsync: last: {}, storage size: {}", last, data.size() );

      if( modified.getAndSet( false ) ) {
         final Path tmpPath = path.resolveSibling( path.getFileName() + ".tmp" );
         log.debug( "fsync storing {}...", tmpPath );

         OutputStream out = IoStreams.out( tmpPath, IoStreams.Encoding.from( path ) );
         out.write( "[".getBytes() );

         Iterator<Metadata<T>> it = data.values().iterator();
         while( it.hasNext() ) {
            Metadata<T> metadata = it.next();
            synchronized( metadata.id.intern() ) {
               out.write( Binder.json.marshal( metadata ).getBytes() );
            }
            if( it.hasNext() ) {
               out.write( ",".getBytes() );
            }
         }

         out.write( "]".getBytes() );
         out.close();
         Files.rename( tmpPath, path );
         log.debug( "fsync storing {}... done", path );
      }
   }

   @Override
   public synchronized void close() {
      Scheduled.cancel( scheduled );
      fsync( scheduled.lastExecuted() );
   }

   private class SFSDataListener<T> implements DataListener<T> {
      @Override
      public void updated( T object ) {
         modified.set( true );
      }

      @Override
      public void updated( Collection<T> objects ) {
         modified.set( true );
      }

      @Override
      public void deleted( T object ) {
         modified.set( true );
      }

      @Override
      public void deleted( Collection<T> objects ) {
         modified.set( true );
      }
   }
}
