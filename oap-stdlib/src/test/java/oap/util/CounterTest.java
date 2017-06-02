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
import org.testng.annotations.Test;

import static oap.util.Counter.CounterType.DAILY;
import static oap.util.Counter.CounterType.HOURLY;
import static oap.util.Counter.CounterType.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 02.06.2017.
 */
public class CounterTest {
    @Test
    public void testHourly() throws Exception {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        val counter = Counter.get( HOURLY );

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 6, 2, 15, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

    @Test
    public void testDaily() throws Exception {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        val counter = Counter.get( DAILY );

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 6, 3, 0, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

    @Test
    public void testMonthly() throws Exception {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        val counter = Counter.get( MONTHLY );

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 7, 2, 0, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

}