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

package oap.logstream;

import oap.testng.AbstractTest;
import oap.testng.Asserts;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class TimestampTest extends AbstractTest {

    @Test
    public void format() throws Exception {
        assertEquals( Timestamp.format( new DateTime( 2015, 12, 3, 11, 28, 30 ), 12 ), "2015-12-03-11-05" );
    }

    @Test
    public void directoryName() throws Exception {
        assertEquals( Timestamp.directoryName( "2015-12-03-11-05" ), "2015-12/03" );
    }

    @Test
    public void timestamps() {
        Asserts.assertEquals( Timestamp.timestamps( new DateTime( 2016, 2, 1, 1, 1, 1 ), 10, 12 ), Stream.of(
            "2016-02-01-00-02",
            "2016-02-01-00-03",
            "2016-02-01-00-04",
            "2016-02-01-00-05",
            "2016-02-01-00-06",
            "2016-02-01-00-07",
            "2016-02-01-00-08",
            "2016-02-01-00-09",
            "2016-02-01-00-10",
            "2016-02-01-00-11",
            "2016-02-01-01-00"
        ) );
    }

    @Test
    public void parse() {
        DateTime[] times = {
            new DateTime( 2016, 2, 1, 1, 0 ),
            new DateTime( 2016, 2, 1, 1, 55 ),
            new DateTime( 2016, 2, 1, 1, 5 ),
            new DateTime( 2016, 2, 1, 1, 15 )
        };
        for( DateTime time : times ) assertEquals( Timestamp.parse( Timestamp.format( time, 12 ), 12 ), time );
    }
}
