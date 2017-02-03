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
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.util.Stream;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static oap.util.Maps.Collectors.toConcurrentMap;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 23.09.2016.
 */
@Slf4j
public class LazyFileStorage<T> extends MemoryStorage<T> {
   private Path path;
   private boolean closed = true;

   public LazyFileStorage( Path path, Function<T, String> identify ) {
      super( identify );
      this.path = path;
   }

   @Override
   public Stream<T> select() {
      open();
      return super.select();
   }

   @Override
   public void store( T object ) {
      open();
      super.store( object );
   }

   @Override
   public Optional<T> update( String id, Consumer<T> update ) {
      open();
      return super.update( id, update );
   }

   @Override
   public Optional<T> update( String id, Consumer<T> update, Supplier<T> init ) {
      open();
      return super.update( id, update, init );
   }

   @Override
   public void update( Collection<String> ids, Consumer<T> update ) {
      open();
      super.update( ids, update );
   }

   @Override
   public void update( Collection<String> ids, Consumer<T> update, Supplier<T> init ) {
      open();
      super.update( ids, update, init );
   }

   @Override
   public Optional<T> get( String id ) {
      open();
      return super.get( id );
   }

   @Override
   public void deleteAll() {
      open();
      super.deleteAll();
   }

   @Override
   public void delete( String id ) {
      open();
      super.delete( id );
   }

   private void open() {
      if( data.size() > 0 ) {
         return;
      }
      Files.ensureFile( path );

      if( java.nio.file.Files.exists( path ) ) {
         data = Binder.json.unmarshal( new TypeReference<List<Metadata<T>>>() {
         }, path )
            .stream()
            .map( x -> __( x.id, x ) )
            .collect( toConcurrentMap() );
      }
      closed = false;
      log.info( data.size() + " object(s) loaded." );
   }

   @Override
   @SneakyThrows
   public synchronized void close() {
      if ( closed ) return;
      OutputStream out = IoStreams.out( path, IoStreams.Encoding.from( path ), IoStreams.DEFAULT_BUFFER, false, true );
      Binder.json.marshal( out, data.values() );
      out.close();
      log.debug( "storing {}... done", path );
      data.clear();
      closed = true;
   }

}
