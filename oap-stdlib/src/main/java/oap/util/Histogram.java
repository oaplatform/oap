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

import lombok.experimental.var;
import lombok.val;
import oap.concurrent.LongAdder;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;

/**
 * Created by igor.petrenko on 16.02.2018.
 */
public class Histogram implements Serializable {
    private static final long serialVersionUID = 2085517363901568734L;

    private final LongAdder[] values;
    private final long period;
    private volatile long lastTick;

    public Histogram() {
        this( new LongAdder[0], 0, 0 );
    }

    public Histogram( LongAdder[] values, long period, long lastTick ) {
        this.values = values;
        this.period = period;
        this.lastTick = lastTick;
    }

    public Histogram( int count, long period ) {
        values = new LongAdder[count];
        for( int i = 0; i < count; i++ ) values[i] = new LongAdder();

        this.period = period;
        lastTick = currentTick();
    }

    private long currentTick() {
        return DateTimeUtils.currentTimeMillis() / period;
    }

    public void inc( long value ) {
        shift();
        values[0].add( value );
    }

    public Histogram merge( Histogram update ) {
        synchronized( this ) {
            shift();
            update.shift();
            for( int i = 0; i < values.length; i++ ) {
                values[i].add( update.values[i].longValue() );
            }
        }
        return this;
    }

    private void shift() {
        var ct = currentTick();
        if( ct == lastTick ) return;
        synchronized( this ) {
            ct = currentTick();
            if( ct == lastTick ) return;

            val sc = ( int ) ( ct - lastTick );
            var len = values.length;
            if( sc < values.length ) {
                System.arraycopy( values, 0, values, sc, len - sc );
                for( int i = 0; i < sc; i++ ) {
                    values[i] = new LongAdder();
                }

                len = sc;
            }

            for( int i = 0; i < len; i++ ) {
                values[i] = new LongAdder();
            }

            lastTick = ct;
        }
    }

    public final void inc() {
        inc( 1 );
    }


    public long[] get() {
        shift();

        val length = values.length;
        val ret = new long[length];
        for( int i = 0; i < length; i++ ) {
            ret[i] = values[i].longValue();
        }

        return ret;
    }
}
