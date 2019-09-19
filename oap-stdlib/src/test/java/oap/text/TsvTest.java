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

package oap.text;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 09/19/2019.
 */
public class TsvTest {
    public final ArrayList<String> split = new ArrayList<>();

    @BeforeMethod
    public void beforeMethod() {
        split.clear();
    }

    @Test
    public void testSplit() {
        Tsv.split( "1", split );
        assertThat( split ).containsExactly( "1" );
    }

    @Test
    public void testSplitTab() {
        Tsv.split( "1\t5\tttt", split );
        assertThat( split ).containsExactly( "1", "5", "ttt" );
    }

    @Test
    public void testSplitTabEscape() {
        Tsv.split( "1\\\\t5\tttt", split );
        assertThat( split ).containsExactly( "1\\\\t5", "ttt" );
    }
}
