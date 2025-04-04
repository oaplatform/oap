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
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.List;

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class TemplateMacrosTest {
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
        assertThat( new TemplateEngine( Dates.d( 10 ) ).getTemplate( "testListFieldDefaultValue", new TypeRef<TestTemplateClass>() {}, "{{ list; toJson() ?? [] }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[]" );

        c.listString = List.of( "1", "2", "3" );
        assertThat( new TemplateEngine( Dates.d( 10 ) ).getTemplate( "testListFieldDefaultValue", new TypeRef<TestTemplateClass>() {}, "{{ listString; toJson() ?? [] }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[\"1\",\"2\",\"3\"]" );
    }
}
