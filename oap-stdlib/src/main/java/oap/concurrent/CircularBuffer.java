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

package oap.concurrent;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by anton on 8/22/16.
 */
@ThreadSafe
public class CircularBuffer<T> {
    private final T[] data;
    private final AtomicInteger index;
    private final int size;

    @SuppressWarnings( "unchecked" )
    public CircularBuffer( int size ) {
        this.size = size;
        index = new AtomicInteger();
        data = ( T[] ) new Object[size];
    }

    public void add( T element ) {
        final int idx = index.updateAndGet( ( i ) -> i + 1 >= size ? 0 : i + 1 );
        data[idx] = element;
    }

    public T[] getElements() {
        return Arrays.copyOf( data, size );
    }
}
