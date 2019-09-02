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

package oap.http;

import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.testng.annotations.Test;

import java.util.Locale;

import static oap.testng.Asserts.assertString;

public class CookieTest {
    @Test
    public void expires() {
        Dates.setTimeFixed( 2019, 11, 11, 11, 11, 11, 11 );
        Locale.setDefault( Locale.CHINA );
        Cookie cookie = new Cookie( "test", "test" )
            .withExpires( DateTime.now()
                .plus( new Period( 2, 30, 10, 0 ) )
                .toDateTime( DateTimeZone.UTC ) );
        assertString( cookie.toString() ).isEqualTo( "test=test; expires=Mon, 11-Nov-2019 13:41:21 UTC" );
    }

}