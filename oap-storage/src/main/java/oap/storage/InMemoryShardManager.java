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

import oap.util.Try;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by igor.petrenko on 22.09.2016.
 */
public class InMemoryShardManager<T, ShardID> implements ShardManager<T, ShardID> {
   private final ConcurrentHashMap<ShardID, Storage<T>> storages = new ConcurrentHashMap<>();

   @Override
   public Storage<T> getOrCreate( ShardID shard, Function<ShardID, Storage<T>> cons ) {
      return storages.computeIfAbsent( shard, cons::apply );
   }

   @Override
   public Set<ShardID> shards() {
      return storages.keySet();
   }

   @Override
   public Collection<Storage<T>> select() {
      return storages.values();
   }

   @Override
   public boolean contains( ShardID shard ) {
      return storages.containsKey( shard );
   }

   @Override
   public void close() {
      storages.values().forEach( Try.consume( Closeable::close ) );
   }
}
