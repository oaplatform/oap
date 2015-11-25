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

package oap.application;

import oap.io.Resources;
import oap.testng.AbstractTest;
import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;

import static oap.testng.Asserts.assertEventually;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class KernelTest extends AbstractTest {
    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();
        ServiceOne.instances = 0;
    }

    @Test
    public void start() {
        List<URL> modules = Module.fromClassPath();
        modules.add( Resources.url( KernelTest.class, "modules/m1.conf" ).get() );
        modules.add( Resources.url( KernelTest.class, "modules/m2.json" ).get() );
        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( Resources.filePath( getClass(), "application.conf" ).get() );
            assertEventually( 50, 10, () -> {
                assertEquals( ServiceOne.instances, 1 );
                assertEquals( Application.service( ServiceOne.class ).i, 2 );
                assertEquals( Application.service( ServiceOne.class ).i2, 100 );
                assertEquals( Application.service( ServiceTwo.class ).j, 3 );
                assertEquals( Application.service( ServiceTwo.class ).one,
                    Application.service( ServiceOne.class ) );
                assertTrue( Application.service( ServiceTwo.class ).started );
                assertTrue( Application.service( ServiceScheduled.class ).executed );

                TestBean b1 = new TestBean( "v1", 1 );
                TestBean b2 = new TestBean( "v2", 2 );

                assertEquals(
                    Application.<Hello>service( "hello" ).hello( Lists.of( b1, b2 ) ),
                    Application.service( ServiceTwo.class ).hello( Lists.of( b1, b2 ) )
                );
                Application.<Hello>service( "hello" ).voidMethod( "test" );
                assertEquals( Application.service( ServiceTwo.class ).test, "test" );
            } );
        } finally {
            kernel.stop();
        }
    }
}

