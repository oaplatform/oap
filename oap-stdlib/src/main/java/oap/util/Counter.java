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
public abstract class Counter<T extends Counter<T>> implements Serializable {
    public long tick = -1;
    public long value = 0;

    private Counter() {
    }

    protected abstract long _currentTick();

    public final void inc() {
        inc( 1 );
    }

    public final long get( long tick ) {
        return this.tick == tick ? value : 0;
    }

    public Counter<T> merge( Counter<T> other ) {
        if( this.tick == other.tick ) this.value += other.value;
        else if( this.tick < other.tick ) {
            this.value = other.value;
            this.tick = other.tick;
        }
        return this;
    }

    public final void inc( long value ) {
        val currentTick = _currentTick();
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
    public static final class HourlyCounter extends Counter<HourlyCounter> {
        private static final long serialVersionUID = -6350858231677830610L;

        public static long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L;
        }

        @Override
        protected final long _currentTick() {
            return currentTick();
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class DailyCounter extends Counter<DailyCounter> {
        private static final long serialVersionUID = -4287987989875991573L;

        public static long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L / 24L;
        }

        @Override
        protected final long _currentTick() {
            return currentTick();
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class MonthlyCounter extends Counter<MonthlyCounter> {
        private static final long serialVersionUID = 4419536959429173372L;

        public static long currentTick() {
            return new DateTime().getMonthOfYear();
        }

        @Override
        protected final long _currentTick() {
            return currentTick();
        }
    }
}
