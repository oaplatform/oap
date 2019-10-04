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

import lombok.val;
import org.testng.annotations.Test;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


public class BinderHoconTest {
    @Test
    public void pattern() {
        val pattern = "{test = \"[^a]+\"}";
        val obj = Binder.hocon.<BeanPattern>unmarshal( BeanPattern.class, pattern );

        assertThat( obj.test.pattern() ).isEqualTo( "[^a]+" );
    }

    @Test
    public void envList() {
        val json = "{list = [${?LIST_ENV}]}";
        System.setProperty( "LIST_ENV", "1a,2a" );

        val obj = Binder.hocon.<BeanPattern>unmarshal( BeanPattern.class, json );

        assertThat( obj.list ).isEqualTo( singletonList( "1a,2a" ) );
    }

    public static class BeanPattern {
        public Pattern test;
        public List<String> list;
    }
}
