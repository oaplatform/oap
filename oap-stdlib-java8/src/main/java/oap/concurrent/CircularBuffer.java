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

import java.lang.reflect.Array;
import java.util.Arrays;

public class CircularBuffer<T> {
    private final Class<T> clazz;
    private final T[] data;
    private int index = 0;
    private boolean cycled = false;

    @SuppressWarnings( "unchecked" )
    public CircularBuffer( Class<T> clazz, int size ) {
        this.clazz = clazz;
        this.data = ( T[] ) Array.newInstance( clazz, size );
    }

    public synchronized void add( T element ) {
        if( !cycled && index == data.length ) cycled = true;
        this.data[index == data.length ? index = 0 : index] = element;
        index++;
    }

    @SuppressWarnings( "unchecked" )
    public synchronized T[] getElements() {
        if( cycled ) {
            T[] result = ( T[] ) Array.newInstance( clazz, data.length );
            System.arraycopy( data, index, result, 0, data.length - index );
            System.arraycopy( data, 0, result, data.length - index, index );
            return result;
        } else return Arrays.copyOf( data, index );
    }
}
