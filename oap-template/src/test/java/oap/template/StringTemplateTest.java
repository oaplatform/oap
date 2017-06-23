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
import lombok.val;
import oap.io.Files;
import oap.template.StringTemplateTest.Tst.Test1;
import oap.template.StringTemplateTest.Tst.Test2;
import oap.template.StringTemplateTest.Tst.Test3;
import oap.template.StringTemplateTest.Tst.Test4;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 15.06.2017.
 */
public class StringTemplateTest extends AbstractTest {
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
    public void testDepth() {
        Tst test = new Tst();
        Test1 test1 = new Test1( "a i/d" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id | tst.test1.id ; urlencode(0)}" )
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
    public void testOtherJoinStrategy() {
        Tst test = new Tst();
        Test4 test4 = new Test4( 320, 50 );
        test.test4 = Optional.of( test4 );

        val template = engine.getTemplate( "tmp", Container.class,
            Lists.of( new Template.Line( "WaH", "tst.test4.{a,\"xx\",b}", "" ) ), "œ", new JoinAsSingleTemplateStrategy() );

        InvocationAccumulator invAccumulator = new InvocationAccumulator();

        template.render( new Container( test ), invAccumulator );
        assertThat( invAccumulator.get() ).isEqualTo( 1 );
        assertThat( invAccumulator.build() ).isEqualTo( "320xx50" );

    }

    @Test
    public void testAlternatives() {
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
    public void testAlternatives2() {
        Tst test = new Tst();
        Test1 test1 = new Test1( null );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test1.id | tst.test2.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );
    }

    @Test
    public void testOverride() {
        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        val override = new HashMap<String, String>();
        override.put( "tst.test2.id", "tst.test1.id" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id}", override, emptyMap() )
            .renderString( new Container( test ) ) ).isEqualTo( "id=id1" );
    }

    @Test
    public void testMapper() {
        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        val mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "tst.test2.id", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id}", emptyMap(), mapper )
            .renderString( new Container( test ) ) ).isEqualTo( "id=new value" );
    }

    @Test
    public void testMapperWithUrlEncode() {
        Tst test = new Tst();
        Test1 test1 = new Test1( "id1" );
        test.test1 = Optional.of( test1 );

        val mapper = new HashMap<String, Supplier<String>>();
        mapper.put( "tst.test2.id", () -> "new value" );

        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test2.id ; urlencode(1)}", emptyMap(), mapper )
            .renderString( new Container( test ) ) ).isEqualTo( "id=new+value" );
    }

    @Test
    public void testDoubleValue() {
        Tst test = new Tst();
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=" );

        Test3 test3 = new Test3( 10.0 );
        test.test3 = Optional.of( test3 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=${tst.test3.dval}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=10.0" );
    }

    @Test
    public void testEscape() {
        Tst test = new Tst();
        Test1 test1 = new Test1( "aid" );
        test.test1 = Optional.of( test1 );
        assertThat( engine.getTemplate( "tmp", Container.class, "id=$${tst.test2.id | tst.test1.id}" )
            .renderString( new Container( test ) ) ).isEqualTo( "id=${tst.test2.id | tst.test1.id}" );

        assertThat( engine.getTemplate( "tmp1", Container.class, "\"';\\n\\t\\r" )
            .renderString( new Container( test ) ) ).isEqualTo( "\"';\\n\\t\\r" );
    }

    private static class InvocationAccumulator extends StringAccumulator {
        int invs = 0;

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
