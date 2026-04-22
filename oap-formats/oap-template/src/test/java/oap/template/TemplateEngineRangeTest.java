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

public class TemplateEngineRangeTest extends AbstractTemplateEngineTest {
    private TestTemplateClass root( String... fieldValues ) {
        TestTemplateClass c = new TestTemplateClass();
        for( String fv : fieldValues ) {
            TestTemplateClass item = new TestTemplateClass();
            item.field = fv;
            c.listItems.add( item );
        }
        return c;
    }

    @Test
    public void testRangeImplicitScope() {
        TestTemplateClass c = root( "a", "b", "c" );
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range listItems }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "abc" );
    }

    @Test
    public void testRangeNamedItem() {
        TestTemplateClass c = root( "x", "y" );
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $item := listItems }}{{ $item.field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "xy" );
    }

    @Test
    public void testRangeNamedIndexItem() {
        TestTemplateClass c = root( "a", "b", "c" );
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $i,$item := listItems }}{{ $i }}:{{ $item.field }} {{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "0:a 1:b 2:c " );
    }

    @Test
    public void testRangeMap() {
        TestTemplateClass c = new TestTemplateClass();
        TestTemplateClass v1 = new TestTemplateClass();
        v1.field = "val1";
        c.mapItems.put( "k1", v1 );
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $k,$v := mapItems }}{{ $k }}={{ $v.field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "k1=val1" );
    }

    @Test
    public void testRangeElseEmpty() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range listItems }}{{ field }}{{% else }}empty{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "empty" );
    }

    @Test
    public void testRangeElseNonEmpty() {
        TestTemplateClass c = root( "v" );
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range listItems }}{{ field }}{{% else }}empty{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "v" );
    }

    @Test
    public void testRangeIntervalLiteral() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $k := 1 .. 3 }}{{ $k }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "123" );
    }

    @Test
    public void testRangeIntervalWithStep() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $k := 1 .. 5 step 2 }}{{ $k }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "135" );
    }

    @Test
    public void testRangeIntervalFieldBased() {
        TestTemplateClass c = new TestTemplateClass();
        c.rangeStart = 2;
        c.rangeEnd = 4;
        c.rangeStep = 1;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% range $k := rangeStart .. rangeEnd step rangeStep }}{{ $k }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "234" );
    }
}
