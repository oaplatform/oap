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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;

@Slf4j
public class TestDirectory implements Fixture {

    public static TestDirectory FIXTURE = new TestDirectory();

    public static void deleteDirectory( Path path ) {
        try {
            Files.delete( path );
        } catch( UncheckedIOException e ) {
            var fileIterator = iterateFiles( Env.tmp.toFile(), trueFileFilter(), trueFileFilter() );
            while( fileIterator.hasNext() ) {
                var next = fileIterator.next();
                if( next.isDirectory() ) continue;

                System.err.println( "FILE " + next );
            }

            throw e;
        }
    }

    @Override
    public void afterClass() {
        deleteDirectory( Env.tmpRoot );
        cleanTestDirectories();
    }

    @Override
    public void afterMethod() {
        deleteDirectory( Env.tmpRoot );
    }

    @SneakyThrows
    private void cleanTestDirectories() {
        try( var stream = java.nio.file.Files.list( Env.tmp ) ) {
            stream
                .filter( path -> java.nio.file.Files.isDirectory( path ) )
                .filter( path -> Files.getLastModifiedTime( path ) < DateTimeUtils.currentTimeMillis() - Dates.h( 2 ) )
                .forEach( path -> {
                    try {
                        Files.delete( path );
                    } catch( Exception ignored ) {
                    }
                } );
        }
    }
}
