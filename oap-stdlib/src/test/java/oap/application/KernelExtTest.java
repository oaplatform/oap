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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 2021-03-30.
 */
public class KernelExtTest {
    @Test
    public void testModuleExt() {
        var modules = List.of( url( "module-ext.conf" ) );

        var kernel = new Kernel( modules );
        try {
            kernel.start( Map.of( "boot.main", "module-ext" ) );

            var found = kernel.modulesByExt( "ws", TestKernelExt.class );
            assertThat( found ).hasSize( 1 );
            assertThat( found.get( 0 ).ext ).isEqualTo( new TestKernelExt( "/p", "string" ) );

        } finally {
            kernel.stop();
        }
    }

    private URL url( String s ) {
        return urlOfTestResource( getClass(), s );
    }

    public static class TestBean {

    }

    @ToString
    @EqualsAndHashCode
    public static class TestKernelExt {
        public final String path;
        public final String p2;

        @JsonCreator
        public TestKernelExt( String path, String p2 ) {
            this.path = path;
            this.p2 = p2;
        }
    }
}
