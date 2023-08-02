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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.template.ErrorStrategy.ERROR;
import static oap.template.ErrorStrategy.IGNORE;
import static oap.template.TemplateAccumulators.OBJECT;
import static oap.template.TemplateAccumulators.STRING;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineTest extends Fixtures {
    private TemplateEngine engine;
    private String testMethodName;

    @BeforeMethod
    public void beforeCMethod() {
        engine = new TemplateEngine();
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testRenderStringText() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "sdkjf hdkfgj d$...{}", STRING, null ).render( null ).get() )
            .isEqualTo( "sdkjf hdkfgj d$...{}" );
    }

    @Test
    public void testWithoutDefaultValue() {
        var c = new TestTemplateClass();
        c.childNullable = new TestTemplateClass();
        c.childNullable.longField = 0;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.longField}", OBJECT, null ).render( c ).get() )
            .isEqualTo( 0L );
    }

    @Test
    public void testEscapeVariables() {
        var c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}\t${field}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "12\t12" );
    }

    @Test
    public void testEscapeExpression() {
        var c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "$${field}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "${field}" );
    }

    @Test
    public void testMapProperty() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${prop}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testEscapeClassName() {
        assertThat( engine.getTemplate( "111-" + testMethodName + "-? ()?пп1", new TypeRef<Map<String, String>>() {}, "${prop}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testField() {
        var c = new TestTemplateClass();
        c.field = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonProperty() {
        var c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTest}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestNew}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonAlias() {
        var c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTest}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestAlias1}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestAlias2}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testEnumField() {
        var c = new TestTemplateClass();
        c.enumField = TestTemplateEnum.VAL1;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField}", STRING, null ).render( c ).get() )
            .isEqualTo( "VAL1" );
    }

    @Test
    public void testListField() {
        var c = new TestTemplateClass();
        c.list = List.of( 1, 2, 3 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list}", STRING, null ).render( c ).get() )
            .isEqualTo( "[1,2,3]" );
    }

    @Test
    public void testListFieldWithoutDefault() {
        var c = new TestTemplateClass();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list}", STRING, null ).render( c ).get() )
            .isEqualTo( "[]" );
    }

    @Test
    public void testListString() {
        var c = new TestTemplateClass();
        c.listString = List.of( "1", "'", "\\" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${listString}", STRING, null ).render( c ).get() )
            .isEqualTo( "['1','\\'','\\\\']" );
    }

    @Test
    public void testListEnum() {
        var c = new TestTemplateClass();
        c.listEnum = List.of( TestTemplateEnum.VAL2, TestTemplateEnum.VAL1 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${listEnum}", STRING, null ).render( c ).get() )
            .isEqualTo( "['VAL2','VAL1']" );
    }

    @Test
    public void testListFieldDefaultValue() {
        var c = new TestTemplateClass();
        c.list = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list??[]}", STRING, null ).render( c ).get() )
            .isEqualTo( "[]" );
    }

    @Test
    public void testChain() {
        var c1 = new TestTemplateClass();
        var c2 = new TestTemplateClass();
        c1.child = c2;
        c2.field = "val3";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child.field}", STRING, null ).render( c1 ).get() )
            .isEqualTo( "val3" );
    }

    @Test
    public void testOrEmptyString() {
        var c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field | field2}", STRING, null ).render( c ).get() )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOrCollections() {
        var c = new TestTemplateClass();
        c.list2 = List.of( 2, 3 );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list | list2}", STRING, null ).render( c ).get() )
            .isEqualTo( "[2,3]" );
    }

    @Test
    public void testOptional() {
        var c = new TestTemplateClass();

        c.fieldOpt = Optional.of( "o" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING, null ).render( c ).get() )
            .isEqualTo( "o" );

        c.fieldOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testChildOptional() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );
        c.intField = 10;

        cp.childOpt = Optional.of( c );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}-${childOpt.intField}", STRING, Map.of(), IGNORE ).render( cp ).get() )
            .isEqualTo( "o-10" );

        cp.childOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}", STRING, null ).render( cp ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testNullable() {
        var c = new TestTemplateClass();

        c.fieldNullable = "o";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING, null ).render( c ).get() )
            .isEqualTo( "o" );

        c.fieldNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testChildNullable() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldNullable = "o";

        cp.childNullable = c;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable??''}", STRING, null ).render( cp ).get() )
            .isEqualTo( "o" );

        cp.childNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable??''}", STRING, null ).render( cp ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testDefaultString() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${bbb??'test'}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "test" );
    }

    @Test
    public void testDefaultInt() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Integer>>() {}, "${bbb??-1}", STRING, null ).render( Map.of( "prop", 1 ) ).get() )
            .isEqualTo( "-1" );
    }

    @Test
    public void testDefaultBoolean() {
        var c = new TestTemplateClass();
        c.childNullable = null;
        c.childOpt = Optional.empty();

        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${childNullable.booleanObjectField??true}", STRING, null ).render( c ).get() )
            .isEqualTo( "true" );
        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${childOpt.booleanObjectField??true}", STRING, Map.of(), IGNORE ).render( c ).get() )
            .isEqualTo( "true" );
    }

    @Test
    public void testDefaultDouble() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "${bbb??0.0}", STRING, null ).render( Map.of( "prop", 1.1 ) ).get() )
            .isEqualTo( "0.0" );
    }

    @Test
    public void testMix() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "-${prop}-${b}-", STRING, null ).render( Map.of( "prop", "val", "b", "b1" ) ).get() )
            .isEqualTo( "-val-b1-" );
    }

    @Test
    public void testAliases() {
        var c1 = new TestTemplateClass();
        var c2 = new TestTemplateClass();
        var c3 = new TestTemplateClass2();
        c1.child = c2;
        c1.child2 = c3;

        c2.field = "val3";
        c3.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child.field}", STRING,
            Map.of( "child.field", "child2.field2" ), ERROR ).render( c1 ).get() )
            .isEqualTo( "f2" );
    }

    @Test
    public void testErrorSyntax() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testExt() {
        var c = new TestTemplateClass();
        c.ext2 = new TestTemplateClassExt( "ev" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${ext.a|ext2.a}", STRING, null ).render( c ).get() )
            .isEqualTo( "ev" );
    }

    @Test
    public void testConcatenation() {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenation() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        c.child = c1;
        c1.field = "f1";
        c1.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child{field,\"x\",field2}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass2();
        c.child2 = c1;
        c1.field2 = "f1";
        c1.field22 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child2.{field2,\"x\",field22}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testSum() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intField + 12.45}", STRING, null ).render( c ).get() )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testSumDefault() {
        var c = new TestTemplateClass();

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intObjectField + 12.45 ?? 5}", STRING, null ).render( c ).get() )
            .isEqualTo( "5" );
    }

    @Test
    public void testComment() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${/* intField */intField}", STRING, null ).render( c ).get() )
            .isEqualTo( "123" );
    }

    @Test
    public void testErrorSyntaxMsg() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${unknownField.unknownField}", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class )
            .hasMessageContaining( "unknownField.unknownField" );
    }

    @Test
    public void testPrimitiveAsObject() {
        var templateAccumulator = new TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();
        templateClass.booleanField = true;
        templateClass.booleanObjectField = true;
        templateClass.intField = 1;
        templateClass.intObjectField = 2;

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:${booleanField},booleanObjectField:${booleanObjectField},intField:${intField},intObjectField:${intObjectField}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();


        assertString( str ).isEqualTo( "booleanField:true_b,booleanObjectField:true_b,intField:1_i,intObjectField:2_i" );
    }

    @SuppressWarnings( "checkstyle:NoWhitespaceBefore" )
    @Test
    public void testPrimitiveAsObjectDefaultValue() {
        var templateAccumulator = new TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:${booleanField??true},booleanObjectField:${booleanObjectField??true}"
                + ",byteField:${byteField??1},byteObjectField:${byteObjectField??1}"
                + ",shortField:${shortField??1},shortObjectField:${shortObjectField??1}"
                + ",intField:${intField??1},intObjectField:${intObjectField??1}"
                + ",longField:${longField??1},longObjectField:${longObjectField??1}"
                + ",floatField:${floatField??1},floatObjectField:${floatObjectField??1}"
                + ",doubleField:${doubleField??1},doubleObjectField:${doubleObjectField??1}"
            , templateAccumulator, ERROR, null ).render( templateClass ).get();


        assertString( str ).isEqualTo( "booleanField:false_b,booleanObjectField:true_b"
            + ",byteField:0,byteObjectField:1"
            + ",shortField:0,shortObjectField:1"
            + ",intField:0_i,intObjectField:1_i"
            + ",longField:0,longObjectField:1"
            + ",floatField:0.0,floatObjectField:1.0"
            + ",doubleField:0.0,doubleObjectField:1.0" );
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

    public static class TestPrimitiveTemplateAccumulatorString extends TemplateAccumulatorString {
        @Override
        public void accept( boolean b ) {
            super.accept( b + "_b" );
        }

        @Override
        public void accept( int i ) {
            super.accept( i + "_i" );
        }

        @Override
        public TemplateAccumulatorString newInstance() {
            return new TestPrimitiveTemplateAccumulatorString();
        }
    }
}
