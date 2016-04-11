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

import oap.testng.AbstractTest;
import oap.util.Lists;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;

import static oap.testng.Asserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class KernelTest extends AbstractTest {

   @Test
   public void start() {
      System.setProperty( "failedValue", "value that can fail config parsing" );
      List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
      modules.add( urlOfTestResource( KernelTest.class, "modules/m1.conf" ) );
      modules.add( urlOfTestResource( KernelTest.class, "modules/m2.json" ) );

      Kernel kernel = new Kernel( modules );
      try {
         kernel.start( pathOfTestResource( getClass(), "application.conf" ),
            pathOfTestResource( getClass(), "conf.d" ) );
         assertEventually( 50, 1, () -> {
            assertThat( ServiceOne.instances ).isEqualTo( 1 );
            ServiceOne one = Application.service( ServiceOne.class );
            ServiceTwo two = Application.service( ServiceTwo.class );
            assertNotNull( one );
            assertThat( one.i ).isEqualTo( 2 );
            assertThat( one.i2 ).isEqualTo( 100 );
            assertNotNull( two );
            assertThat( two.j ).isEqualTo( 3000 );
            assertThat( two.one ).isSameAs( one );
            assertTrue( two.started );
            ServiceScheduled scheduled = Application.service( ServiceScheduled.class );
            assertNotNull( scheduled );
            assertTrue( scheduled.executed );

            ServiceDepsList depsList = Application.service( ServiceDepsList.class );
            assertNotNull( depsList );
            assertThat( depsList.deps ).contains( one, two );

            TestBean b1 = new TestBean( "v1", 1 );
            TestBean b2 = new TestBean( "v2", 2 );

            assertThat( Application.<Hello>service( "hello" ).hello( Lists.of( b1, b2 ) ) ).contains( b1, b2 );
            assertThat( two.beans ).contains( b1, b2 );

            Application.<Hello>service( "hello" ).voidMethod( "test" );
            assertString( two.test ).isEqualTo( "test" );
         } );
      } finally {
         kernel.stop();
      }
   }

}

