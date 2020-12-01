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

package oap.jpath;

import lombok.ToString;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 2020-06-08.
 */
public class JPathTest {
    @Test
    public void testVariable() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test}", Map.of( "test", 1 ), output );

        assertThat( output.toString() ).isEqualTo( "1" );
    }

    @Test
    public void testNested() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.val}", Map.of( "test", new TestBean( "val1", null, null ) ), output );

        assertThat( output.toString() ).isEqualTo( "val1" );
    }

    @Test
    public void testPrivateField() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.testPrivate.val}", Map.of( "test", new TestBean( "val1", null, new TestBean( "pv", null, null ) ) ), output );

        assertThat( output.toString() ).isEqualTo( "pv" );
    }

    @Test
    public void testPrivateGetter() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.getPrivate().val}", Map.of( "test", new TestBean( "val1", null, new TestBean( "pv", null, null ) ) ), output );

        assertThat( output.toString() ).isEqualTo( "pv" );
    }

    @Test
    public void testSuperMethod() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.keySet().stream().count()}", Map.of( "test", Map.of( "1", "2", "3", "4" ) ), output );
        assertThat( output.toString() ).isEqualTo( "2" );
    }

    @Test
    public void testMethodString() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.getString(\"str\")}", Map.of( "test", new TestBean( "val1", null, null ) ), output );
        assertThat( output.toString() ).isEqualTo( "str" );

        output.reset();
        JPath.evaluate( "${test.getString(\"str\", \"str2\")}", Map.of( "test", new TestBean( "val1", null, null ) ), output );
        assertThat( output.toString() ).isEqualTo( "strstr2" );
    }

    @Test
    public void testArray() {
        var output = new StringBuilderJPathOutput();
        var testBean = new TestBean( "val1", null, null );
        testBean.intList = new int[] { 1, 2, 3 };
        JPath.evaluate( "${test.intList[1]}", Map.of( "test", testBean ), output );
        assertThat( output.toString() ).isEqualTo( "2" );
    }

    @Test
    public void testIndexedList() {
        var output = new StringBuilderJPathOutput();
        var testBean = new TestBean( "val1", null, null );
        testBean.list = List.of( new TestBean( "b1", null, null ), new TestBean( "b2", null, null ) );
        JPath.evaluate( "${test.list[1].val}", Map.of( "test", testBean ), output );
        assertThat( output.toString() ).isEqualTo( "b2" );
    }

    @Test
    public void testDecimalInteger() {
        var output = new StringBuilderJPathOutput();
        JPath.evaluate( "${test.getStringInt(\"str\", 2)}", Map.of( "test", new TestBean( "val1", null, null ) ), output );
        assertThat( output.toString() ).isEqualTo( "str2" );

        output.reset();
        JPath.evaluate( "${test.getStringLong(\"str\", 3)}", Map.of( "test", new TestBean( "val1", null, null ) ), output );
        assertThat( output.toString() ).isEqualTo( "str3" );

        output.reset();
        JPath.evaluate( "${test.getStringLong2(\"str\", 4)}", Map.of( "test", new TestBean( "val1", null, null ) ), output );
        assertThat( output.toString() ).isEqualTo( "str4" );
    }

    @ToString
    public static class TestBean {
        public String val;
        public TestBean test1;
        public List<TestBean> list;
        public int[] intList;
        private TestBean testPrivate;

        public TestBean( String val, TestBean test1, TestBean testPrivate ) {
            this.val = val;
            this.test1 = test1;
            this.testPrivate = testPrivate;
        }

        public TestBean get() {
            return testPrivate;
        }

        private TestBean getPrivate() {
            return testPrivate;
        }

        public String getString( String str ) {
            return str;
        }

        public String getString( String str, String str2 ) {
            return str + str2;
        }

        public String getStringInt( String str, int i ) {
            return str + i;
        }

        public String getStringLong( String str, long l ) {
            return str + l;
        }

        public String getStringLong2( String str, Long l ) {
            return str + l;
        }
    }
}
