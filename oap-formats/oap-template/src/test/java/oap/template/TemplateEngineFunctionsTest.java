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

import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.util.Dates;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static oap.template.TemplateAccumulators.OBJECT;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineFunctionsTest extends Fixtures {

    private TemplateEngine engine;
    private String testMethodName;

    @BeforeClass
    public void beforeClass() {
        engine = new TemplateEngine( Dates.d( 10 ) );
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testMethod() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val2";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldM()}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2" );
    }

    @Test
    public void testMethodDefault() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldM()??'d'}", STRING, null ).render( c ).get() )
            .isEqualTo( "d" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childM().field??'d'}", STRING, null ).render( c ).get() )
            .isEqualTo( "d" );
    }

    @Test
    public void testMethodWithIntParameter() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val2";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldMInt(1  )}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2-1" );
    }

    @Test
    public void testMethodWithNegativeIntParameter() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val2";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldMInt( -1)}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2--1" );
    }

    @Test
    public void testMethodWithFloatParameter() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val2";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldMDouble(1.2  )}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2-1.2" );
    }

    @Test
    public void testMethodWithStringParameter() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val2";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldMString( 'str')}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2-str" );
    }

    @Test
    public void testFunctionUrlencode() {
        assertThat( engine.getTemplate( "testFunctionUrlencode0", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode(0)}", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=a i/d" );
        assertThat( engine.getTemplate( "testFunctionUrlencode1", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode( 1)}", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "testFunctionUrlencode", new TypeRef<Map<String, String>>() {}, "id=${ v ; urlencode() }", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=a+i%2Fd" );
        assertThat( engine.getTemplate( "testFunctionUrlencode2", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode ( 2 )}", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=a%2Bi%252Fd" );
        assertThat( engine.getTemplate( "testFunctionUrlencode", new TypeRef<Map<String, String>>() {}, "id=${ v ; urlencodePercent() }", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=a%20i%2Fd" );
    }

    @Test
    public void testFunctionUrlencodeCollection() {
        assertThat( engine.getTemplate( "testFunctionUrlencodeList", new TypeRef<Map<String, List<String>>>() {}, "id=${v; urlencode()}", STRING, null ).render( Map.of( "v", List.of( "a i/d", "X" ) ) ).get() )
            .isEqualTo( "id=%5B%27a+i%2Fd%27%2C%27X%27%5D" );
    }

    @Test
    public void testFunctionToUpperCase() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()}", STRING, null ).render( Map.of( "v", "a i/d" ) ).get() )
            .isEqualTo( "id=A I/D" );
    }

    @Test
    public void testTypes() {
        Map<String, Object> map = Map.of( "v", Map.of( "d", 1.1d, "s1", Map.of( "s2", "str" ) ) );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${v.d ?? 0.0}", OBJECT, null ).render( map ).get() )
            .isEqualTo( 1.1d );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${v.c ?? 0.1}", OBJECT, null ).render( map ).get() )
            .isEqualTo( 0.1d );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${v.d ?? 0.0}", STRING, null ).render( map ).get() )
            .isEqualTo( "1.1" );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${<java.lang.Double>v.d ?? 0.0}", OBJECT, null ).render( map ).get() )
            .isEqualTo( 1.1d );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${<java.lang.Double>v.c ?? 0.1}", OBJECT, null ).render( map ).get() )
            .isEqualTo( 0.1d );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${<java.lang.Double>v.d ?? 0.0}", STRING, null ).render( map ).get() )
            .isEqualTo( "1.1" );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Object>>() {}, "${<java.lang.String>v.s1.s2 ?? ''}", STRING, null ).render( map ).get() )
            .isEqualTo( "str" );
    }

    @Test
    public void testAlias() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 10;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ intField ; testInc() }}", STRING, null ).render( c ).get() )
            .isEqualTo( "11" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ intField ; testIncAlias() }}", STRING, null ).render( c ).get() )
            .isEqualTo( "11" );
    }
}
