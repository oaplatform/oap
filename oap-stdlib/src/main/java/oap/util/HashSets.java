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

import java.util.HashSet;

public class HashSets {
    public static <T> HashSet<T> of() {
        return new HashSet<>();
    }

    public static <T> HashSet<T> of( T value1 ) {
        HashSet<T> set = new HashSet<>();
        set.add( value1 );
        return set;
    }

    public static <T> HashSet<T> of( T value1, T value2 ) {
        HashSet<T> set = of( value1 );
        set.add( value2 );
        return set;
    }

    public static <T> HashSet<T> of( T value1, T value2, T value3 ) {
        HashSet<T> set = of( value1, value2 );
        set.add( value3 );
        return set;
    }

    public static <T> HashSet<T> of( T value1, T value2, T value3, T value4 ) {
        HashSet<T> set = of( value1, value2, value3 );
        set.add( value4 );
        return set;
    }

    public static <T> HashSet<T> of( T value1, T value2, T value3, T value4, T value5 ) {
        HashSet<T> set = of( value1, value2, value3, value4 );
        set.add( value5 );
        return set;
    }

    public static <T> HashSet<T> of( T... values ) {
        HashSet<T> set = of();
        for( var v : values ) set.add( v );
        return set;
    }
}
