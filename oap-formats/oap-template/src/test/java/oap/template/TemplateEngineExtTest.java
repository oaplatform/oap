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

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineExtTest extends AbstractTemplateEngineTest {
    @Test
    public void testExt() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext = new TestTemplateClassExt();
        ( ( TestTemplateClassExt ) c.ext ).a = "aaa";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ ext.a }}", STRING, null ).render( c ).get() ).isEqualTo( "aaa" );
    }

    @Test
    public void testExtConcatenation() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "f";
        c.ext = new TestTemplateClassExt();
        ( ( TestTemplateClassExt ) c.ext ).a = "aaa";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field + ext.a + 1 }}", STRING, null ).render( c ).get() ).isEqualTo( "faaa1" );
    }

    @Test
    public void testExtIfInline() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext = new TestTemplateClassExt();
        ( ( TestTemplateClassExt ) c.ext ).a = "aaa";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if ext.a then ext.a end }}", STRING, null ).render( c ).get() ).isEqualTo( "aaa" );
    }

    @Test
    public void testExtIfBlock() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext = new TestTemplateClassExt();
        ( ( TestTemplateClassExt ) c.ext ).a = "aaa";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{% if ext.a }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() ).isEqualTo( "yes" );
    }

    @Test
    public void testExtWith() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext = new TestTemplateClassExt();
        ( ( TestTemplateClassExt ) c.ext ).a = "aaa";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{% with ext }}{{ a }}{{% end }}", STRING, null ).render( c ).get() ).isEqualTo( "aaa" );
    }
}
