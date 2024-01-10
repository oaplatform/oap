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

package oap.util;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class NumbersTest {

    @Test
    public void testParseLong() {
        assertThat( Numbers.parseLongWithUnits( "1_000_000" ) ).isEqualTo( 1_000_000L );
    }

    @Test
    public void parseLongWithUnits() {
        assertThat( Numbers.parseLongWithUnits( "1s" ) ).isEqualTo( 1000L );
        assertThat( Numbers.parseLongWithUnits( "500ms" ) ).isEqualTo( 500L );
        assertThat( Numbers.parseLongWithUnits( "1 day" ) ).isEqualTo( 24 * 3_600_000L );
        assertThat( Numbers.parseLongWithUnits( "30 days" ) ).isEqualTo( 2592000000L );
        assertThatExceptionOfType( NumberFormatException.class )
            .isThrownBy( () -> Numbers.parseLongWithUnits( "1aaa" ) )
            .withMessage( "1aaa" );
    }

    @Test
    public void testParseDoubleWithPercent() {
        assertThat( Numbers.parseDoubleWithUnits( "∞" ) ).isEqualTo( Double.POSITIVE_INFINITY );
        assertThat( Numbers.parseDoubleWithUnits( "1%" ) ).isEqualTo( 0.01d );
        assertThat( Numbers.parseDoubleWithUnits( "200%" ) ).isEqualTo( 2d );
    }
}
