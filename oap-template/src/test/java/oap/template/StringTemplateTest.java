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
import oap.testng.TestDirectory;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static oap.io.Files.ensureDirectory;
import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;

public class StringTemplateTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    @Test
    public void ttl() {
        Path test = ensureDirectory( tmpPath( "test" ) );
        Engine engine = new Engine( test );
        String clazz = Engine.getName( "test" );
        var template = engine.getTemplate( clazz, EngineTest.Test1.class, "test${tst.test2.i}" );

        template.renderString( new EngineTest.Test1() );
        engine.run();

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
        Path test = ensureDirectory( tmpPath( "test" ) );
        Engine engine = new Engine( test );
        String clazz = Engine.getName( "test" );
        var template = engine.getTemplate( clazz, EngineTest.Test1.class, "test${tst.test2.i}" );

        template.renderString( new EngineTest.Test1() );

        Engine engine2 = new Engine( test );

        var template2 = engine2.getTemplate( clazz, EngineTest.Test1.class, "test${tst.test2.i}" );
        template2.renderString( new EngineTest.Test1() );
    }

    @Test
    public void processWithoutVariables() throws Exception {
        Path test = ensureDirectory( tmpPath( "test" ) );
        Engine engine = new Engine( test );
        assertThat( engine.getTemplate( "test", Container.class, "d" ) )
            .isExactlyInstanceOf( ConstTemplate.class );
        assertThat( engine.getTemplate( "test- s%;\\/:", Container.class, "d" )
            .renderString( new Container( new Tst() ) ) ).isEqualTo( "d" );
    }

    @Test
    public void depth() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "a i/d" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp- s%;\\/:", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(0)}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=a i/d" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=a i/d" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(1)}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode( 1) }" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(2)}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=a%2Bi%252Fd" );
    }

    @Test
    public void otherJoinStrategy() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test4 test4 = new Test4( 320, 50 );
        test.test4 = Optional.of( test4 );

        var template = engine.getTemplate( "tmp", Container.class,
            Lists.of( new JavaCTemplate.Line( "WaH", "tst.test4.{a,\"xx\",b}", "" ) ), "œ", new JoinAsSingleTemplateStrategy() );

        InvocationAccumulator invAccumulator = new InvocationAccumulator();

        template.render( new Container( test ), invAccumulator );
        assertThat( invAccumulator.get() ).isEqualTo( 1 );
        assertThat( invAccumulator.build() ).isEqualTo( "320xx50" );

    }

    @Test
    public void alternatives() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=aid" );

        Test2 test2 = new Test2( "sid" );
        test.test2 = Optional.of( test2 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=sid" );
    }

    @Test
    public void alternatives2() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( null );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1.id | tst.test2.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );
    }

    @Test
    public void override() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var override = new HashMap<String, String>();
        override.put( "tst.test2.id", "tst.test1.id" );
        override.put( "PRICE", "1.2.3" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id}-${PRICE}", override, emptyMap() )
            .renderString( new Container( test ) ) ).isEqualTo( "id=id1-" );
    }

    @Test
    public void mapper() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "tst.test2.id", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id}", emptyMap(), mapper )
            .renderString( new Container( test ) ) ).isEqualTo( "id=new value" );
    }

    @Test
    public void mapperWithUrlEncode() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        var mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "tst.test2.id", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id ; urlencode(1)}", emptyMap(), mapper )
            .renderString( new Container( test ) ) ).isEqualTo( "id=new+value" );
    }

    @Test
    public void doubleValue() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );

        Test3 test3 = new Test3( 10.0 );
        test.test3 = Optional.of( test3 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=10.0" );
    }

    @Test
    public void escape() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=$${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=${tst.test2.id | tst.test1.id}" );

        assertThat( engine.getTemplate( "tmp1", Container.class, "\"';\\n\\t\\r" )
            .renderString( new Container( test ) ) ).isEqualTo( "\"';\\n\\t\\r" );
    }

    @Test
    public void invalidPath() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        Tst test = new Tst();
        Test1 test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tstNotFound}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2NotFound.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1.idNotFound}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );

    }

    @Test
    public void map() {
        Engine engine = new Engine( ensureDirectory( tmpPath( "test" ) ) );

        var map = Maps.of2( "a", 1, "b", "test", "c (1)", 0.0 );

        assertThat( engine.getTemplate( "tmp", Map.class, "id=${a},id2=${b},id3=${c ((1)}" )
            .renderString( map ) ).isEqualTo( "id=1,id2=test,id3=0.0" );
    }

    private static class InvocationAccumulator extends StringAccumulator {
        int invs = 0;

        public InvocationAccumulator() {
            super( new StringBuilder() );
        }

        @Override
        public Accumulator accept( String o ) {
            invs++;
            return super.accept( o );
        }

        @Override
        public Accumulator accept( int o ) {
            invs++;
            return super.accept( o );
        }

        public int get() {
            return invs;
        }
    }

    @AllArgsConstructor
    public static class Container {
        public Tst tst;
    }

    public static class Tst {
        public Optional<Test1> test1 = Optional.empty();
        public Optional<Test2> test2 = Optional.empty();
        public Optional<Test3> test3 = Optional.empty();
        public Optional<Test4> test4 = Optional.empty();

        @AllArgsConstructor
        public static class Test1 {
            public String id;
        }

        @AllArgsConstructor
        public static class Test2 {
            public String id;
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
