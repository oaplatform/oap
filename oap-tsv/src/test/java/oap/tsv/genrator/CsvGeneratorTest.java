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

package oap.tsv.genrator;

import com.google.common.collect.ImmutableMap;
import oap.testng.AbstractTest;
import oap.tsv.genrator.CsvGenerator.Line;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.tsv.genrator.CsvGenerator.Line.line;
import static oap.tsv.genrator.CsvGeneratorStrategy.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
public class CsvGeneratorTest extends AbstractTest {
   @Test
   public void testProcessString() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "testStr", "testStr", "d" ) ), ' ', DEFAULT )
         .process( new Test1( "val" ) ) ).isEqualTo( "val" );
   }

   @Test
   public void testProcessDefault() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), ' ', DEFAULT )
         .process( new Test1( Optional.empty(), Optional.of( new Test2() ) ) ) ).isEqualTo( "d1 d2" );
   }

   @Test
   public void testProcessArray() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "array", "array", emptyList() ) ), ' ', DEFAULT )
         .process( new Test1( asList( "1", "2" ) ) ) ).isEqualTo( "[1,2]" );
   }

   @Test
   public void testProcessOptString() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "optStr", "optStr", "d" ) ), ' ', DEFAULT )
         .process( new Test1( Optional.of( "test" ) ) ) ).isEqualTo( "test" );
   }

   @Test
   public void testOr() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "optStr", "optStr|testStr", "d" ) ), ' ', DEFAULT )
         .process( new Test1( Optional.of( "test1" ) ) ) ).isEqualTo( "test1" );
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "optStr", "optStr|testStr", "d" ) ), ' ', DEFAULT )
         .process( new Test1( "test" ) ) ).isEqualTo( "test" );
   }

   @Test
   public void testProcessStringNull() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "testStr", "testStr", "d" ) ), ' ', DEFAULT )
         .process( new Test1( ( String ) null ) ) ).isEqualTo( "d" );
   }

   @Test
   public void testProcessInt() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "testInt", "testInt", 1 ) ), ' ', DEFAULT )
         .process( new Test1( 235 ) ) ).isEqualTo( "235" );
   }

   @Test
   public void testProcessIntDiv2() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "testInt", "testInt/2", 1 ) ), ' ', DEFAULT )
         .process( new Test1( 235 ) ) ).isEqualTo( "117" );
   }

   @Test
   public void testProcessConc() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList(
         line( "t", "{testInt,\"x\",testInt2}", 2 ),
         line( "t", "{testInt,\"x\",testInt2}", 2 )
      ), ' ', DEFAULT )
         .process( new Test1( 235, 12 ) ) ).isEqualTo( "235x12 235x12" );
   }

   @Test
   public void testProcessFunction() throws Exception {
      assertThat( new CsvGenerator<>( Test1.class, asList( line( "f", "getTestInt()", 10 ) ), ' ', DEFAULT )
         .process( new Test1( 235 ) ) ).isEqualTo( "235" );
   }

   @Test
   public void testDelimiter() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList( line( "testStr", "testStr", "d" ), line( "testInt", "testInt", 2 ) ), ' ', DEFAULT );
      assertThat( test.process( new Test1( "str", 235 ) ) ).isEqualTo( "str 235" );
   }

   @Test
   public void testNested() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList( line( "test2", "test2.testStr", "d" ), line( "test3", "test2.testInt", 2 ) ), ' ', DEFAULT );
      assertThat( test.process( new Test1( new Test2( "str", 235 ) ) ) ).isEqualTo( "str 235" );
   }

   @Test
   public void testNestedOptional() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList( line( "opt", "optTest2.testStr", "d" ) ), ' ', DEFAULT );
      assertThat( test.process( new Test1( Optional.empty(), Optional.of( new Test2( "str" ) ) ) ) ).isEqualTo( "str" );
   }

   @Test
   public void testNestedOptionalSeparators() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList(
         line( "opt", "optTest2.testStr", "d" ),
         line( "testInt", "optTest2.testInt", 1 )
      ), ' ', DEFAULT );
      assertThat( test.process( new Test1( Optional.empty(), Optional.of( new Test2( "str", 10 ) ) ) ) ).isEqualTo( "str 10" );
   }

   @Test
   public void testNestedOptionalEmpty() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList( line( "opt", "optTest2.test1.testStr", "def" ) ), ' ', DEFAULT );
      assertThat( test.process( new Test1( Optional.empty(), Optional.empty() ) ) ).isEqualTo( "def" );
   }

   @Test
   public void testNested2() {
      final CsvGenerator<Test1, Line> test = new CsvGenerator<>( Test1.class, asList( line( "f1", "test2.testStr", "d" ), line( "f2", "test2.test1.testInt", 2 ) ), ' ', DEFAULT );
      assertThat( test.process( new Test1( new Test2( "n2", 2, new Test1( "str", 235 ) ) ) ) ).isEqualTo( "n2 235" );
   }

   @Test
   public void testNestedMap() {
      Test4 sample = new Test4( new Test3( ImmutableMap.of( "mapKey", "mapValue" ) ) );
      final CsvGenerator<Test4, Line> test = new CsvGenerator<>( Test4.class,
         singletonList( line( "f1", "test3.map.mapKey", "unknown" ) ), ' ', DEFAULT );

      assertThat( test.process( sample ) ).isEqualTo( "mapValue" );
   }


   public static class Test1 {
      public String testStr;
      public Optional<String> optStr = Optional.empty();
      public int testInt;
      public int testInt2;
      public Test2 test2;
      public Optional<Test2> optTest2 = Optional.empty();
      public List<String> array = new ArrayList<>();

      public Test1() {
      }

      public Test1( List<String> array ) {
         this.array = array;
      }

      public Test1( String testStr ) {
         this.testStr = testStr;
      }

      public Test1( Optional<String> optStr ) {
         this.optStr = optStr;
      }

      public Test1( Optional<String> optStr, Optional<Test2> optTest2 ) {
         this.optStr = optStr;
         this.optTest2 = optTest2;
      }

      public Test1( int testInt ) {
         this.testInt = testInt;
      }

      public Test1( int testInt, int testInt2 ) {
         this.testInt = testInt;
         this.testInt2 = testInt2;
      }

      public Test1( String testStr, int testInt ) {
         this.testStr = testStr;
         this.testInt = testInt;
      }

      public Test1( Test2 test2 ) {
         this.test2 = test2;
      }

      public Test1( String testStr, int testInt, Test2 test2 ) {
         this.testStr = testStr;
         this.testInt = testInt;
         this.test2 = test2;
      }

      public int getTestInt() {
         return testInt;
      }
   }

   public static class Test2 {
      public String testStr;
      public int testInt;
      public Test1 test1;

      public Test2() {
      }

      public Test2( String testStr ) {
         this.testStr = testStr;
      }

      public Test2( int testInt ) {
         this.testInt = testInt;
      }

      public Test2( String testStr, int testInt ) {
         this.testStr = testStr;
         this.testInt = testInt;
      }

      public Test2( Test1 test1 ) {
         this.test1 = test1;
      }

      public Test2( String testStr, int testInt, Test1 test1 ) {
         this.testStr = testStr;
         this.testInt = testInt;
         this.test1 = test1;
      }
   }

   public static class Test4 {
      public final Test3 test3;

      public Test4( Test3 test3 ) {
         this.test3 = test3;
      }
   }
   public static class Test3 {
      public final Map<?, Object> map;

      public Test3( Map<?, Object> map ) {
         this.map = map;
      }
   }
}