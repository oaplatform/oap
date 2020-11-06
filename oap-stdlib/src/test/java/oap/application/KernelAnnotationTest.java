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

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;


import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelAnnotationTest {
    @Test
    public void testServiceName() {
        var modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "modules/servicename.conf" ) );

        try( var kernel = new Kernel( modules ) ) {
            kernel.start();

            var fServiceName = kernel.serviceOfClass2( TestServiceNameField.class );
            System.out.println( fServiceName.serviceName );
            assertThat( fServiceName.serviceName ).isEqualTo( "field" );

            var sServiceName = kernel.serviceOfClass2( TestServiceNameFSetter.class );
            assertThat( sServiceName.serviceName ).isEqualTo( "setter" );
        }
    }

    public static class TestServiceNameField {
        @ServiceName
        private String serviceName = "<change me>";
    }

    public static class TestServiceNameFSetter {
        private String serviceName = "<change me>";

        @ServiceName
        public void setServiceName( String serviceName ) {
            this.serviceName = serviceName;
        }
    }
}

