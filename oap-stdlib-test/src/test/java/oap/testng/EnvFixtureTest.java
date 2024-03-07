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

package oap.testng;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvFixtureTest {
    @Test
    public void substitute() {
        System.setProperty( "value", "1" );
        EnvFixture fixture = new EnvFixture()
            .define( "method", "method:${value}" )
            .define( "u", "u=${PWD}" );
        fixture.beforeMethod();
        assertThat( System.getProperty( "method" ) ).isEqualTo( "method:1" );
        assertThat( System.getProperty( "u" ) ).isEqualTo( "u=" );
    }

    @Test
    public void portFor() {
        EnvFixture fixture = new EnvFixture();
        assertThat( fixture.portFor( "AAA" ) ).isEqualTo( fixture.portFor( "AAA" ) );
    }

}
