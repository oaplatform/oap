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
import oap.dictionary.DictionaryParser;
import oap.etl.StringExport;
import oap.etl.Table;
import oap.testng.AbstractTest;
import oap.tsv.DictionaryModel;
import oap.tsv.Model;
import org.testng.annotations.Test;

import static oap.etl.accumulator.AccumulatorType.AVG;
import static oap.etl.accumulator.AccumulatorType.COUNT;
import static oap.etl.accumulator.AccumulatorType.SUM;
import static oap.testng.Asserts.assertString;

public class ConfigurationTest extends AbstractTest {
   @Test
   public void testGroupAndCount() {
      // {
      //   "table":"table",
      //   "aggregates":{
      //     "_by_column1_column2": {
      //       "fields":["column1","column2"],
      //       "accumulators":[{
      //         "name":"count",
      //         "type":"COUNT"
      //       }],
      //       "export":"export"
      //     }
      //   }
      // }
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .table( "table" )
         .aggregator( "_by_column1_column2" ).fields( "column1", "column2" )
         .accumulator( "count" ).operation( COUNT )
         .export( "export" )
         .build();

      val export = new StringExport();

      val dictionary = DictionaryParser.parseFromString( "{\n" +
         "  \"name\" : \"config.v14\",\n" +
         "  \"version\" : 14,\n" +
         "  \"values\" : [ {\n" +
         "    \"id\" : \"table\",\n" +
         "    \"eid\" : 0,\n" +
         "    \"values\" : [ {\n" +
         "      \"id\" : \"column1\",\n" +
         "      \"eid\" : 0,\n" +
         "      \"type\" : \"STRING\",\n" +
         "      \"default\" : \"\"\n" +
         "    }, " +
         "    {\n" +
         "      \"id\" : \"column2\",\n" +
         "      \"eid\" : 1,\n" +
         "      \"type\" : \"STRING\",\n" +
         "      \"default\" : \"\"\n" +
         "    }, " +
         "    {\n" +
         "      \"id\" : \"value\",\n" +
         "      \"eid\" : 2,\n" +
         "      \"type\" : \"INTEGER\",\n" +
         "      \"default\" : 0\n" +
         "    }]\n" +
         "  }]\n" +
         "} "
      );

      final DictionaryModel dictionaryModel = new DictionaryModel( dictionary );
      AggregatorBuilder
         .custom()
         .withModel( dictionaryModel )
         .withConfiguration( aggregatorConfiguration )
         .withTable( "table", Table.fromString( "a\tb\t10\n" +
            "a\tb\t20\n" +
            "a1\tb1\t10\n", dictionaryModel.toModel( "table" ) ) )
         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\tb\t2\na1\tb1\t1\n" );
   }

   @Test
   public void testGroupAndSum() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .table( "table" )
         .aggregator( "_by_column1" ).fields( "column1" )
         .accumulator( "count" ).operation( COUNT )
         .accumulator( "sumi" ).operation( SUM ).field( "valuei" )
         .accumulator( "suml" ).operation( SUM ).field( "valuel" )
         .accumulator( "sumd" ).operation( SUM ).field( "valued" )
         .export( "export" )
         .build();

      final Model model = new Model( false ).s( "column1", 0 ).i( "valuei", 1 ).l( "valuel", 2 ).d( "valued", 3 );

      StringExport export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "table", model )
         .withTable( "table", Table.fromString( "a\t10\t11\t1.1\n" +
            "a\t20\t21\t2.2\n" +
            "a1\t10\t10\t1.1\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t2\t30\t32\t3.3000000000000003\na1\t1\t10\t10\t1.1\n" );
   }

   @Test
   public void testGroupAndAvg() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .table( "table" )
         .aggregator( "by_column1" ).fields( "column1" )
         .accumulator( "avg" ).operation( AVG ).field( "value" )
         .export( "export" )
         .build();

      val model = new Model( false ).s( "column1", 0 ).i( "value", 1 );
      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "table", model )
         .withTable( "table", Table.fromString( "a\t10\n" +
            "a\t20\n" +
            "a1\t10\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t15.0\na1\t10.0\n" );
   }

   @Test
   public void testGroupAndCountFilter() {
      val aggregatorConfiguration = AggregatorConfigurationBuilder
         .custom()
         .table( "table" )
         .aggregator( "by_column1" ).fields( "column1" )
         .accumulator( "count" ).operation( COUNT ).filter( "filter", "==", "test" )
         .export( "export" )
         .build();

      val model = new Model( false ).s( "column1", 0 ).i( "value", 1 ).s( "filter", 2 );
      val export = new StringExport();

      AggregatorBuilder
         .custom()
         .withModel( "table", model )
         .withTable( "table", Table.fromString( "a\t10\ttest\n" +
            "a\t20\tunknown\n" +
            "a1\t10\ttest\n", model ) )
         .withConfiguration( aggregatorConfiguration )
         .withExport( "export", ( s ) -> export )
         .build();

      assertString( export.toString() ).isEqualTo( "a\t1\n" +
         "a1\t1\n" );
   }
}
