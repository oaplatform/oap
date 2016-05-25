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

import com.google.common.base.Throwables;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class LimitedTimeExecutor extends AsyncCallbacks<LimitedTimeExecutor> {
   private final ExecutorService executor;
   public final long timeout;
   public final TimeUnit unit;

   public LimitedTimeExecutor() {
      this( Long.MAX_VALUE, TimeUnit.MILLISECONDS );
   }

   public LimitedTimeExecutor( long timeout, TimeUnit unit ) {
      this( timeout, unit, Executors.newCachedThreadPool() );
   }

   public LimitedTimeExecutor( long timeout, TimeUnit unit, ExecutorService executor ) {
      this.timeout = timeout;
      this.unit = unit;
      this.executor = executor;
   }

   public <T> Optional<T> execute( Supplier<T> code ) {
      return execute( this.timeout, this.unit, code );
   }

   public <T> Optional<T> execute( long timeout, TimeUnit unit, Supplier<T> code ) {
      try {
         T value = executor.submit( code::get ).get( timeout, unit );
         onSuccess.run();
         return Optional.ofNullable( value );
      } catch( InterruptedException | TimeoutException e ) {
         onTimeout.run();
         return Optional.empty();
      } catch( ExecutionException e ) {
         onError.accept( e );
         throw Throwables.propagate( e.getCause() );
      }
   }

   public void execute( Runnable code ) {
      execute( this.timeout, this.unit, code );
   }

   public void execute( long timeout, TimeUnit unit, Runnable code ) {
      try {
         executor.submit( code ).get( timeout, unit );
         onSuccess.run();
      } catch( InterruptedException | TimeoutException e ) {
         onTimeout.run();
      } catch( ExecutionException e ) {
         onError.accept( e );
         throw Throwables.propagate( e.getCause() );
      }
   }


}
