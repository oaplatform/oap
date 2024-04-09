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
import java.util.List;

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineEnumTest extends Fixtures {
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
    public void testEnumField() {
        var c = new TestTemplateClass();
        c.enumFieldWithoutDefaultValue = TestTemplateEnumWithoutDefaultValue.VAL1;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumFieldWithoutDefaultValue}", STRING, null ).render( c ).get() )
            .isEqualTo( "VAL1" );
    }

    @Test
    public void testEnumFieldDefault() {
        var c = new TestTemplateClass();
        c.enumField = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField??'VAL2'}", STRING, null ).render( c ).get() )
            .isEqualTo( "VAL2" );
    }

    @Test
    public void testEnumFieldWithoutDefault() {
        var c = new TestTemplateClass();
        c.enumField = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testEnumFieldDefaultEmptyAsUNKNOWN() {
        var c = new TestTemplateClass();
        c.enumField = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${enumField??''}", STRING, null ).render( c ).get() )
            .isEqualTo( "UNKNOWN" );
    }

    @Test
    public void testEnumFieldNonNull() {
        var c = new TestTemplateClass();
        c.nonNullEnumField = TestTemplateEnum.VAL2;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${nonNullEnumField}", STRING, null ).render( c ).get() )
            .isEqualTo( "VAL2" );
    }

    @Test
    public void testListEnum() {
        var c = new TestTemplateClass();
        c.listEnum = List.of( TestTemplateEnum.VAL2, TestTemplateEnum.VAL1 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${listEnum}", STRING, null ).render( c ).get() )
            .isEqualTo( "['VAL2','VAL1']" );
    }
}
