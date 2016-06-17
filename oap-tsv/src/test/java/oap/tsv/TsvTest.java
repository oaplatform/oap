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

package oap.tsv;

import oap.io.IoStreams;
import oap.testng.Env;
import oap.util.Stream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

import static oap.testng.Asserts.assertFile;
import static org.testng.Assert.assertEquals;

public class TsvTest {
   @Test
   public void tabs() {
      String tsv = "aaaa\tbbbb\txxxx\tddd\t19/11/2011\t33.3\taaaa\t11\txxx\tvvvv\tS\tS\t444\txxx\t4444\t1234\tN\tN";
      List<String> delimited = Tsv.parse( tsv );
      assertEquals( delimited.size(), 18 );
   }

   @DataProvider( name = "files" )
   public Object[][] files() {
      return new Object[][]{
         { "1.tsv", IoStreams.Encoding.PLAIN },
         { "1.tsv.gz", IoStreams.Encoding.GZIP },
         { "1.tsv.zip", IoStreams.Encoding.ZIP }
      };
   }

   @Test( dataProvider = "files" )
   public void loadTsv( String file, IoStreams.Encoding encoding ) {
      Model model = Model.withoutHeader().s( 1 ).i( 3 ).filterColumnCount( 4 );
      Path path = Env.deployTestData( getClass() );

      Stream<List<Object>> tsv = Tsv.fromPath( path.resolve( file ), model );
      assertFile( path.resolve( "result.tsv" ) ).hasContent( Tsv.print( tsv ) );
   }

}
