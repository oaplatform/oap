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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static oap.template.TemplateAccumulators.BINARY;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineConcatenationTest extends Fixtures {
    private TemplateEngine engine;
    private String testMethodName;

    public TemplateEngineConcatenationTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @BeforeClass
    public void beforeClass() {
        engine = new TemplateEngine( Dates.d( 10 ) );
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testConcatenation() {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testConcatenationBinary() throws IOException {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( "f1xf2" ) ) );
    }

    @Test
    public void testConcatenationWithNumber() {
        var c = new TestTemplateClass();
        c.intField = 3;
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{intField,\"x\",field2}}", STRING, null ).render( c ).get() )
            .isEqualTo( "3xf2" );
    }

    @Test
    public void testNestedConcatenation() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        c.child = c1;
        c1.field = "f1";
        c1.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child{field,\"x\",field2}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass2();
        c.child2 = c1;
        c1.field2 = "f1";
        c1.field22 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child2.{field2,\"x\",field22}}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedNullableConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        var c11 = new TestTemplateClass();
        c.childNullable = c1;

        c1.childNullable = c11;

        c11.field2 = "f1";
        c11.intField = 5;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.childNullable.{field2,\"x\",intField}??''}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1x5" );
    }

    @Test
    public void testNestedOptConcatenationWithDot() {
        var c = new TestTemplateClass();
        var c1 = new TestTemplateClass();
        var c11 = new TestTemplateClass();
        c.childOpt = Optional.of( c1 );

        c1.childOpt = Optional.of( c11 );

        c11.field2 = "f1";
        c11.intField = 5;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.childOpt.{field2,\"x\",intField}??''}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1x5" );
    }
}
