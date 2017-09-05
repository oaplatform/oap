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

import lombok.AllArgsConstructor;
import lombok.val;
import oap.io.Files;
import oap.testng.AbstractPerformance;
import oap.testng.Env;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

/**
 * Created by igor.petrenko on 15.06.2017.
 */
public class StringTemplatePerformance extends AbstractPerformance {
    private Path test;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        test = Env.tmpPath( "test" );
        Files.ensureDirectory( test );
    }

    @Test
    public void testCache() {
        final String clazz = Engine.getName( "test" );

//        benchmark( "template-compile", 10, () -> {
//            val engine = new Engine( null );
//            val template = engine.getTemplate( clazz, Test1.class, "test${id}" );
//
//            template.renderString( new Test1("1") );
//        } ).inThreads( 5, 10 ).experiments( 5 ).run();

        benchmark( "template-disk-cache", 500, () -> {
            val engine = new Engine( test );
            val template = engine.getTemplate( clazz, Test1.class, "test${id}" );

            template.renderString( new Test1("1") );
        } ).inThreads( 5, 1000 ).experiments( 5 ).run();
    }

    @AllArgsConstructor
    public static class Test1 {
        public String id;
    }
}
