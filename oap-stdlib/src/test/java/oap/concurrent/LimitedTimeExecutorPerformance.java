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

import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Test(enabled = false)
public class LimitedTimeExecutorPerformance extends AbstractPerformance {
   @Test
   public void testPerf() {

      final int SAMPLES = 100000;
      final int EXPERIMENTS = 5;
      final int THREADS = 5000;

      benchmark( "without-LimitedTime", SAMPLES, EXPERIMENTS, THREADS, ( i ) -> {
         Thread.sleep( 10 );
      } );

      LimitedTimeExecutor lt = new LimitedTimeExecutor( 100, TimeUnit.MILLISECONDS );

      benchmark( "LimitedTime", SAMPLES, EXPERIMENTS, THREADS, ( i ) -> {
         lt.execute( () -> {
            try {
               Thread.sleep( 10 );
            } catch( InterruptedException e ) {
               e.printStackTrace();
            }
         } );
      } );
   }
}
