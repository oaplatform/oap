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
import oap.util.Dates;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class TemplateMacrosTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;
    private TemplateEngine templateEngine;

    public TemplateMacrosTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        templateEngine = new TemplateEngine( testDirectoryFixture.testDirectory(), Dates.d( 10 ) );
    }

    @Test
    public void testUrlencode() {
        assertThat( TemplateMacros.urlencode( "12 ?3", 2 ) ).isEqualTo( "12%2B%253F3" );
        assertThat( TemplateMacros.urlencode( null, 1 ) ).isNull();
        assertThat( TemplateMacros.urlencode( null ) ).isNull();
    }

    @Test
    public void testFormat() {
        assertThat( TemplateMacros.format( new DateTime( 2022, 9, 20, 17, 1, 2, UTC ), "DATE" ) )
            .isEqualTo( "2022-09-20" );
        assertThat( TemplateMacros.format( new DateTime( 2022, 9, 20, 17, 1, 2, UTC ), "YYYY-dd" ) )
            .isEqualTo( "2022-20" );
    }

    @Test
    public void testListFieldDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        c.list = null;
        assertThat( new TemplateEngine( Dates.d( 10 ) ).getTemplate( "testListFieldDefaultValue", new TypeRef<TestTemplateClass>() {}, "{{ list; toJson() ?? [] }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "[]" );

        c.listString = List.of( "1", "2", "3" );
        assertThat( templateEngine.getTemplate( "testListFieldDefaultValue", new TypeRef<TestTemplateClass>() {}, "{{ listString; toJson() ?? [] }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "[\"1\",\"2\",\"3\"]" );
    }

    @Test
    public void testToString() {
        TestTemplateClass c = new TestTemplateClass();
        c.byteObjectField = null;
        c.shortObjectField = null;
        c.intObjectField = null;
        c.longObjectField = null;
        c.floatObjectField = null;
        c.doubleObjectField = null;

        assertThat( templateEngine.getTemplate( "testToString_byte", new TypeRef<TestTemplateClass>() {}, "{{ byteObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
        assertThat( templateEngine.getTemplate( "testToString_short", new TypeRef<TestTemplateClass>() {}, "{{ shortObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
        assertThat( templateEngine.getTemplate( "testToString_int", new TypeRef<TestTemplateClass>() {}, "{{ intObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
        assertThat( templateEngine.getTemplate( "testToString_long", new TypeRef<TestTemplateClass>() {}, "{{ longObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
        assertThat( templateEngine.getTemplate( "testToString_float", new TypeRef<TestTemplateClass>() {}, "{{ floatObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
        assertThat( templateEngine.getTemplate( "testToString_double", new TypeRef<TestTemplateClass>() {}, "{{ doubleObjectField; toString() ?? 'str' }}", STRING, null, null ).render( c ).get() )
            .isEqualTo( "str" );
    }
}
