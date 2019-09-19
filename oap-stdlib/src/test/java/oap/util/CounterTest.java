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

import oap.testng.Fixtures;
import oap.testng.ResetSystemTimer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CounterTest extends Fixtures {
    {
        fixture( ResetSystemTimer.FIXTURE );
    }

    @Test
    public void hourly() {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        var counter = new Counter.HourlyCounter();

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 6, 2, 15, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

    @Test
    public void daily() {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        var counter = new Counter.DailyCounter();

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 6, 3, 0, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

    @Test
    public void monthly() {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );

        var counter = new Counter.MonthlyCounter();

        counter.inc();
        counter.inc( 2 );
        assertThat( counter.value ).isEqualTo( 3 );

        Dates.setTimeFixed( 2017, 7, 2, 0, 0, 0 );
        counter.inc();
        assertThat( counter.value ).isEqualTo( 1 );
    }

    @Test
    public void merge() {
        Dates.setTimeFixed( 2017, 6, 2, 14, 16, 10 );
        var counter1 = new Counter.HourlyCounter();
        counter1.inc();

        var counter2 = new Counter.HourlyCounter();
        counter2.inc();

        Dates.setTimeFixed( 2017, 6, 2, 14 + 1, 16, 10 );
        var counter3 = new Counter.HourlyCounter();
        counter3.inc();

        counter1.merge( counter2 );
        assertThat( counter1.value ).isEqualTo( 2 );

        counter1.merge( counter3 );
        assertThat( counter1.value ).isEqualTo( 1 );
    }
}
