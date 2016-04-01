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

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class LimitedTimeExecutorTest {
   @Test
   public void execute() {
      assertExecution( 1, 0, 0, () -> Threads.sleepSafely( 10 ) );
      assertExecution( 0, 1, 0, () -> Threads.sleepSafely( 200 ) );
      assertExecution( 0, 0, 1, () -> {
         throw new RuntimeException( "expected" );
      } );
   }

   private void assertExecution( int expectedSuccess, int expectedTimeout, int expectedError, Runnable code ) {
      AtomicInteger success = new AtomicInteger();
      AtomicInteger timeout = new AtomicInteger();
      AtomicInteger error = new AtomicInteger();
      LimitedTimeExecutor executor = new LimitedTimeExecutor( 100, TimeUnit.MILLISECONDS )
         .onSuccess( success::incrementAndGet )
         .onError( e -> error.incrementAndGet() )
         .onTimeout( timeout::incrementAndGet );
      try {
         executor.execute( code );
      } catch( RuntimeException e ) {
         assertThat( e.getMessage() ).isEqualTo( "expected" );
      }
      assertThat( success.get() ).isEqualTo( expectedSuccess );
      assertThat( timeout.get() ).isEqualTo( expectedTimeout );
      assertThat( error.get() ).isEqualTo( expectedError );
   }
}
