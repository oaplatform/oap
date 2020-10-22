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

import lombok.AllArgsConstructor;
import oap.io.Files;
import oap.template.StringTemplateTest.Tst.Test1;
import oap.template.StringTemplateTest.Tst.Test2;
import oap.template.StringTemplateTest.Tst.Test3;
import oap.template.StringTemplateTest.Tst.Test4;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static oap.io.Files.ensureDirectory;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class StringTemplateTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void ttl() {
        var test = ensureDirectory( TestDirectoryFixture.testPath( "test" ) );
        var engine = new Engine( test );
        var template = engine.getTemplate( "name", EngineTest.Test1.class, "test${tst.test2.i}" );

        template.renderString( new EngineTest.Test1(), Map.of() );
        engine.run();

        var clazz = "name_" + Engine.getHashName( "test${tst.test2.i}" );

        assertThat( test.resolve( "oap.template." + clazz + ".java" ) ).exists();
        assertThat( test.resolve( "oap.template." + clazz + ".class" ) ).exists();

        Files.setLastModifiedTime( test.resolve( "oap.template." + clazz + ".java" ), System.currentTimeMillis() - engine.ttl - 100 );
        Files.setLastModifiedTime( test.resolve( "oap.template." + clazz + ".class" ), System.currentTimeMillis() - engine.ttl - 100 );

        engine.run();

        assertThat( test.resolve( "oap.template." + clazz + ".java" ) ).doesNotExist();
        assertThat( test.resolve( "oap.template." + clazz + ".class" ) ).doesNotExist();
    }

    @Test
    public void loadFromDisk() {
        var test = ensureDirectory( TestDirectoryFixture.testPath( "test" ) );
        var engine = new Engine( test );
        var clazz = Engine.getHashName( "test" );
        var template = engine.getTemplate( clazz, EngineTest.Test1.class, "test${tst.test2.i}" );

        template.renderString( new EngineTest.Test1(), Map.of() );

        var engine2 = new Engine( test );

        var template2 = engine2.getTemplate( clazz, EngineTest.Test1.class, "test${tst.test2.i}" );
        template2.renderString( new EngineTest.Test1(), Map.of() );
    }

    @Test
    public void processWithoutVariables() {
        var test = ensureDirectory( TestDirectoryFixture.testPath( "test" ) );
        var engine = new Engine( test );
        assertThat( engine.getTemplate( "test", Container.class, "d" ) )
            .isExactlyInstanceOf( ConstTemplate.class );
        assertThat( engine.getTemplate( "test- s%;\\/:", Container.class, "d" )
            .renderString( new Container( new Tst() ), Map.of() ) ).isEqualTo( "d" );
    }

    @Test
    public void depth() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "a i/d" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp- s%;\\/:", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(0)}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a i/d" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a i/d" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(1)}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode( 1) }" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(2)}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a%2Bi%252Fd" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencodePercent() }" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=a%20i%2Fd" );//why?????
    }

    @Test
    public void testToUpperCase() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "a i/d" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "name1", Container.class, "${tst.test1.id ; toUpperCase()}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "A I/D" );
    }
    
    @Test
    public void otherJoinStrategy() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test4 = new Test4( 320, 50 );
        test.test4 = test4;

        var template = engine.getTemplate( "tmp", Container.class,
            Lists.of( new JavaCTemplate.Line( "WaH", "tst.test4.{a,\"xx\",b}", "" ) ), "œ", new JoinAsSingleTemplateStrategy() );

        var invAccumulator = new InvocationAccumulator();

        template.render( new Container( test ), Map.of(), invAccumulator );
        assertThat( invAccumulator.count ).isEqualTo( 1 );
        assertThat( invAccumulator.get() ).isEqualTo( "320xx50" );

    }

    @Test
    public void alternatives() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "aid" );
        test.test1n = new Test1( null, test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2n.test2.id | tst.test1n.test1.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=aid" );

        var test2 = new Test2( "sid" );
        test.test2n = new Test2( null, test2 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2n.test2.id | tst.test1n.test1.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=sid" );
    }

//    @Test
//    public void testNullableAlternatives() {
//        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );
//
//        var test = new Tst();
//        test.test1n = new Test1( null );
//        test.test2n = new Test2( "v" );
//        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1n.id | tst.test2n.id}" )
//            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=v" );
//    }

    @Test
    public void alternatives2() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( null );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1.id | tst.test2.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=" );
    }

    @Test
    public void override() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var override = new HashMap<String, String>();
        override.put( "tst.test2.id", "tst.test1.id" );
        override.put( "PRICE", "1.2.3" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id}-${PRICE}", override )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=id1-" );
    }

    @Test
    public void mapper() {
        var templatePath = TestDirectoryFixture.testPath( "test" );
        var engine = new Engine( ensureDirectory( templatePath ) );

        var test = new Tst();
        var test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "my.var", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${my.var}" )
            .renderString( new Container( test ), mapper ) ).isEqualTo( "id=new value" );

        mapper.put( "my.var", () -> "new value 2" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${my.var}" )
            .renderString( new Container( test ), mapper ) ).isEqualTo( "id=new value 2" );
    }

    @Test
    public void mapperWithUrlEncode() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "my.var", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${my.var; urlencode(1)}" )
            .renderString( new Container( test ), mapper ) ).isEqualTo( "id=new+value" );
    }

    @Test
    public void doubleValue() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=" );

        Test3 test3 = new Test3( 10.0 );
        test.test3 = Optional.of( test3 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=10.0" );
    }

    @Test
    public void escape() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=$${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=${tst.test2.id | tst.test1.id}" );

        assertThat( engine.getTemplate( "tmp1", Container.class, "\"';\\n\\t\\r" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "\"';\\n\\t\\r" );
    }

    @Test
    public void invalidPath() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var test = new Tst();
        var test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tstNotFound}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2NotFound.id}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1.idNotFound}" )
            .renderString( new Container( test ), Map.of() ) ).isEqualTo( "id=" );

    }

    @Test
    public void testExt() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var obj = new TestTemplateBean();
        obj.ext = new TestTemplateBeanExt( "v1" );

        assertString( engine.getTemplate( "txt", TestTemplateBean.class, "-${ext.value}-" ).renderString( obj, Map.of() ) )
            .isEqualTo( "-v1-" );
    }

    @Test
    public void map() {
        var engine = new Engine( ensureDirectory( TestDirectoryFixture.testPath( "test" ) ) );

        var map = Maps.of2( "a", 1, "b", "test", "c (1)", 0.0 );

        assertThat( engine.getTemplate( "tmp", Map.class, "id=${a},id2=${b},id3=${c ((1)}" )
            .renderString( map, Map.of() ) ).isEqualTo( "id=1,id2=test,id3=0.0" );
    }

    private static class InvocationAccumulator extends StringAccumulator {
        public int count = 0;

        public InvocationAccumulator() {
            super( new StringBuilder() );
        }

        @Override
        public Accumulator<String> accept( String o ) {
            count++;
            return super.accept( o );
        }

        @Override
        public Accumulator<String> accept( int o ) {
            count++;
            return super.accept( o );
        }
    }

    @AllArgsConstructor
    public static class Container {
        public Tst tst;
    }

    public static class Tst {
        @Template.Nullable
        public Test1 test1n = null;
        @Template.Nullable
        public Test2 test2n = null;

        public Optional<Test1> test1 = Optional.empty();
        public Optional<Test2> test2 = Optional.empty();
        public Optional<Test3> test3 = Optional.empty();
        @Template.Nullable
        public Test4 test4 = null;

        public static class Test1 {
            public String id;
            public Test1 test1;

            public Test1( String id ) {
                this.id = id;
            }

            public Test1( String id, Test1 test1 ) {
                this.id = id;
                this.test1 = test1;
            }
        }

        public static class Test2 {
            public String id;
            public Test2 test2;

            public Test2( String id ) {
                this.id = id;
            }

            public Test2( String id, Test2 test2 ) {
                this.id = id;
                this.test2 = test2;
            }
        }

        @AllArgsConstructor
        public static class Test3 {
            public double dval;
        }

        @AllArgsConstructor
        public static class Test4 {
            public int a;
            public int b;
        }
    }
}
