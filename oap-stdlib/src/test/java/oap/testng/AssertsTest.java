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

package oap.testng;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Map;

import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Asserts.assertString;

public class AssertsTest {

    @Test
    public void sortedContentOfFileResource() {
        Path unsorted = Asserts.pathOfTestResource( getClass(), "random-flow-of-mind.txt" );
        String expected = Asserts.contentOfTestResource( getClass(), "sorted-flow-of-mind.txt" );
        assertFile( unsorted ).hasContentLineSorting( expected, PLAIN );
    }

    @Test
    public void contentOfTestResource() {
        assertString( Asserts.contentOfTestResource( getClass(), "substitutions.txt", Map.of( "a", 1 ) ) )
            .isEqualTo( "1 = b" );
    }

    @Test
    public void locationOfTestResource() {
        assertString( Asserts.locationOfTestResource( getClass(), "a.txt" ) )
            .isEqualTo( "/oap/testng/AssertsTest/a.txt" );
    }

}
