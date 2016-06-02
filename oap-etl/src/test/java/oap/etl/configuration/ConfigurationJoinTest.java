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
import oap.etl.StringExport;
import oap.etl.Table;
import oap.testng.AbstractTest;
import oap.tsv.Model;
import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.etl.accumulator.AccumulatorType.COUNT;
import static oap.etl.accumulator.AccumulatorType.SUM;
import static oap.testng.Asserts.assertString;
import static oap.util.Pair.__;

/**
 * Created by Admin on 31.05.2016.
 */
public class ConfigurationJoinTest extends AbstractTest {
   @Test
   public void testCountingJoin() {
      final Aggregator join = AggregatorConfigurationBuilder
         .custom()
         .from( "table2" )
         .select( "count", COUNT )
         .groupBy( "column" )
         .build();

      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .from( "table1" )
         .join( "join", join )
         .select( "sum", SUM, "value1" )
         .select( "sumj", SUM, "join.count" )
         .groupBy( "gcol" ).export( "export" )
         .build();

      final Model model1 = new Model( false ).s( 0, 1 ).i( 2 );
      final Model model2 = new Model( false ).s( 0 ).i( 1 );

      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "table1", model1, Maps.of( __( "gcol", 0 ), __( "column", 1 ), __( "value1", 2 ) ) )
         .withModel( "table2", model2, Maps.of( __( "column", 0 ), __( "value2", 1 ) ) )

         .withTable( "table1", Table.fromString( "s\ta\t10\n" +
            "s\ta\t20\n" +
            "s2\ta\t10\n", model1 ) )
         .withTable( "table2", Table.fromString( "a\t20\n" +
            "a\t21\n" +
            "a1\t21\n", model2 ) )

         .withConfiguration( aggregatorConfiguration )

         .withExport( "export", export )
         .build();

      assertString( export.toString() ).isEqualTo( "s\t30\t4\n" +
         "s2\t10\t2\n" );
   }
}
