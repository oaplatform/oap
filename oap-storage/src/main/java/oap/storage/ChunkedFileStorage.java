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
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.PeriodicScheduled;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Try;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;

@Slf4j
public class ChunkedFileStorage<T> extends MemoryStorage<T> implements Closeable {

   private final static String EXT = ".json.gz";

   private final Path path;
   private final int chunks;

   private final PeriodicScheduled scheduled;

   public ChunkedFileStorage( Path path, Function<T, String> identify, int chunks, long fsync ) {
      super( identify );
      this.path = path;
      this.chunks = chunks < 1 ? 1 : chunks;
      data = load( path );
      this.scheduled = Scheduler.scheduleWithFixedDelay( fsync, this::fsync );
   }

   public ChunkedFileStorage( Path path, Function<T, String> identify, int chunks ) {
      this( path, identify, chunks, 60000 );
   }

   Map<String, Metadata<T>> load( Path path ) {
      Files.ensureDirectory( path );
      List<Path> paths = Files.wildcard( path, "*" + EXT );
      log.debug( "found {} files", paths.size() );
      Map<String, Metadata<T>> data = paths
         .stream()
         .flatMap( Try.map(
            f -> Binder.json.unmarshal( new TypeReference<List<Metadata<T>>>() {
            }, f ).stream() ) )
         .map( x -> __( x.id, x ) )
         .collect( toConcurrentMap() );
      log.info( data.size() + " object(s) loaded." );

      return data;
   }

   private synchronized void fsync( long last ) {
      log.trace( "fsync: last: {}, storage size: {}", last, data.size() );
      Iterator<Metadata<T>> it = data.values().iterator();

      int currentChunk = 0;
      while( it.hasNext() ) {
         List<Metadata<T>> chunked = newArrayList();
         for( int i = 0; it.hasNext() && i < data.size() / chunks; ++i )
            chunked.add( it.next() );

         Binder.json.marshal( path.resolve( currentChunk + EXT ), chunked );
         ++currentChunk;
      }
   }

   @Override
   public synchronized void close() {
      Scheduled.cancel( scheduled );
      data.clear();
   }

}
