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

public class TemplateEngineMathTest extends AbstractTemplateEngineTest {
    @Test
    public void testMul() {
        TestTemplateClass c = new TestTemplateClass();
        c.doubleField = 1.2;
        c.intField = 10;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ doubleField * 2 }};{{ intField * 2 }}", STRING, null ).render( c ).get() ).isEqualTo( "2.4;20" );
    }

    @Test
    public void testDiv() {
        TestTemplateClass c = new TestTemplateClass();
        c.doubleField = 2.2;
        c.intField = 3;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ doubleField / 2 }};{{ intField / 2 }}", STRING, null ).render( c ).get() ).isEqualTo( "1.1;1" );
    }

    @Test
    public void testMod() {
        TestTemplateClass c = new TestTemplateClass();
        c.doubleField = 2.2;
        c.intField = 3;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ doubleField % 2 }};{{ intField % 2 }}", STRING, null ).render( c ).get() ).isEqualTo( "0.20000000000000018;1" );
    }

    @Test
    public void testMinus() {
        TestTemplateClass c = new TestTemplateClass();
        c.doubleField = 2.2;
        c.intField = 3;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ doubleField - 3 }};{{ intField - 10 }};{{ intField - 1.2 }}", STRING, null ).render( c ).get() ).isEqualTo( "-0.7999999999999998;-7;1.8" );
    }

    @Test
    public void testPlus() {
        TestTemplateClass c = new TestTemplateClass();
        c.doubleField = 2.2;
        c.intField = 3;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ doubleField + 3.3 }};{{ intField + 10 }}", STRING, null ).render( c ).get() ).isEqualTo( "5.5;13" );
    }
}
