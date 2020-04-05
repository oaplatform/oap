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

import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.Lists;
import oap.util.Sets;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.io.Files.ensureDirectory;
import static oap.template.Template.Line.line;
import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;

public class EngineTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    private final Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

    @Test
    public void processString() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "val" );
    }

    @Test
    public void processWithoutVariables() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", null, "d" ) ), " " ) )
            .isExactlyInstanceOf( ConstTemplate.class );
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", null, "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "d" );
    }

    @Test
    public void processEmptyPath() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", null, "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "d" );
    }

    @Test
    public void processStringReload() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( "val" ) ) ).isEqualTo( "val" );
    }

    @Test
    public void processDefault() {
        engine.getTemplate( "test1", Test1.class, Lists.of( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) );


        assertThat( engine.getTemplate( "test2", Test1.class, Lists.of( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) ) ).isEqualTo( "d1 d2" );
        assertThat( engine.getTemplate( "test3", Test1.class, Lists.of( line( "testStr", "testStr", "d1" ), line( "testStr2", "optTest2.testStr", "d2" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2() ) ) ) ).isEqualTo( "d1 d2" );
    }

    @Test
    public void processArray() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "array", "array", emptyList() ) ), " " )
            .renderString( new Test1( Lists.of( "1", "2" ) ) ) ).isEqualTo( "[1,2]" );
    }

    @Test
    public void processSet() {
        Test1 source = new Test1( Lists.of( "1", "2" ) );
        source.set = Sets.of( "4", "5" );
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "array", "set", emptyList() ) ), " " )
            .renderString( source ) ).isEqualTo( "[4,5]" );
    }

    @Test
    public void processOptString() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "optStr", "optStr", "d" ) ), " " )
            .renderString( new Test1( Optional.of( "test" ) ) ) ).isEqualTo( "test" );
    }

    @Test
    public void or() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "optStr", "optStr|testStr", "d" ) ), " " )
            .renderString( new Test1( Optional.of( "test1" ) ) ) ).isEqualTo( "test1" );
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "optStr", "optStr|testStr", "d" ) ), " " )
            .renderString( new Test1( "test" ) ) ).isEqualTo( "test" );
    }

    @Test
    public void processStringNull() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testStr", "testStr", "d" ) ), " " )
            .renderString( new Test1( ( String ) null ) ) ).isEqualTo( "d" );
    }

    @Test
    public void processInt() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testInt", "testInt", 1 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "235" );
    }

    @Test
    public void processIntDiv2() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "testInt", "testInt/2", 1 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "117" );
    }

    @Test
    public void processConc() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of(
            line( "t", "{testInt,\"x\",testInt2}", 2 ),
            line( "t", "{testInt,\"x\",testInt2}", 2 )
        ), " " )
            .renderString( new Test1( 235, 12 ) ) ).isEqualTo( "235x12 235x12" );
    }

    @Test
    public void processFunction() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "f", "getTestInt()", 10 ) ), " " )
            .renderString( new Test1( 235 ) ) ).isEqualTo( "235" );
        assertThat( engine.getTemplate( "test", Map.class, Lists.of( line( "f", "getTest()", 10 ) ), " " )
            .renderString( Map.of( "getTest()", 235 ) ) ).isEqualTo( "" );
    }

    @Test
    public void delimiter() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of(
            line( "testStr", "testStr", "d" ),
            line( "testInt", "testInt", 2 ) ), " " )
            .renderString( new Test1( "str", 235 ) ) ).isEqualTo( "str 235" );
    }

    @Test
    public void nested() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "test2", "test2.testStr", "d" ), line( "test3", "test2.testInt", 2 ) ), " " )
            .renderString( new Test1( new Test2( "str", 235 ) ) ) ).isEqualTo( "str 235" );
    }

    @Test
    public void nestedOptional() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "opt", "optTest2.testStr", "d" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2( "str" ) ) ) ) ).isEqualTo( "str" );
    }

    @Test
    public void nestedNullable() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "opt", "nullableTest2.testStr", "d" ) ), " " )
            .renderString( new Test1() ) ).isEqualTo( "d" );
    }

    @Test
    public void nestedOptionalSeparators() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of(
            line( "opt", "optTest2.testStr", "d" ),
            line( "testInt", "optTest2.testInt", 1 )
        ), " " )
            .renderString( new Test1( Optional.empty(), Optional.of( new Test2( "str", 10 ) ) ) ) ).isEqualTo( "str 10" );
    }

    @Test
    public void nestedOptionalEmpty() {
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "opt", "optTest2.test1.testStr", "def" ) ), " " )
            .renderString( new Test1( Optional.empty(), Optional.empty() ) ) ).isEqualTo( "def" );
    }

    @Test
    public void nested2() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );
        assertThat( engine.getTemplate( "test", Test1.class, Lists.of( line( "f1", "test2.testStr", "d" ), line( "f2", "test2.test1.testInt", 2 ) ), " " )
            .renderString( new Test1( new Test2( "n2", 2, new Test1( "str", 235 ) ) ) ) ).isEqualTo( "n2 235" );
    }

    @Test
    public void nestedMap() {
        var sample = new Test4( new Test3( Map.of( "mapKey", "mapValue" ) ) );
        assertThat( engine.getTemplate( "test", Test4.class,
            singletonList( line( "f1", "test3.map.mapKey", "unknown" ) ), " " )
            .renderString( sample ) ).isEqualTo( "mapValue" );
    }

    @Test
    public void mutableStrategy() {
        var sample = new Test4( new Test3( Map.of( "mapKey", "mapValue" ) ) );
        var strategy = new TestTemplateStrategy();
        final Template<Test4, Template.Line> template = engine.getTemplate( "test", Test4.class,
            Lists.of(
                line( "f1", "test3.map.mapKey", "unknown" ),
                line( "f1", "test3.map.mapKey", "unknown" ) ), " ", strategy );
        assertThat( template.renderString( sample ) ).isEqualTo( "a0mapValue b0a1mapValueb1" );
    }

    public static class Test1 {
        public String testStr;
        public Optional<String> optStr = Optional.empty();
        public int testInt;
        public int testInt2;
        public Test2 test2;
        public Optional<Test2> optTest2 = Optional.empty();
        @Template.Nullable
        public Test2 nullableTest2 = null;
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

    public static class TestTemplateStrategy implements TemplateStrategy<Template.Line> {
        private int mutable1;
        private int mutable2;

        @Override
        public void beforeLine( StringBuilder c, Template.Line line, String delimiter ) {
            c.append( "acc.accept( \"a\" + " ).append( mutable1 ).append( " );" );
            mutable1++;
        }

        @Override
        public void afterLine( StringBuilder c, Template.Line line, String delimiter ) {
            c.append( "acc.accept( \"b\" + " ).append( mutable2 ).append( " );" );
            mutable2++;
        }
    }
}
