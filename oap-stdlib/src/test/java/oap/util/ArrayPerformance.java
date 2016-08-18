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
import org.testng.annotations.Test;

import java.util.Random;

/**
 * Created by Igor.Petrenko on 18.08.2016.
 */
public class ArrayPerformance extends AbstractPerformance {

   private final Random random = new Random();

   @Test
   public void testClassVsArray() {
      final int samples = 50000000;
      final int experiments = 5;

      final long[] array = new long[]{ 1, 2, 3, 4 };
      final Test4 test4 = new Test4( 1, 2, 3, 4 );

      final long[] sum = new long[]{ 0 };

      benchmark( "class_vs_array->array", samples, experiments, ( i ) -> {
         final int x = random.nextInt(4);
         switch( x ) {
            case 0:
               sum[0] += array[x];
               break;
            case 1:
               sum[0] += array[x];
               break;
            case 2:
               sum[0] += array[x];
               break;
            case 3:
               sum[0] += array[x];
               break;
         }
      } );

      benchmark( "class_vs_array->class", samples, experiments, ( i ) -> {
         final int x = random.nextInt(4);
         switch( x ) {
            case 0:
               sum[0] += test4.a1;
               break;
            case 1:
               sum[0] += test4.a2;
               break;
            case 2:
               sum[0] += test4.a3;
               break;
            case 3:
               sum[0] += test4.a4;
               break;
         }
      } );

      System.out.println(sum[0]);
   }

   @Test
   public void testArrayType() {
      final int samples = 50000000;
      final int experiments = 5;

      Object[] data = new Object[10];
      boolean[] array = new boolean[10];
      for( int i = 0; i < 9; i++ ) {
         data[i] = ( long ) i;
         array[i] = true;
      }
      array[9] = false;

      long[] sum = new long[]{ 0 };

      data[9] = new long[]{ 10, 20 };
      benchmark( "as-array", samples, experiments, ( i ) -> {
         final int x = random.nextInt(10);

         if( array[x] ) sum[0] += ( long ) data[x];
         else {
            final long[] t = ( long[] ) data[x];
            sum[0] -= t[0];
            sum[0] += t[1];
         }
      } );

      data[9] = new Test2( 10, 20 );
      benchmark( "as-object", samples, experiments, ( i ) -> {
         final int x = random.nextInt(10);

         if( array[x] ) sum[0] += ( long ) data[x];
         else {
            final Test2 t = ( Test2 ) data[x];
            sum[0] -= t.min;
            sum[0] += t.max;
         }
      } );

      long[][] data2 = new long[10][];
      for( int i = 0; i < 9; i++ ) data2[i] = new long[]{ i };
      data2[9] = new long[]{ 10, 20 };

      benchmark( "as-long[]", samples, experiments, ( i ) -> {
         final int x = random.nextInt(10);

         if( array[x] ) sum[0] += data2[x][0];
         else {
            final long[] t = data2[x];
            sum[0] -= t[0];
            sum[0] += t[1];
         }
      } );

      System.out.println(sum[0]);
   }

   public static class Test2 {
      public final long max;
      public final long min;

      public Test2( long max, long min ) {
         this.max = max;
         this.min = min;
      }
   }

   public static class Test4 {
      public final long a1;
      public final long a2;
      public final long a3;
      public final long a4;

      public Test4( long a1, long a2, long a3, long a4 ) {
         this.a1 = a1;
         this.a2 = a2;
         this.a3 = a3;
         this.a4 = a4;
      }
   }
}
