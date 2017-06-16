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

package oap.template;

import com.google.common.collect.ImmutableMap;
import lombok.val;
import oap.io.Files;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.mockito.internal.util.collections.Sets;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.template.Template.Line.line;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
public class EngineTest extends AbstractTest {

    private Engine engine;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        final Path test = Env.tmpPath( "test" );
        Files.ensureDirectory( test );
        engine = new Engine( test, true );
    }

    @Test
    public void testProcessString() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "val" );
    }

    @Test
    public void testProcessEmptyPath() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testStr", null, "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "d" );
    }

    @Test
    public void testProcessStringReload() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "val" );
    }

    @Test
    public void testProcessDefault() throws Exception {
        engine.getTemplate( "test1", Test1.class, asList( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) );


        assertThat( engine.getTemplate( "test2", Test1.class, asList( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) ) ).isEqualTo( "d1 d2" );
        assertThat( engine.getTemplate( "test3", Test1.class, asList( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) ) ).isEqualTo( "d1 d2" );
    }

    @Test
    public void testProcessArray() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "array", "array", emptyList() ) ), " " )
            .renderString( new Test1( asList( "1", "2" ) ) ) ).isEqualTo( "[1,2]" );
    }

    @Test
    public void testProcessSet() throws Exception {
        Test1 source = new Test1( asList( "1", "2" ) );
        source.set = Sets.newSet( "4", "5" );
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "array", "set", emptyList() ) ), " " )
            .renderString( source ) ).isEqualTo( "[4,5]" );
    }

    @Test
    public void testProcessOptString() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "optStr", "optStr", "d" ) ), " " )
            .renderString( new Test1( Optional.of( "test" ) ) ) ).isEqualTo( "test" );
    }

    @Test
    public void testOr() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "optStr", "optStr|testStr", "d" ) ), " " )
            .renderString( new Test1( Optional.of( "test1" ) ) ) ).isEqualTo( "test1" );
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "optStr", "optStr|testStr", "d" ) ), " " )
            .renderString( new Test1( "test" ) ) ).isEqualTo( "test" );
    }

    @Test
    public void testProcessStringNull() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( ( String ) null ) ) ).isEqualTo( "d" );
    }

    @Test
    public void testProcessInt() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testInt", "testInt", 1 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "235" );
    }

    @Test
    public void testProcessIntDiv2() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "testInt", "testInt/2", 1 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "117" );
    }

    @Test
    public void testProcessConc() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList(
            line( "t", "{testInt,\"x\",testInt2}", 2 ),
            line( "t", "{testInt,\"x\",testInt2}", 2 )
        ), " " )
            .renderString( new Test1( 235, 12 ) ) ).isEqualTo( "235x12 235x12" );
    }

    @Test
    public void testProcessFunction() throws Exception {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "f", "getTestInt()", 10 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "235" );
    }

    @Test
    public void testDelimiter() {
        assertThat( engine.getTemplate( "test", Test1.class, asList(
            line( "testStr", "testStr", "d" ),
            line( "testInt", "testInt", 2 ) ), " " )
            .renderString( new Test1( "str", 235 ) ) ).isEqualTo( "str 235" );
    }

    @Test
    public void testNested() {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "test2", "test2.testStr", "d" ), line( "test3", "test2.testInt", 2 ) ), " " )
            .renderString( new Test1( new Test2( "str", 235 ) ) ) ).isEqualTo( "str 235" );
    }

    @Test
    public void testNestedOptional() {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "opt", "optTest2.testStr", "d" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2( "str" ) ) ) ) ).isEqualTo( "str" );
    }

    @Test
    public void testNestedOptionalSeparators() {
        assertThat( engine.getTemplate( "test", Test1.class, asList(
            line( "opt", "optTest2.testStr", "d" ),
            line( "testInt", "optTest2.testInt", 1 )
        ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2( "str", 10 ) ) ) ) ).isEqualTo( "str 10" );
    }

    @Test
    public void testNestedOptionalEmpty() {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "opt", "optTest2.test1.testStr", "def" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.empty() ) ) ).isEqualTo( "def" );
    }

    @Test
    public void testNested2() {
        assertThat( engine.getTemplate( "test", Test1.class, asList( line( "f1", "test2.testStr", "d" ), line( "f2", "test2.test1.testInt", 2 ) ), " " )
            .renderString( new Test1( new Test2( "n2", 2, new Test1( "str", 235 ) ) ) ) ).isEqualTo( "n2 235" );
    }

    @Test
    public void testNestedMap() {
        val sample = new Test4( new Test3( ImmutableMap.of( "mapKey", "mapValue" ) ) );
        assertThat( engine.getTemplate( "test", Test4.class,
            singletonList( line( "f1", "test3.map.mapKey", "unknown" ) ), " " )
            .renderString( sample ) ).isEqualTo( "mapValue" );
    }


    public static class Test1 {
        public String testStr;
        public Optional<String> optStr = Optional.empty();
        public int testInt;
        public int testInt2;
        public Test2 test2;
        public Optional<Test2> optTest2 = Optional.empty();
        public List<String> array = new ArrayList<>();
        public Set<String> set = new HashSet<>();

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