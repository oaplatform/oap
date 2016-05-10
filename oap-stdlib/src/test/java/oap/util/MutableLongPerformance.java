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

package oap.util;

import oap.testng.AbstractPerformance;
import org.apache.commons.lang3.mutable.MutableLong;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Created by Admin on 10.05.2016.
 */
@Test( enabled = false )
public class MutableLongPerformance extends AbstractPerformance {
   @Test
   public void testIncrement() {
      final int SAMPLES = 1000000;
      final int EXPERIMENTS = 5;

      final HashMap<Integer, MutableLong> map1 = new HashMap<>();
      final HashMap<Integer, Long> map2 = new HashMap<>();

      benchmark( "mutable_long", SAMPLES, EXPERIMENTS, ( i ) -> {
         map1.computeIfAbsent( i % 5, ( k ) -> new MutableLong() ).increment();
      } );

      benchmark( "Long_compute", SAMPLES, EXPERIMENTS, ( i ) -> {
         map2.compute( i % 5, ( k, old ) -> old != null ? old + 1 : 1L );
      } );
   }
}
