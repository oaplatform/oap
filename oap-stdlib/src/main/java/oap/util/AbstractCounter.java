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
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.io.Serial;
import java.io.Serializable;

import static org.joda.time.DateTimeZone.UTC;

@EqualsAndHashCode
@ToString
public abstract class AbstractCounter<T extends AbstractCounter<T>> implements Mergeable<AbstractCounter<T>>, Serializable {
    public long tick = -1;
    public long value = 0;

    private AbstractCounter() {
    }

    protected abstract long getCurrentTick();

    public final void inc() {
        inc( 1 );
    }

    public final synchronized void inc( long value ) {
        var currentTick = getCurrentTick();
        if( this.tick != currentTick ) {
            this.value = value;
            this.tick = currentTick;
        } else
            this.value += value;
    }

    public final long get( long tick ) {
        return this.tick == tick ? value : 0;
    }

    @Override
    public synchronized AbstractCounter<T> merge( AbstractCounter<T> other ) {
        if( this.tick == other.tick ) this.value += other.value;
        else if( this.tick < other.tick ) {
            this.value = other.value;
            this.tick = other.tick;
        }

        return this;
    }

    public final synchronized void reset() {
        tick = -1;
        value = 0;
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class CustomCounter extends AbstractCounter<HourlyCounter> {
        @Serial
        private static final long serialVersionUID = 5833832571566146598L;
        private final long periodMs;

        public CustomCounter( long periodMs ) {
            this.periodMs = periodMs;
        }

        public CustomCounter( long periodMs, long startPeriod, long count ) {
            this.periodMs = periodMs;
            this.tick = startPeriod / periodMs;
            this.value = count;
        }

        public long currentTick() {
            return DateTimeUtils.currentTimeMillis() / periodMs;
        }

        @Override
        protected long getCurrentTick() {
            return currentTick();
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class HourlyCounter extends AbstractCounter<HourlyCounter> {
        @Serial
        private static final long serialVersionUID = -6350858231677830610L;

        public static long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L;
        }

        @Override
        protected long getCurrentTick() {
            return currentTick();
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class DailyCounter extends AbstractCounter<DailyCounter> {
        @Serial
        private static final long serialVersionUID = -4287987989875991573L;

        public static long currentTick() {
            return DateTimeUtils.currentTimeMillis() / 1000L / 60L / 60L / 24L;
        }

        @Override
        protected long getCurrentTick() {
            return currentTick();
        }
    }

    @ToString( callSuper = true )
    @EqualsAndHashCode( callSuper = true )
    public static final class MonthlyCounter extends AbstractCounter<MonthlyCounter> {
        @Serial
        private static final long serialVersionUID = 4419536959429173372L;

        public static long currentTick() {
            return new DateTime( UTC ).getMonthOfYear();
        }

        @Override
        protected long getCurrentTick() {
            return currentTick();
        }
    }
}
