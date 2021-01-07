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

import org.joda.time.DateTimeUtils;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.lang.reflect.Array;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@NotThreadSafe
public abstract class ObjectHistogram<T extends Mergeable<T>> implements Serializable {
    private final T[] values;
    private volatile long lastTick;

    protected ObjectHistogram() {
        this( null, 0 );
    }

    public ObjectHistogram( T[] values, long lastTick ) {
        this.values = values;
        this.lastTick = lastTick;
    }

    @SuppressWarnings( "unchecked" )
    protected ObjectHistogram( Class<T> clazz, int count, long period ) {
        values = ( T[] ) Array.newInstance( clazz, count );
        for( int i = 0; i < count; i++ ) values[i] = null;

        lastTick = currentTick( period );
    }

    private long currentTick( long period ) {
        return DateTimeUtils.currentTimeMillis() / period;
    }

    public void update( long period, T value ) {
        shift( period );
        T obj = values[0];
        if( obj == null ) {
            values[0] = value;
        } else obj.merge( value );
    }

    public void update( long period, long time, T value ) {
        shift( period );

        int count = ( int ) ( ( DateTimeUtils.currentTimeMillis() - time ) / period );

        if( count >= values.length ) return;

        T obj = values[count];
        if( obj == null ) values[count] = value;
        else obj.merge( value );
    }

    private void shift( long period ) {
        long ct = currentTick( period );
        if( ct == lastTick ) return;
        ct = currentTick( period );
        if( ct == lastTick ) return;

        int sc = ( int ) ( ct - lastTick );
        int len = values.length;
        if( sc < values.length ) {
            System.arraycopy( values, 0, values, sc, len - sc );
            for( int i = 0; i < sc; i++ ) values[i] = null;
        } else {
            for( int i = 0; i < len; i++ ) values[i] = null;
        }

        lastTick = ct;
    }

    public T[] get( long period ) {
        shift( period );

        return values;
    }

    public void merge( long period, ObjectHistogram<T> update ) {
        synchronized( this ) {
            shift( period );
            update.shift( period );
            for( int i = 0; i < values.length; i++ ) {
                T thisValue = values[i];
                T updateValue = update.values[i];

                if( thisValue == null ) values[i] = updateValue;
                else if( updateValue != null )
                    thisValue.merge( updateValue );
            }
        }
    }
}
