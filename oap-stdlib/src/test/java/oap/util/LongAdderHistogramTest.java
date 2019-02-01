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

import lombok.val;
import oap.concurrent.LongAdder;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LongAdderHistogramTest {

    private static final int PERIOD_HOUR = 1000 * 60 * 60;

    @AfterMethod
    public void afterMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testHour() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new LongAdderHistogram( 5, PERIOD_HOUR );
        histogram.inc( PERIOD_HOUR, 1 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 1, 0, 0, 0, 0 } );
        DateTimeUtils.setCurrentMillisFixed( 1000 );
        histogram.inc( PERIOD_HOUR, 4 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 5, 0, 0, 0, 0 } );
    }

    @Test
    public void testHourShiftOne() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new LongAdderHistogram( 5, PERIOD_HOUR );
        histogram.inc( PERIOD_HOUR, 3 );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR + 10 );
        histogram.inc( PERIOD_HOUR, 4 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 4, 3, 0, 0, 0 } );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 2 + 10 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 0, 4, 3, 0, 0 } );
    }

    @Test
    public void testHistogramShiftAll() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new LongAdderHistogram( 5, PERIOD_HOUR );
        histogram.inc( PERIOD_HOUR, 3 );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 100 + 10 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 0, 0, 0, 0, 0 } );
    }

    @Test
    public void testMerge() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val master = new LongAdderHistogram( new LongAdder[] { new LongAdder( 1 ), new LongAdder( 4 ), new LongAdder( 0 ) }, 0 );
        val update = new LongAdderHistogram( new LongAdder[] { new LongAdder( 2 ), new LongAdder( 0 ), new LongAdder( 0 ) }, 0 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 3, 4, 0 } );
    }

    @Test
    public void testMergeShift() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val master = new LongAdderHistogram( new LongAdder[] { new LongAdder( 1 ), new LongAdder( 4 ), new LongAdder( 0 ) }, 0 );

        val update = new LongAdderHistogram( new LongAdder[] { new LongAdder( 2 ), new LongAdder( 0 ), new LongAdder( 0 ) }, 1 );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 2 + 1 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new long[] { 0, 2, 1 } );
    }
}
