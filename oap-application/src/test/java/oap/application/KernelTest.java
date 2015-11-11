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
import java.util.ArrayList;

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
    public void startStopJsonConfig() throws InterruptedException {
        ArrayList<URL> modules = Lists.of(
            Resources.url( KernelTest.class, "modules/m1.json" ).get(),
            Resources.url( KernelTest.class, "modules/m2.json" ).get()
        );
        run( modules );
    }

    @Test
    public void startStopHoconConfig() throws InterruptedException {
        ArrayList<URL> modules = Lists.of(
            Resources.url( KernelTest.class, "modules/m1.conf" ).get(),
            Resources.url( KernelTest.class, "modules/m2.conf" ).get()
        );
        run( modules );
    }

    private void run( ArrayList<URL> modules ) {
        modules.addAll( Module.fromClassPath() );
        try( Kernel kernel = new Kernel( modules, "ServiceOne.parameters.i2 = 100" ) ) {
            kernel.start( "ServiceOne.i = 3" );
            assertEventually( 50, 10, () -> {
                assertEquals( ServiceOne.instances, 1 );
                assertEquals( Application.findFirst( ServiceOne.class ).get().i, 3 );
                assertEquals( Application.findFirst( ServiceOne.class ).get().i2, 100 );
                assertEquals( Application.findFirst( ServiceTwo.class ).get().j, 1 );
                assertEquals( Application.findFirst( ServiceTwo.class ).get().one,
                    Application.findFirst( ServiceOne.class ).get() );
                assertTrue( Application.findFirst( ServiceTwo.class ).get().started );
                assertTrue( Application.findFirst( ServiceScheduled.class ).get().executed );
                assertEquals( Application.<RemoteHello>service( "hello" ).hello(),
                    Application.findFirst( ServiceTwo.class ).get().hello() );
            } );
        }
    }

}

