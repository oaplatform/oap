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

package oap.etl.accumulator;

import oap.etl.StringExport;
import oap.etl.Table;
import oap.io.IoStreams;
import oap.testng.AbstractPerformance;
import oap.testng.Env;
import oap.tsv.Model;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Igor Petrenko on 27.04.2016.
 */
@Test( enabled = false )
public class AccumulatorPerformance extends AbstractPerformance {

   public static final int SAMPLES = 200;
   public static final int EXPERIMENTS = 5;
   private Path path1;
   private Path path2;
   private Random random;

   @BeforeMethod
   @Override
   public void beforeMethod() {
      super.beforeMethod();

      path1 = Env.tmpPath( "test.tsv" );
      path2 = Env.tmpPath( "test2.tsv" );

      writeFile( path1 );
      writeFile( path2 );
   }

   @DataProvider( name = "count" )
   public Object[][] aggregates() {
      return new Object[][]{ { 1 }, { 2 }, { 4 }, { 8 } };
   }

   private void writeFile( Path path ) {
      try( OutputStream out = IoStreams.out( path, IoStreams.Encoding.GZIP ) ) {
         for( int y = 0; y < 10000; y++ ) {
            random = new Random();
            final String row = IntStream
               .range( 0, 6 )
               .mapToObj( x -> x > 2 ? String.valueOf( random.nextInt( 10 ) ) : "test-" + x + "-" + random.nextInt( 10 ) )
               .collect( Collectors.joining( "\t" ) ) + "\n";
            out.write( row.getBytes() );
         }
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   @Test( dataProvider = "count" )
   public void testAggregatedByHM( int count ) {
      benchmark( "accumulator.without_sort", SAMPLES, EXPERIMENTS, ( i ) -> {

         Table.GroupBy[] groups = Stream
            .generate( () -> new Table.GroupBy( new int[]{ 0, 1, 2 }, Accumulator.count(), Accumulator.intSum( 3 ), Accumulator.intSum( 4 ) ) )
            .limit( count )
            .toArray( Table.GroupBy[]::new );

         List<Table> tables = Table.fromPaths( Arrays.asList( path1, path2 ),
            Model.withoutHeader().s( 0, 1, 2 ).i( 3, 4, 5 ) )
            .groupBy( groups )
            .getTables();

         for( int x = 0; x < count; x++ ) {
            StringExport export = new StringExport();

            tables.get( x ).sort( new int[]{ 0, 1, 2 } ).export( export ).compute();
         }
      } );
   }
}
