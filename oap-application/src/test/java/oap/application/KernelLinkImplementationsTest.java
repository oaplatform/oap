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

import lombok.val;
import oap.testng.AbstractTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KernelLinkImplementationsTest extends AbstractTest {
    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        Application.unregisterServices();
    }


    @Test
    public void testFieldReference() {
        val kernel = new Kernel(
            singletonList( urlOfTestResource( getClass(), "field-reference.conf" ) ),
            emptyList()
        );

        try {
            kernel.start();
            val service = Application.service( FieldReference.class );

            assertThat( service.ti ).isNotNull();
            assertThat( service.ti.toString() ).isEqualTo( "TestInterfaceImpl1" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testFieldReferences() {
        val kernel = new Kernel(
            singletonList( urlOfTestResource( getClass(), "field-references.conf" ) ),
            emptyList()
        );

        try {
            kernel.start();
            val service = Application.service( FieldReferences.class );

            assertThat( service.tis ).isNotNull();
            assertThat( service.tis.stream().map( Object::toString ).collect( toList() ) )
                .containsExactlyInAnyOrder( "TestInterfaceImpl1", "TestInterfaceImpl3" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testFieldReferenceUnknownInterface() {
        val kernel = new Kernel(
            singletonList( urlOfTestResource( getClass(), "field-reference-unknown-interface.conf" ) ),
            emptyList()
        );

        try {
            assertThatThrownBy( kernel::start ).isInstanceOf( ApplicationException.class );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testFieldReferencesUnknownInterface() {
        val kernel = new Kernel(
            singletonList( urlOfTestResource( getClass(), "field-references-unknown-interface.conf" ) ),
            emptyList()
        );

        try {
            kernel.start();
            val service = Application.service( FieldReferences.class );

            assertThat( service.tis ).isEmpty();
        } finally {
            kernel.stop();
        }
    }

    public interface TestInterface {

    }

    public static class TestInterfaceImpl1 implements TestInterface {
        @Override
        public String toString() {
            return "TestInterfaceImpl1";
        }
    }

    public static class TestInterfaceImpl2 implements TestInterface {
        @Override
        public String toString() {
            return "TestInterfaceImpl2";
        }
    }

    public static class TestInterfaceImpl3 implements TestInterface {
        @Override
        public String toString() {
            return "TestInterfaceImpl3";
        }
    }

    public static class FieldReference {
        public TestInterface ti;
    }

    public static class FieldReferences {
        public List<TestInterface> tis;
    }
}

