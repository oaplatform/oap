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
import java.util.Optional;

import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineTypesTest extends Fixtures {
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
    public void testTypes() {
        var templateAccumulator = new TemplateEngineTest.TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();
        templateClass.booleanField = true;
        templateClass.booleanObjectField = true;
        templateClass.intField = 1;
        templateClass.intObjectField = 2;

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:${<java.lang.Boolean>booleanField},booleanObjectField:${<java.lang.Boolean>booleanObjectField},intField:${<java.lang.Integer>intField},intObjectField:${<java.lang.Integer>intObjectField}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();

        assertString( str ).isEqualTo( "booleanField:true_b,booleanObjectField:true_b,intField:1_i,intObjectField:2_i" );

        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:${<java.lang.Integer>booleanField}",
            templateAccumulator, ERROR, null ).render( templateClass ) )
            .isInstanceOf( TemplateException.class )
            .hasCauseInstanceOf( ClassCastException.class );
    }

    @Test
    public void testObjectReference() {
        var templateAccumulator = new TemplateEngineTest.TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();
        templateClass.child = new TestTemplateClass();
        templateClass.child.intField = 100;

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "child.intField:${<java.lang.Integer>child.intField}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();

        assertString( str ).isEqualTo( "child.intField:100_i" );
    }

    @Test
    public void testObjectReferenceWithConcatenation() {
        var templateAccumulator = new TemplateEngineTest.TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();
        templateClass.child = new TestTemplateClass();
        templateClass.child.child = new TestTemplateClass();
        templateClass.child.child.field = "v1";
        templateClass.child.child.field2 = "v2";

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "child.child.{field,\"x\",field2}:${<java.lang.String>child.child.{field,\"x\",field2}}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();

        assertString( str ).isEqualTo( "child.child.{field,\"x\",field2}:v1xv2" );
    }

    @Test
    public void testNullableObjectReference() {
        var templateAccumulator = new TemplateEngineTest.TestPrimitiveTemplateAccumulatorString();
        var templateClass = new TestTemplateClass();
        templateClass.childNullable = new TestTemplateClass();
        templateClass.childNullable.intField = 100;

        var str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "childNullable.intField:${<java.lang.Integer>childNullable.intField}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();

        assertThat( str ).isEqualTo( "childNullable.intField:100_i" );
    }

    @Test
    public void testDefaultBoolean() {
        var c = new TestTemplateClass();
        c.childNullable = null;
        c.childOpt = Optional.empty();

        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "${<java.lang.Boolean>childNullable.booleanObjectField??true}", STRING, null ).render( c ).get() )
            .isEqualTo( "true" );
    }
}
