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

package oap.dictionary.maven;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertFile;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.TestDirectoryFixture.testPath;

public class DictionaryMojoTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void execute() {
        DictionaryMojo mojo = new DictionaryMojo();
        mojo.sourceDirectory = "src/test/resources/dictionary";
        mojo.dictionaryPackage = "test";
        mojo.outputDirectory = testPath( "dictionary" ).toString();
        mojo.exclude = new String[] { "**/test-dictionary.json" };

        mojo.execute();

        assertFile( testPath( "dictionary/test/TestDictionaryExternalIdAsCharacter.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "TestDictionaryExternalIdAsCharacter.java" ) );
        assertFile( testPath( "dictionary/test/Child1.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "Child1.java" ) );
        assertFile( testPath( "dictionary/test/Child2.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "Child2.java" ) );
    }

}
