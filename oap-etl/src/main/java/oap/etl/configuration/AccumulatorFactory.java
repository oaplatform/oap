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

package oap.etl.configuration;

import lombok.val;
import oap.etl.accumulator.Accumulator;
import oap.etl.accumulator.AvgAccumulator;
import oap.etl.accumulator.CountAccumulator;
import oap.etl.accumulator.DoubleSumAccumulator;
import oap.etl.accumulator.IntegerSumAccumulator;
import oap.etl.accumulator.LongSumAccumulator;
import oap.tsv.Model;
import oap.util.Pair;

import java.util.Optional;

/**
 * Created by Admin on 31.05.2016.
 */
public class AccumulatorFactory {
   public static Accumulator create( oap.etl.configuration.Accumulator accumulator, Optional<Pair<Integer, Model.ColumnType>> field ) {
      switch( accumulator.type ) {
         case COUNT:
            return new CountAccumulator();
         case SUM: {
            val f = field.orElseThrow( () -> new IllegalArgumentException( "SUM/" + accumulator.name + ": Unknown fields" ) );
            switch( f._2 ) {
               case INT:
                  return new IntegerSumAccumulator( f._1 );
               case LONG:
                  return new LongSumAccumulator( f._1 );
               case DOUBLE:
                  return new DoubleSumAccumulator( f._1 );
               default:
                  throw new IllegalArgumentException( "SUM/" + accumulator.name + " : Unknown type " + f._2 );
            }
         }
         case AVG: {
            val f = field.orElseThrow( () -> new IllegalArgumentException( "AVG/" + accumulator.name + ": Unknown fields" ) );
            return new AvgAccumulator( f._1 );
         }
         default:
            throw new IllegalArgumentException( accumulator.name + " Unknown accumulator type " + accumulator.type );
      }
   }
}
