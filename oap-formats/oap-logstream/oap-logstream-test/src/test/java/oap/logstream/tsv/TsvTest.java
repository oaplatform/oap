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

package oap.logstream.tsv;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;


@Deprecated
public class TsvTest {
    public final ArrayList<String> split = new ArrayList<>();

    @BeforeMethod
    public void beforeMethod() {
        split.clear();
    }

    @Test
    public void testSplit() {
        TsvInputStream.split( "1", split );
        assertThat( split ).containsExactly( "1" );
    }

    @Test
    public void testEmptyLine() {
        TsvInputStream.split( "", split );
        assertThat( split ).containsExactly( "" );
    }

    @Test
    public void testSplitTab() {
        TsvInputStream.split( "1\t5\tttt", split );
        assertThat( split ).containsExactly( "1", "5", "ttt" );
    }

    @Test
    public void testSplitTabEscape() {
        TsvInputStream.split( "1\\t5\t\\r\\nttt", split );
        assertThat( split ).containsExactly( "1\\t5", "\\r\\nttt" );
    }

    @Test
    public void testEmptyCell() {
        TsvInputStream.split( "start\t\tend", split );
        assertThat( split ).containsExactly( "start", "", "end" );
    }

    @Test
    public void testEmptyCellEnd() {
        TsvInputStream.split( "start\t\t", split );
        assertThat( split ).containsExactly( "start", "", "" );
    }

    @Test
    public void testEscape() {
        assertString( TsvInputStream.escape( "1\n2\r3\t4\\5\\" ) ).isEqualTo( "1\\n2\\r3\\t4\\\\5\\\\" );
    }
}
