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
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 2020-07-13.
 */
public class TemplateEngineTest extends Fixtures {

    private TemplateEngine engine;
    private String testMethodName;

    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @BeforeClass
    public void beforeClass() {
        engine = new TemplateEngine( TestDirectoryFixture.testDirectory() );
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testRenderStringText() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "sdkjf hdkfgj d$...{}", STRING ).render( null ) )
            .isEqualTo( "sdkjf hdkfgj d$...{}" );
    }

    @Test
    public void testEscapeVariables() {
        var c = new TestTemplateClass();
        c.field = "1";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}\t${field}", new TestTemplateAccumulatorString() )
            .render( c ) ).isEqualTo( "12\t12" );
    }

    @Test
    public void testMapProperty() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${prop}", STRING ).render( Map.of( "prop", "val" ) ) )
            .isEqualTo( "val" );
    }

    @Test
    public void testField() {
        var c = new TestTemplateClass();
        c.field = "val1";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}", STRING ).render( c ) )
            .isEqualTo( "val1" );
    }

    @Test
    public void testEnumField() {
        var c = new TestTemplateClass();
        c.enumField = TestTemplateEnum.VAL1;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField}", STRING ).render( c ) )
            .isEqualTo( "VAL1" );
    }

    @Test
    public void testListField() {
        var c = new TestTemplateClass();
        c.list = List.of( 1, 2, 3 );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list}", STRING ).render( c ) )
            .isEqualTo( "[1,2,3]" );
    }

    @Test
    public void testMethod() {
        var c = new TestTemplateClass();
        c.field = "val2";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldM()}", STRING ).render( c ) )
            .isEqualTo( "val2" );
    }

    @Test
    public void testChain() {
        var c1 = new TestTemplateClass();
        var c2 = new TestTemplateClass();
        c1.child = c2;
        c2.field = "val3";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child.field}", STRING ).render( c1 ) )
            .isEqualTo( "val3" );
    }

    @Test
    public void testOrEmptyString() {
        var c = new TestTemplateClass();
        c.field2 = "f2";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field | field2}", STRING ).render( c ) )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOrCollections() {
        var c = new TestTemplateClass();
        c.list2 = List.of( 2, 3 );

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list | list2}", STRING ).render( c ) )
            .isEqualTo( "[2,3]" );
    }

    @Test
    public void testOptional() {
        var c = new TestTemplateClass();

        c.fieldOpt = Optional.of( "o" );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING ).render( c ) )
            .isEqualTo( "o" );

        c.fieldOpt = Optional.empty();
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING ).render( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildOptional() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );

        cp.childOpt = Optional.of( c );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}", STRING ).render( cp ) )
            .isEqualTo( "o" );

        cp.childOpt = Optional.empty();
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}", STRING ).render( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testNullable() {
        var c = new TestTemplateClass();

        c.fieldNullable = "o";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING ).render( c ) )
            .isEqualTo( "o" );

        c.fieldNullable = null;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING ).render( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildNullable() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldNullable = "o";

        cp.childNullable = c;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}", STRING ).render( cp ) )
            .isEqualTo( "o" );

        cp.childNullable = null;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}", STRING ).render( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testDefaultString() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${bbb??'test'}", STRING ).render( Map.of( "prop", "val" ) ) )
            .isEqualTo( "test" );
    }

    @Test
    public void testDefaultInt() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, Integer>>() {}, "${bbb??-1}", STRING ).render( Map.of( "prop", 1 ) ) )
            .isEqualTo( "-1" );
    }

    @Test
    public void testDefaultBoolean() {
        var c = new TestTemplateClass();
        c.booleanField = true;
        assertString( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${booleanField??false}", STRING ).render( c ) )
            .isEqualTo( "true" );
    }

    @Test
    public void testDefaultDouble() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "${bbb??0.0}", STRING ).render( Map.of( "prop", 1.1 ) ) )
            .isEqualTo( "0.0" );
    }

    @Test
    public void testMix() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "-${prop}-${b}-", STRING ).render( Map.of( "prop", "val", "b", "b1" ) ) )
            .isEqualTo( "-val-b1-" );
    }

    @Test
    public void testFunctionUrlencode() {
        assertString( engine.getTemplate( "testFunctionUrlencode0", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode(0)}", STRING ).render( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a i/d" );
        assertString( engine.getTemplate( "testFunctionUrlencode1", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode( 1)}", STRING ).render( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a+i%2Fd" );
        assertString( engine.getTemplate( "testFunctionUrlencode", new TypeRef<Map<String, String>>() {}, "id=${ v ; urlencode() }", STRING ).render( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a+i%2Fd" );
        assertString( engine.getTemplate( "testFunctionUrlencode2", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode ( 2 )}", STRING ).render( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a%2Bi%252Fd" );
    }

    @Test
    public void testFunctionToUpperCase() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()}", STRING ).render( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=A I/D" );
    }

    @Test
    public void testErrorSyntax() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()", STRING, ERROR ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testExt() {
        var c = new TestTemplateClass();
        c.ext2 = new TestTemplateClassExt( "ev" );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${ext.a|ext2.a}", STRING ).render( c ) )
            .isEqualTo( "ev" );
    }

    @Test
    public void testConcatenation() {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", STRING ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenation() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        c.child = c1;
        c1.field = "f1";
        c1.field2 = "f2";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child{field,\"x\",field2}}", STRING ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass2();
        c.child_2 = c1;
        c1.field2 = "f1";
        c1.field22 = "f2";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child_2.{field2,\"x\",field22}}", STRING ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testSum() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intField + 12.45}", STRING ).render( c ) )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testSumDefault() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intField + 12.45 ?? 5}", STRING ).render( c ) )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testComment() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${/* intField */intField}", STRING ).render( c ) )
            .isEqualTo( "123" );
    }

    @Test
    public void testErrorSyntaxMsg() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${unknownField.unknownField}", STRING, ERROR ) )
            .isInstanceOf( TemplateException.class )
            .hasMessageContaining( "unknownField.unknownField" );
    }

    public static class TestTemplateAccumulatorString extends TemplateAccumulatorString {
        @Override
        public void accept( String text ) {
            super.accept( text + "2" );
        }

        @Override
        public TemplateAccumulatorString newInstance() {
            return new TestTemplateAccumulatorString();
        }
    }
}
