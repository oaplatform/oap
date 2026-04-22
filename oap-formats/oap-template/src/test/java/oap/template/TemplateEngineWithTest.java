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

public class TemplateEngineWithTest extends AbstractTemplateEngineTest {
    @Test
    public void testInlineWithField() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ with (child) field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testInlineWithFallback() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = null;
        c.child.field2 = "fb";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ with (child) field | default field2 end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "fb" );
    }

    @Test
    public void testInlineWithNullScope() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = null;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ with (child) field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockWithBasic() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testBlockWithTextAroundExpr() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}A{{ field }}B{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "AvalB" );
    }

    @Test
    public void testBlockWithNullScopeSkipsBody() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = null;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockWithMultipleFields() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "f1";
        c.child.field2 = "f2";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ field }}-{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1-f2" );
    }

    @Test
    public void testBlockWithRootScope() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "root-val";
        c.child = new TestTemplateClass();
        c.child.field = "scoped-val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ $.field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "root-val" );
    }

    @Test
    public void testInlineWithRootScopeFallback() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "root-val";
        c.child = new TestTemplateClass();
        c.child.field = null;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ with (child) field | default $.field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "root-val" );
    }

    @Test
    public void testBlockWithNullScopeUsesDefault() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = null;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}A{{ field2 ?? 'a' }}B{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "AaB" );
    }

    @Test
    public void testBlockWithUnknownFieldThrows() {
        assertThatThrownBy( () ->
            getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
                "{{% with unknownField }}x{{% end }}", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class );
    }
}
