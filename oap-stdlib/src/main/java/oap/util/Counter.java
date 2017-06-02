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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;

/**
 * Created by igor.petrenko on 02.06.2017.
 */
@EqualsAndHashCode
@ToString
public abstract class Counter implements Serializable {
    public long tick = -1;
    public long value = 0;

    private Counter() {
    }

    public abstract long currentTick();

    public final void inc() {
        inc( 1 );
    }

    public final void inc( long value ) {
        val currentTick = currentTick();
        if( this.tick != currentTick ) {
            this.value = value;
            this.tick = currentTick;
        } else
            this.value += value;
    }

    public final void reset() {
        tick = -1;
        value = 0;
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class HourlyCounter extends Counter {
        private static final long serialVersionUID = -6350858231677830610L;

        @Override
        public final long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L;
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class DailyCounter extends Counter {
        private static final long serialVersionUID = -4287987989875991573L;

        @Override
        public final long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L / 24L;
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class MonthlyCounter extends Counter {
        private static final long serialVersionUID = 4419536959429173372L;

        @Override
        public final long currentTick() {
            return new DateTime().getMonthOfYear();
        }
    }
}
