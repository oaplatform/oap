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

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class KernelLinkImplementationsTest {

    @Test
    public void fieldReference() {
        var kernel = new Kernel( List.of( urlOfTestResource( getClass(), "field-reference.conf" ) ) );

        try {
            kernel.start( Map.of( "boot.main", "field-reference" ) );
            FieldReference service = kernel.<FieldReference>service( "*:m" ).get();

            assertThat( service.ti ).isNotNull();
            assertThat( service.ti.toString() ).isEqualTo( "TestInterfaceImpl1" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void fieldReferences() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "field-references.conf" ) )
        );

        try {
            kernel.start( Map.of( "boot.main", "field-references" ) );
            FieldReferences service = kernel.<FieldReferences>service( "*:m" ).get();

            assertThat( service.tis ).isNotNull();
            assertThat( service.tis.stream().map( Object::toString ).collect( toList() ) )
                .containsExactlyInAnyOrder( "TestInterfaceImpl1", "TestInterfaceImpl3" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void fieldReferenceUnknownInterface() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "field-reference-unknown-interface.conf" ) )
        );

        try {
            assertThatThrownBy( () -> kernel.start( Map.of( "boot.main", "field-reference-unknown-interface" ) ) )
                .isInstanceOf( ApplicationException.class );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void fieldReferencesUnknownInterface() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "field-references-unknown-interface.conf" ) )
        );

        try {
            kernel.start( Map.of( "boot.main", "field-references-unknown-interface" ) );
            FieldReferences service = kernel.<FieldReferences>service( "*:m" ).get();

            assertThat( service.tis ).isEmpty();
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testDifferentTypesOfFieldAndConstructor() {
        var kernel = new Kernel(
            List.of( urlOfTestResource( getClass(), "cons-field-diff-types.conf" ) )
        );

        try {
            kernel.start( Map.of( "boot.main", "cons-field-diff-types" ) );
            var list = kernel.serviceOfClass( TestList.class ).orElseThrow();

            assertThat( list.list ).containsExactly( entry( "a", "a" ), entry( "b", "b" ), entry( "c", "c" ) );
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

    public static class TestList {
        public final LinkedHashMap<String, String> list = new LinkedHashMap<>();

        public TestList( List<String> list ) {
            for( var item : list ) this.list.put( item, item );
        }
    }
}
