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
import oap.util.Dates;
import org.testng.annotations.Test;

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineConfigurationTest {
    private final TemplateEngine engine = new TemplateEngine( Dates.d( 10 ) )
        .withNewConfiguration( c -> c.withExpression( "[", "]" ) );

    @Test
    public void testCustomDelimiterField() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "hello";
        assertThat( engine.getTemplate( "test", new TypeRef<TestTemplateClass>() {},
            "[field]", STRING, null, null ).render( c ).get() ).isEqualTo( "hello" );
    }

    @Test
    public void testCustomDelimiterNestedField() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "nested";
        assertThat( engine.getTemplate( "test", new TypeRef<TestTemplateClass>() {},
            "[child.field]", STRING, null, null ).render( c ).get() ).isEqualTo( "nested" );
    }

    @Test
    public void testDefaultDelimiterStillWorks() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "standard";
        assertThat( engine.getTemplate( "test", new TypeRef<TestTemplateClass>() {},
            "{{field}}", STRING, null, null ).render( c ).get() ).isEqualTo( "standard" );
    }

    @Test
    public void testDollarDelimiterStillWorks() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "dollar";
        assertThat( engine.getTemplate( "test", new TypeRef<TestTemplateClass>() {},
            "${field}", STRING, null, null ).render( c ).get() ).isEqualTo( "dollar" );
    }
}
