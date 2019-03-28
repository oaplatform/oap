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

import com.google.common.base.Preconditions;
import oap.testng.AbstractTest;
import oap.util.Lists;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;

import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelProfileTest extends AbstractTest {
    @BeforeMethod
    public void unregister() {
        Application.unregisterServices();
    }

    @Test
    public void testProfileName() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "module.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "appWithProfileName.conf" ) );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isNotNull();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isNull();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isNotNull();
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testProfileName2() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "module.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "appWithProfileName2.conf" ) );

            assertThat( kernel.<TestProfile1>service( "profile1" ) ).isNull();
            assertThat( kernel.<TestProfile2>service( "profile2" ) ).isNotNull();
            assertThat( kernel.<TestProfile3>service( "profile3" ) ).isNotNull();
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testProfile3() {
        List<URL> modules = Lists.of( urlOfTestResource( getClass(), "module3.yaml" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "appWithProfileName.conf" ) );
            assertThat( kernel.<TestContainer>service( "container" ) ).isNotNull();
        } finally {
            kernel.stop();
        }
    }

    public interface TestProfile {

    }

    public static class TestProfile1 implements TestProfile {
    }

    public static class TestProfile2 implements TestProfile {
    }

    public static class TestProfile3 implements TestProfile {
    }

    public static class TestContainer {
        public TestContainer( TestProfile profile ) {
            assertThat( profile ).isNotNull();
        }
    }
}

