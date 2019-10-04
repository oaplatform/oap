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

package oap.json;

import org.apache.commons.lang3.mutable.MutableObject;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MutableObjectModuleTest {
    @Test
    public void serializeDeserialize() {
        final TestMutableObjectBean b = new TestMutableObjectBean();
        b.i.setValue( 101 );

        final String marshal = Binder.json.marshal( b );
        assertThat( marshal ).isEqualTo( "{\"i\":101}" );

        final TestMutableObjectBean ub = Binder.json.unmarshal( TestMutableObjectBean.class, marshal );
        assertThat( ub.i.getValue() ).isEqualTo( 101 );
    }

    @Test
    public void update() {
        final TestMutableObjectBean b = new TestMutableObjectBean();
        b.i.setValue( 101 );

        Binder.update( b, "{i = 102}" );
        assertThat( b.i.getValue() ).isEqualTo( 102 );
    }

    public static class TestMutableObject extends MutableObject<Integer> {

    }

    public static class TestMutableObjectBean {
        public TestMutableObject i = new TestMutableObject();
    }
}
