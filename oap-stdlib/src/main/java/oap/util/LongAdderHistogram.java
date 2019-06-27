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

import oap.concurrent.LongAdder;
import org.joda.time.DateTimeUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

@ThreadSafe
public class LongAdderHistogram implements Serializable {
    private static final long serialVersionUID = 2085517363901568734L;

    private final LongAdder[] values;
    private volatile long lastTick;

    public LongAdderHistogram() {
        this( new LongAdder[0], 0 );
    }

    public LongAdderHistogram( LongAdder[] values, long lastTick ) {
        this.values = values;
        this.lastTick = lastTick;
    }

    public LongAdderHistogram( int count, long period ) {
        values = new LongAdder[count];
        for( int i = 0; i < count; i++ ) values[i] = new LongAdder();

        lastTick = currentTick( period );
    }

    private long currentTick( long period ) {
        return DateTimeUtils.currentTimeMillis() / period;
    }

    public void inc( long period, long value ) {
        shift( period );
        values[0].add( value );
    }

    public void merge( long period, LongAdderHistogram update ) {
        synchronized( this ) {
            shift( period );
            update.shift( period );
            for( int i = 0; i < values.length; i++ ) {
                values[i].add( update.values[i].longValue() );
            }
        }
    }

    private void shift( long period ) {
        long ct = currentTick( period );
        if( ct == lastTick ) return;
        synchronized( this ) {
            ct = currentTick( period );
            if( ct == lastTick ) return;

            var sc = ( int ) ( ct - lastTick );
            int len = values.length;
            if( sc < values.length ) {
                System.arraycopy( values, 0, values, sc, len - sc );
                for( int i = 0; i < sc; i++ ) values[i] = new LongAdder();
            } else {
                for( int i = 0; i < len; i++ ) values[i] = new LongAdder();
            }

            lastTick = ct;
        }
    }

    public long[] get( long period ) {
        shift( period );

        var length = values.length;
        var ret = new long[length];
        for( int i = 0; i < length; i++ ) {
            ret[i] = values[i].longValue();
        }

        return ret;
    }
}
