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
import org.testng.annotations.Test;

import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineConditionTest extends AbstractTemplateEngineTest {
    @Test
    public void testIfConditionTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testIfConditionFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = false;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfElseConditionFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.field2 = "val2";
        c.booleanField = false;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field else field2 end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2" );
    }

    @Test
    public void testIfConditionNullableObject() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";

        c.booleanObjectField = true;
        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "{{ if booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );

        c.booleanObjectField = null;
        assertThat( engine.getTemplate( testMethodName + "Null", new TypeRef<TestTemplateClass>() {}, "{{ if booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfConditionWithText() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "prefix-{{ if booleanField then field end }}-suffix", STRING, null ).render( c ).get() )
            .isEqualTo( "prefix-val-suffix" );
    }

    @Test
    public void testIfConditionWithDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end ?? 'default' }}", STRING, null ).render( c ).get() )
            .isEqualTo( "default" );
    }

    @Test
    public void testBlockIfTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "hello";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "hello" );
    }

    @Test
    public void testBlockIfFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.field = "hello";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockIfElseTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "then-val";
        c.field2 = "else-val";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% else }}{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "then-val" );
    }

    @Test
    public void testBlockIfElseFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.field = "then-val";
        c.field2 = "else-val";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% else }}{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "else-val" );
    }

    @Test
    public void testBlockIfNullableBoolTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanObjectField = true;
        c.field = "yes";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanObjectField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfNullableBoolNull() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanObjectField = null;
        c.field = "yes";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanObjectField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockIfWithSurroundingText() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "world";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "Hello {{% if booleanField }}{{ field }}{{% end }} done", STRING, null ).render( c ).get() )
            .isEqualTo( "Hello world done" );
    }

    @Test
    public void testBlockIfNestedPath() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.booleanField = true;
        c.child.field = "nested";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if child.booleanField }}{{ child.field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "nested" );
    }

    @Test
    public void testBlockIfMultiLineBody() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "myval";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}\n  value={{ field }}\n{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "\n  value=myval\n" );
    }

    @Test
    public void testBlockIfNested() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.child = new TestTemplateClass();
        c.child.booleanField = true;
        c.child.field = "deep";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField %}}{{% if child.booleanField }}{{ child.field }}{{% end }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "deep" );
    }

    @Test
    public void testBlockIfUnknownFieldThrows() {
        assertThatThrownBy( () ->
            engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
                "{{% if unknownField }}x{{% end }}", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class );
    }

}
