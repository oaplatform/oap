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

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Iterator;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

@Slf4j
public class TestDirectory implements Fixture {

    public static TestDirectory FIXTURE = new TestDirectory();

    @Override
    public void afterClass() {
        deleteDirectory( Env.tmpRoot );
    }

    @Override
    public void afterMethod() {
        deleteDirectory( Env.tmpRoot );
    }

    public static void deleteDirectory( Path path ) {
        try {
            Files.delete( path );
        } catch( UncheckedIOException e ) {
            final Iterator<File> fileIterator = iterateFiles( Env.tmp.toFile(), trueFileFilter(), trueFileFilter() );
            while( fileIterator.hasNext() ) {
                final File next = fileIterator.next();
                if( next.isDirectory() ) continue;

                System.err.println( "FILE " + next );
            }

            throw e;
        }
    }
}
