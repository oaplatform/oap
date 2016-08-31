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
package oap.concurrent;

import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class Timed implements Closeable {
   private final AtomicLong lastTimeExecuted = new AtomicLong( 0 );
   private final long period;
   private Scheduled scheduled;
   private TimeUnit timeUnit;

   public Timed( long period, TimeUnit timeUnit ) {
      this.period = period;
      this.timeUnit = timeUnit;
   }

   public void start() {
      this.scheduled = Scheduler.scheduleWithFixedDelay( period, timeUnit, this::doit );

   }

   public void stop() {
      Scheduled.cancel( scheduled );
   }

   private void doit() {
      long current = DateTimeUtils.currentTimeMillis();
      run( lastTimeExecuted.get() );
      lastTimeExecuted.set( current );
   }

   abstract void run( long lastTimeExecuted );

   @Override
   public void close() {
      stop();
   }

   public long lastExecuted() {
      return lastTimeExecuted.get();
   }

   public static Timed create( long period, Consumer<Long> consume ) {
      return create( period, MILLISECONDS, consume );
   }

   public static Timed create( long period, final TimeUnit timeUnit, Consumer<Long> consume ) {
      return new Timed( period, timeUnit ) {
         @Override
         void run( long lastTimeExecuted ) {
            consume.accept( lastTimeExecuted );
         }
      };
   }
}
