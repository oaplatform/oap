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
import oap.json.Binder;
import oap.testng.AbstractTest;
import oap.tsv.Model;
import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.etl.accumulator.AccumulatorType.AVG;
import static oap.etl.accumulator.AccumulatorType.COUNT;
import static oap.etl.accumulator.AccumulatorType.SUM;
import static oap.testng.Asserts.assertString;
import static oap.util.Pair.__;

/**
 * Created by Admin on 31.05.2016.
 */
public class ConfigurationTest extends AbstractTest {
   @Test
   public void testGroupAndCount() {
      // {
      //   "table":"from",
      //   "aggregates":[{
      //     "select":[{
      //       "name":"count",
      //       "type":"COUNT"
      //     }],
      //     "export":"export",
      //     "groupBy":["column1","column2"]
      //   }]
      // }
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .from( "from" )
         .select( "count", COUNT ).groupBy( "column1", "column2" ).export( "export" )
         .build();

      val export = new StringExport();
      val model = new Model( false ).s( 0, 1 ).i( 2 );

      AggregatorBuilder
         .custom()
         .withModel( "from", model, Maps.of( __( "column1", 0 ), __( "column2", 1 ), __( "value", 2 ) ) )
         .withConfiguration( aggregatorConfiguration )
         .withTable( "from", Table.fromString( "a\tb\t10\n" +
            "a\tb\t20\n" +
            "a1\tb1\t10\n", model ) )
         .withExport( "export", export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\tb\t2\na1\tb1\t1\n" );
   }

   @Test
   public void testGroupAndSum() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .from( "from" )

         .select( "count", COUNT )
         .select( "sumi", SUM, "valuei" )
         .select( "suml", SUM, "valuel" )
         .select( "sumd", SUM, "valued" )
         .groupBy( "column1" )
         .export( "export" )

         .build();

      final Model model = new Model( false ).s( 0 ).i( 1 ).l( 2 ).d( 3 );

      StringExport export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "from", model, Maps.of( __( "column1", 0 ), __( "valuei", 1 ), __( "valuel", 2 ), __( "valued", 3 ) ) )
         .withTable( "from", Table.fromString( "a\t10\t11\t1.1\n" +
            "a\t20\t21\t2.2\n" +
            "a1\t10\t10\t1.1\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t2\t30\t32\t3.3000000000000003\na1\t1\t10\t10\t1.1\n" );
   }

   @Test
   public void testGroupAndAvg() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .from( "from" )
         .select( "avg", AVG, "value" ).groupBy( "column1" )
         .export( "export" )
         .build();

      val model = new Model( false ).s( 0 ).i( 1 );
      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "from", model, Maps.of( __( "column1", 0 ), __( "value", 1 ) ) )
         .withTable( "from", Table.fromString( "a\t10\n" +
            "a\t20\n" +
            "a1\t10\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t15.0\na1\t10.0\n" );
   }

   @Test
   public void testGroupAndCountFilter() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .from( "from" )
         .select( "count", COUNT ).withFilter( "filter", "==", "test" ).groupBy( "column1" )
         .export( "export" )
         .build();

      val model = new Model( false ).s( 0 ).i( 1 ).s( 2 );
      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "from", model, Maps.of( __( "column1", 0 ), __( "value", 1 ), __( "filter", 2 ) ) )
         .withTable( "from", Table.fromString( "a\t10\ttest\n" +
            "a\t20\tunknown\n" +
            "a1\t10\ttest\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t1\n" +
         "a1\t1\n" );
   }
}
