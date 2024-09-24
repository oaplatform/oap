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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static oap.template.TemplateAccumulators.BINARY;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineOrTest extends Fixtures {
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
    public void testOrEmptyString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field | default field2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOrNull() {
        TestTemplateClass c = new TestTemplateClass();

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field | default field2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testOrDefaultBinary() throws IOException {
        TestTemplateClass c = new TestTemplateClass();

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ intObjectField | default childNullable.intObjectField ?? 3 }}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( 3 ) ) );
    }

    @Test
    public void testOrEmptyStringWithBinaryAccumulator() throws IOException {
        TestTemplateClass c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field | default field2 }}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( "f2" ) ) );
    }

    @Test
    public void testOrCollections() {
        TestTemplateClass c = new TestTemplateClass();
        c.list2 = List.of( 2, 3 );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ list | default list2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[2,3]" );
    }
}
