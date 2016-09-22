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

import oap.util.Stream;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 22.09.2016.
 */
public class ShardStorage<T, ShardID> {
   private ShardManager<T, ShardID> sm;
   private Function<ShardID, Storage<T>> cons;

   public ShardStorage( ShardManager<T, ShardID> sm, Function<ShardID, Storage<T>> cons ) {
      this.sm = sm;
      this.cons = cons;
   }

   public Stream<T> select( ShardID shard ) {
      return getStorage( shard ).select();
   }

   public Storage<T> getStorage( ShardID shard ) {
      return sm.getOrCreate( shard, cons );
   }

   public void store( ShardID shard, T object ) {
      getStorage( shard ).store( object );
   }

   public void store( ShardID shard, Collection<T> objects ) {
      getStorage( shard ).store( objects );
   }

   public Optional<T> update( ShardID shard, String id, Consumer<T> update ) {
      return getStorage( shard ).update( id, update );
   }

   public Optional<T> update( ShardID shard, String id, Consumer<T> update, Supplier<T> init ) {
      return getStorage( shard ).update( id, update, init );
   }

   public void update( ShardID shard, Collection<String> ids, Consumer<T> update ) {
      getStorage( shard ).update( ids, update );
   }

   public void update( ShardID shard, Collection<String> ids, Consumer<T> update, Supplier<T> init ) {
      getStorage( shard ).update( ids, update, init );
   }

   public Optional<T> get( ShardID shard, String id ) {
      return getStorage( shard ).get( id );
   }

   public void delete( ShardID shard, String id ) {
      getStorage( shard ).delete( id );
   }

   public void deleteAll( ShardID shard ) {
      getStorage( shard ).deleteAll();
   }

   public long size( ShardID shard ) {
      return getStorage( shard ).size();
   }

   public Set<ShardID> shards() {
      return sm.shards();
   }
}
