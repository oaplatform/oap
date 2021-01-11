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

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;

import static oap.io.content.ContentReader.ofProperties;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourcesTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void urls() {
        var urls = Resources.urls( getClass().getName(), "txt" );
        assertThat( urls ).containsOnly(
            urlOfTestResource( getClass(), "resource.txt" ),
            urlOfTestResource( getClass(), "test.txt" )
        );
    }

    @Test
    public void readProperties() {
        var properties = Resources.read( ResourcesTest.class,
            ResourcesTest.class.getSimpleName() + "/resource.properties", ofProperties() );
        assertThat( properties ).isPresent();
        assertThat( properties.get() ).hasSize( 2 );
    }

    @Test
    public void filePaths() {
        var paths = Resources.filePaths( getClass(), "/file-paths" );
        assertThat( paths ).hasSize( 1 );
        assertThat( paths.get( 0 ) ).isDirectory();
    }

    @Test
    public void filePath() {
        Optional<Path> path = Resources.filePath( getClass(), "/file-paths" );
        assertThat( path.orElseThrow() ).isDirectory();
    }

    @Test
    public void readStrings() {
        assertThat( Resources.readStrings( getClass(), ResourcesTest.class.getSimpleName() + "/test.txt" ) )
            .containsOnly( "test\n" );
        assertThat( Resources.readStrings( getClass(), "/oap/io/" + ResourcesTest.class.getSimpleName() + "/test.txt" ) )
            .containsOnly( "test\n" );
    }
}
