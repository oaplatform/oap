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

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.util.Optionals;
import oap.util.Stream;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class Replicator<T> implements Closeable {
   private final MemoryStorage<T> slave;
   private final ReplicationMaster<T> master;
   private final Scheduled scheduled;
   public long safeModificationTime = 1000;
   private final AtomicLong lastSync = new AtomicLong( 0 );

   public Replicator( MemoryStorage<T> slave, ReplicationMaster<T> master, long interval ) {
      this.slave = slave;
      this.master = master;
      this.scheduled = Scheduler.scheduleWithFixedDelay( interval, MILLISECONDS, this::replicate );
   }

   public synchronized void replicate() {
      long current = DateTimeUtils.currentTimeMillis() - safeModificationTime;
      long last = lastSync.get();
      List<Metadata<T>> updates = master.updatedSince( last );
      log.trace( "replicate {} to {} current: {}, last: {}, to sync {}", master, slave, current, last, updates.size() );
      for( Metadata<T> metadata : updates ) {
         log.trace( "replicate {}", metadata );
         slave.data.put( metadata.id, metadata );
      }
      slave.fireUpdated( Stream.of( updates ).map( m -> m.object ).toList() );
      List<String> ids = master.ids();
      log.trace( "master ids: {}", ids );
      List<T> deletedObjects = Stream.of( slave.data.keySet() )
         .filter( id -> !ids.contains( id ) )
         .flatMap( id -> Optionals.toStream( slave.deleteObject( id ).map( m -> m.object ) ) )
         .toList();
      log.trace( "deleted: {}", deletedObjects );
      slave.fireDeleted( deletedObjects );
      lastSync.set( current );
   }

   @Override
   public void close() {
      Scheduled.cancel( scheduled );
   }
}
