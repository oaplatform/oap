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

import oap.util.Maps;
import oap.util.Optionals;
import oap.util.Stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MemoryStorage<T> implements Storage<T> {
   protected ConcurrentMap<String, Metadata<T>> data = new ConcurrentHashMap<>();
   protected Function<T, String> identify;
   private List<DataListener<T>> dataListeners = new ArrayList<>();

   public MemoryStorage( Function<T, String> identify ) {
      this.identify = identify;
   }

   @Override
   public Stream<T> select() {
      return Stream.of( data.values() ).map( m -> m.object );
   }

   @Override
   public void store( T object ) {
      String id = this.identify.apply( object );
      synchronized( id.intern() ) {
         Metadata<T> metadata = data.get( id );
         if( metadata != null ) metadata.update( object );
         else data.put( id, new Metadata<>( id, object ) );
         fireUpdated( object );
      }
   }

   @Override
   public void store( Collection<T> objects ) {
      for( T object : objects ) {
         String id = this.identify.apply( object );
         synchronized( id.intern() ) {
            Metadata<T> metadata = data.get( id );
            if( metadata != null ) metadata.update( object );
            else data.put( id, new Metadata<>( id, object ) );
         }
      }
      fireUpdated( objects );
   }

   @Override
   public Optional<T> update( String id, Consumer<T> update ) {
      return update( id, update, null );
   }

   @Override
   public Optional<T> update( String id, Consumer<T> update, Supplier<T> init ) {
      return updateObject( id, update, init )
         .map( m -> {
            fireUpdated( m.object );
            return m.object;
         } );
   }

   protected Optional<Metadata<T>> updateObject( String id, Consumer<T> update, Supplier<T> init ) {
      synchronized( id.intern() ) {
         Metadata<T> m = data.get( id );
         if( m == null ) {
            if( init == null ) return Optional.empty();
            T object = init.get();
            m = new Metadata<>( identify.apply( object ), object );
            data.put( m.id, m );
         } else {
            update.accept( m.object );
            m.update( m.object );
         }
         return Optional.of( m );
      }
   }

   @Override
   public void update( Collection<String> ids, Consumer<T> update ) {
      update( ids, update, null );
   }

   @Override
   public void update( Collection<String> ids, Consumer<T> update, Supplier<T> init ) {
      fireUpdated( Stream.of( ids )
         .flatMap( id -> Optionals.toStream( updateObject( id, update, init )
            .map( m -> m.object ) ) )
         .toList() );
   }

   @Override
   public Optional<T> get( String id ) {
      return Maps.get( data, id ).map( m -> m.object );

   }

   @Override
   public void deleteAll() {
      List<T> objects = select().toList();
      data.clear();
      fireDeleted( objects );
   }

   public void delete( String id ) {
      deleteObject( id ).ifPresent( m -> fireDeleted( m.object ) );
   }

   protected Optional<Metadata<T>> deleteObject( String id ) {
      synchronized( id.intern() ) {
         Optional<Metadata<T>> metadata = Maps.get( data, id );
         if( metadata.isPresent() ) data.remove( id );
         return metadata;
      }
   }

   @Override
   public long size() {
      return data.size();
   }

   protected void fireUpdated( T object ) {
      for( DataListener<T> dataListener : this.dataListeners ) dataListener.updated( object );
   }

   protected void fireUpdated( Collection<T> objects ) {
      if( !objects.isEmpty() )
         for( DataListener<T> dataListener : this.dataListeners )
            dataListener.updated( objects );
   }

   protected void fireDeleted( T object ) {
      for( DataListener<T> dataListener : this.dataListeners ) dataListener.deleted( object );
   }

   protected void fireDeleted( List<T> objects ) {
      if( !objects.isEmpty() )
         for( DataListener<T> dataListener : this.dataListeners )
            dataListener.deleted( objects );
   }

   @Override
   public void addDataListener( DataListener<T> dataListener ) {
      this.dataListeners.add( dataListener );
   }

   @Override
   public void removeDataListener( DataListener<T> dataListener ) {
      this.dataListeners.remove( dataListener );
   }
}
