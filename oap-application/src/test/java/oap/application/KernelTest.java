/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertEventually;
import static oap.util.Pair.__;
import static org.testng.Assert.*;

public class KernelTest extends AbstractTest {
    @Test
    public void startStop() throws InterruptedException {
        Kernel kernel = new Kernel( Lists.of(
            Resources.url( KernelTest.class, "modules/m1.json" ).get(),
            Resources.url( KernelTest.class, "modules/m2.json" ).get()
        ) );
        kernel.start( Maps.of( __( "ServiceOne", Maps.of( __( "i", 3 ) ) ) ) );
        try {
            assertEventually( 50, 30, () -> {
                assertEquals( ServiceOne.instances, 1 );
                assertEquals( Application.<ServiceOne>service( ServiceOne.class.getSimpleName() ).i, 3 );
                assertEquals( Application.<ServiceTwo>service( ServiceTwo.class.getSimpleName() ).j, 1 );
                assertEquals( Application.<ServiceTwo>service( ServiceTwo.class.getSimpleName() ).one,
                    Application.<ServiceOne>service( ServiceOne.class.getSimpleName() ) );
                assertTrue( Application.<ServiceTwo>service( ServiceTwo.class.getSimpleName() ).started );
                assertTrue( Application.<ServiceScheduled>service( ServiceScheduled.class.getSimpleName() ).executed );
            } );
        } finally {
            kernel.stop();
        }
    }

}

class ServiceOne {
    static volatile int instances;
    int i;

    public ServiceOne() {
    }
}

class ServiceTwo {
    int j;
    boolean started;
    public ServiceOne one;

    public ServiceTwo() {
        assertNotNull( Application.service( ServiceOne.class.getSimpleName() ) );
        ServiceOne.instances++;
    }

    public void start() {
        System.out.println( "started" );
        started = true;
    }
}

class ServiceScheduled implements Runnable {

    boolean executed;

    public ServiceScheduled() {
    }

    @Override
    public void run() {
        executed = true;
    }
}
