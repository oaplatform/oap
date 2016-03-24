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
package oap.io;

import java.nio.file.Path;

import static org.testng.Assert.*;

@Deprecated
public class IoAsserts {

    @Deprecated
    public static void assertFileContent( Path actual, String content ) {
        assertFileContent( actual, IoStreams.Encoding.PLAIN, content );
    }

    @Deprecated
    public static void assertFileResource( Class<?> contextClass, Path actual, String expectedResource ) {
        assertFileResource( contextClass, actual, IoStreams.Encoding.PLAIN, expectedResource );
    }

    @Deprecated
    public static void assertFileResource( Class<?> contextClass, Path actual, IoStreams.Encoding encoding,
                                           String expectedResource ) {
        assertEquals( Files.readString( actual, encoding ),
            Resources.readString( contextClass, contextClass.getSimpleName() + "/" + expectedResource ).get() );
    }

    @Deprecated
    public static void assertContentResource( Class<?> contextClass, String actual, String expectedResource ) {
        assertEquals( actual,
            Resources.readString( contextClass, contextClass.getSimpleName() + "/" + expectedResource ).get() );
    }

    @Deprecated
    public static void assertFileContent( Path actual, IoStreams.Encoding encoding, String content ) {
        assertTrue( actual.toFile().exists(), "file " + actual + " does not exist" );
        assertEquals( Files.readString( actual, encoding ), content );
    }

    @Deprecated
    public static void assertFileDoesNotExist( Path path ) {
        assertFalse( path.toFile().exists() );
    }
}
