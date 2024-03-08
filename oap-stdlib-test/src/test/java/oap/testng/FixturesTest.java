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

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FixturesTest extends Fixtures {

    private F fixture;

    {
        fixture = new F();
        fixture( fixture );
    }

    @Test
    public void test() {
        assertThat( fixture.result ).isEqualTo( "BCBM" );
    }

    @AfterTest
    public void assertFixture() {
        assertThat( fixture.result ).isEqualTo( "BCBMAMAC" );
    }


    @Slf4j
    static class F extends AbstractFixture<F> {
        String result = "";

        F() {
            super( "F" );
        }

        @Override
        public void beforeClass() {
            log.info( "beforeClass" );
            result += "BC";
        }

        @Override
        public void afterClass() {
            log.info( "afterClass" );
            result += "AC";
        }

        @Override
        public void beforeMethod() {
            log.info( "beforeMethod" );
            result += "BM";
        }

        @Override
        public void afterMethod() {
            log.info( "afterMethod" );
            result += "AM";
        }
    }
}
