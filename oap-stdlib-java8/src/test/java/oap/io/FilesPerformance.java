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

import oap.testng.Env;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static oap.benchmark.Benchmark.benchmark;

@Test( enabled = false )
public class FilesPerformance {

    public static final int SAMPLES = 100000;
    private Path path;
    private Path path2;
    private Path pathNotExists;

    @BeforeMethod
    public void init() throws Exception {

        path = Env.tmpPath( "tt/test" );
        path2 = Env.tmpPath( "tt/test2/1/2/3/4/5/6/7/8/9/10" );
        pathNotExists = Env.tmpPath( "tt/test2" );
        Files.writeString( path, RandomStringUtils.random( 10 ) );
        Files.writeString( path2, RandomStringUtils.random( 10 ) );
    }

    @Test( enabled = false )
    public void testLastModificationTime() {
        final long[] size = { 0 };

        benchmark( "java.nio.file.Files.getLastModifiedTime()", SAMPLES,
            () -> size[0] += java.nio.file.Files.getLastModifiedTime( path ).to( TimeUnit.NANOSECONDS ) ).run();

        benchmark( "java.nio.file.Files.getLastModifiedTime()-2", SAMPLES,
            () -> size[0] += java.nio.file.Files.getLastModifiedTime( path2 ).to( TimeUnit.NANOSECONDS ) ).run();

        benchmark( "java.io.File.lastModified()", SAMPLES,
            () -> size[0] += path.toFile().lastModified() ).run();
        benchmark( "java.io.File.lastModified()-2", SAMPLES,
            () -> size[0] += path2.toFile().lastModified() ).run();
    }

    @Test( enabled = false )
    public void testExists() {
        benchmark( "java.nio.file.Files.exists()", SAMPLES, () -> {
            java.nio.file.Files.exists( path );
            java.nio.file.Files.exists( pathNotExists );
        } ).run();
        benchmark( "java.io.File.exists()", SAMPLES, () -> {
            path.toFile().exists();
            pathNotExists.toFile().exists();
        } ).run();
    }
}
