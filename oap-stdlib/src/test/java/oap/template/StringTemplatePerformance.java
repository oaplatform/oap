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

package oap.template;

import oap.io.Files;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.benchmark.Benchmark.benchmark;

public class StringTemplatePerformance {

    @Test
    public void cache() {
        Path test = Files.ensureDirectory( TestDirectoryFixture.testPath( "test" ) );
        String clazz = Engine.getName( "test" );

//        benchmark( "template-compile", 10, () -> {
//            var engine = new Engine( null );
//            var template = engine.getTemplate( clazz, Test1.class, "test${id}" );
//
//            template.renderString( new Test1("1") );
//        } ).inThreads( 5, 10 ).experiments( 5 ).run();

        benchmark( "template-disk-cache", 500, () -> {
            var engine = new Engine( test );
            var template = engine.getTemplate( clazz, Test1.class, "test${id}" );

            template.renderString( new Test1( "1" ) );
        } ).inThreads( 5, 1000 ).experiments( 5 ).run();
    }

    public static class Test1 {
        public String id;

        public Test1( String id ) {
            this.id = id;
        }
    }
}
