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

public class TemplateEngineTrimTest extends AbstractTemplateEngineTest {
    @Test
    public void testTrimLeft() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "aa {{- field }}", STRING, null ).render( c ).get() )
            .isEqualTo( "aatest" );
    }

    @Test
    public void testTrimRight() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ field -}} bb", STRING, null ).render( c ).get() )
            .isEqualTo( "testbb" );
    }

    @Test
    public void testTrimBoth() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "aa {{- field -}} \nbb", STRING, null ).render( c ).get() )
            .isEqualTo( "aatestbb" );
    }

    @Test
    public void testTrimMultipleSpaces() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "aa   \t{{- field -}}\t\n  bb", STRING, null ).render( c ).get() )
            .isEqualTo( "aatestbb" );
    }

    @Test
    public void testTrimNoAdjacentText() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{- field -}}", STRING, null ).render( c ).get() )
            .isEqualTo( "test" );
    }

    @Test
    public void testBlockIfLTrim() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "world";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "aa\n{{%- if booleanField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "aaworld" );
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
