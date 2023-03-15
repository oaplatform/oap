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
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineTest extends Fixtures {
    private TemplateEngine engine;
    private String testMethodName;

    public TemplateEngineTest() {
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
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "sdkjf hdkfgj d$...{}", STRING, null ).render( null ) )
            .isEqualTo( "sdkjf hdkfgj d$...{}" );
    }

    @Test
    public void testEscapeVariables() {
        var c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}\t${field}", new TestTemplateAccumulatorString(), null )
            .render( c ) ).isEqualTo( "12\t12" );
    }

    @Test
    public void testEscapeExpression() {
        var c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "$${field}", new TestTemplateAccumulatorString(), null )
            .render( c ) ).isEqualTo( "${field}" );
    }

    @Test
    public void testMapProperty() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${prop}", STRING, null ).render( Map.of( "prop", "val" ) ) )
            .isEqualTo( "val" );
    }

    @Test
    public void testEscapeClassName() {
        assertThat( engine.getTemplate( "111-" + testMethodName + "-? ()?пп1", new TypeRef<Map<String, String>>() {}, "${prop}", STRING, null ).render( Map.of( "prop", "val" ) ) )
            .isEqualTo( "val" );
    }

    @Test
    public void testField() {
        var c = new TestTemplateClass();
        c.field = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonProperty() {
        var c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTest}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestNew}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonAlias() {
        var c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTest}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestAlias1}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${jsonTestAlias2}", STRING, null ).render( c ) )
            .isEqualTo( "val1" );
    }

    @Test
    public void testEnumField() {
        var c = new TestTemplateClass();
        c.enumField = TestTemplateEnum.VAL1;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField}", STRING, null ).render( c ) )
            .isEqualTo( "VAL1" );
    }

    @Test
    public void testListField() {
        var c = new TestTemplateClass();
        c.list = List.of( 1, 2, 3 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list}", STRING, null ).render( c ) )
            .isEqualTo( "[1,2,3]" );
    }

    @Test
    public void testListString() {
        var c = new TestTemplateClass();
        c.listString = List.of( "1", "'", "\\" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${listString}", STRING, null ).render( c ) )
            .isEqualTo( "['1','\\'','\\\\']" );
    }

    @Test
    public void testListEnum() {
        var c = new TestTemplateClass();
        c.listEnum = List.of( TestTemplateEnum.VAL2, TestTemplateEnum.VAL1 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${listEnum}", STRING, null ).render( c ) )
            .isEqualTo( "['VAL2','VAL1']" );
    }

    @Test
    public void testListFieldDefaultValue() {
        var c = new TestTemplateClass();
        c.list = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list??[]}", STRING, null ).render( c ) )
            .isEqualTo( "[]" );
    }

    @Test
    public void testChain() {
        var c1 = new TestTemplateClass();
        var c2 = new TestTemplateClass();
        c1.child = c2;
        c2.field = "val3";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child.field}", STRING, null ).render( c1 ) )
            .isEqualTo( "val3" );
    }

    @Test
    public void testOrEmptyString() {
        var c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field | field2}", STRING, null ).render( c ) )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOrCollections() {
        var c = new TestTemplateClass();
        c.list2 = List.of( 2, 3 );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${list | list2}", STRING, null ).render( c ) )
            .isEqualTo( "[2,3]" );
    }

    @Test
    public void testOptional() {
        var c = new TestTemplateClass();

        c.fieldOpt = Optional.of( "o" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING, null ).render( c ) )
            .isEqualTo( "o" );

        c.fieldOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}", STRING, null ).render( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildOptional() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );
        c.intField = 10;

        cp.childOpt = Optional.of( c );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}-${childOpt.intField}", STRING, null ).render( cp ) )
            .isEqualTo( "o-10" );

        cp.childOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}", STRING, null ).render( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testNullable() {
        var c = new TestTemplateClass();

        c.fieldNullable = "o";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING, null ).render( c ) )
            .isEqualTo( "o" );

        c.fieldNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}", STRING, null ).render( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildNullable() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldNullable = "o";

        cp.childNullable = c;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}", STRING, null ).render( cp ) )
            .isEqualTo( "o" );

        cp.childNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}", STRING, null ).render( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testDefaultString() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${bbb??'test'}", STRING, null ).render( Map.of( "prop", "val" ) ) )
            .isEqualTo( "test" );
    }

    @Test
    public void testDefaultInt() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Integer>>() {}, "${bbb??-1}", STRING, null ).render( Map.of( "prop", 1 ) ) )
            .isEqualTo( "-1" );
    }

    @Test
    public void testDefaultBoolean() {
        var c = new TestTemplateClass();
        c.childNullable = null;
        c.childOpt = Optional.empty();

        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${childNullable.booleanObjectField??true}", STRING, null ).render( c ) )
            .isEqualTo( "true" );
        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${childOpt.booleanObjectField??true}", STRING, null ).render( c ) )
            .isEqualTo( "true" );
    }

    @Test
    public void testDefaultDouble() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "${bbb??0.0}", STRING, null ).render( Map.of( "prop", 1.1 ) ) )
            .isEqualTo( "0.0" );
    }

    @Test
    public void testMix() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "-${prop}-${b}-", STRING, null ).render( Map.of( "prop", "val", "b", "b1" ) ) )
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
            Map.of( "child.field", "child2.field2" ), null ).render( c1 ) )
            .isEqualTo( "f2" );
    }

    @Test
    public void testDiskCache() {
        var c1 = new TestTemplateClass();
        c1.field = "1";
        c1.field2 = "2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}", STRING, Map.of(), null ).render( c1 ) )
            .isEqualTo( "1" );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field2}", STRING, Map.of(), null ).render( c1 ) )
            .isEqualTo( "2" );

        var engine2 = new TemplateEngine( TestDirectoryFixture.testDirectory() );
        assertThat( engine2.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}", STRING, Map.of(), null ).render( c1 ) )
            .isEqualTo( "1" );
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
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${ext.a|ext2.a}", STRING, null ).render( c ) )
            .isEqualTo( "ev" );
    }

    @Test
    public void testConcatenation() {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", STRING, null ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenation() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        c.child = c1;
        c1.field = "f1";
        c1.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child{field,\"x\",field2}}", STRING, null ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass2();
        c.child2 = c1;
        c1.field2 = "f1";
        c1.field22 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child2.{field2,\"x\",field22}}", STRING, null ).render( c ) )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testSum() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intField + 12.45}", STRING, null ).render( c ) )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testSumDefault() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intField + 12.45 ?? 5}", STRING, null ).render( c ) )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testComment() {
        var c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${/* intField */intField}", STRING, null ).render( c ) )
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
            templateAccumulator, ERROR, null ).render( templateClass );


        assertString( str ).isEqualTo( "booleanField:true_b,booleanObjectField:true_b,intField:1_i,intObjectField:2_i" );
    }

    @Test
    public void testCacheClassFormatError() throws IOException {
        FileUtils.write( TestDirectoryFixture.testPath( "oap.template.testCacheClassFormatError.class" ).toFile(), "", UTF_8 );

        var c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}\t${field}", new TestTemplateAccumulatorString(), null )
            .render( c ) ).isEqualTo( "12\t12" );

        FileUtils.write( TestDirectoryFixture.testPath( "oap.template.testCacheClassFormatError.class" ).toFile(), "", UTF_8 );

        var engine2 = new TemplateEngine( TestDirectoryFixture.testDirectory() );

        assertThat( engine2.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}\t${field}", new TestTemplateAccumulatorString(), null )
            .render( c ) ).isEqualTo( "12\t12" );
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
