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

import java.util.HashMap;

/**
 * Created by igor.petrenko on 2020-03-30.
 */
public class HashMaps {
    public static <K, V> HashMap<K, V> of() {
        return new HashMap<K, V>();
    }

    public static <K, V> HashMap<K, V> of( K key1, V value1 ) {
        var map = new HashMap<K, V>();
        map.put( key1, value1 );
        return map;
    }

    public static <K, V> HashMap<K, V> of( K key1, V value1, K key2, V value2 ) {
        var map = of( key1, value1 );
        map.put( key2, value2 );
        return map;
    }

    public static <K, V> HashMap<K, V> of( K key1, V value1, K key2, V value2, K key3, V value3 ) {
        var map = of( key1, value1, key2, value2 );
        map.put( key3, value3 );
        return map;
    }

    public static <K, V> HashMap<K, V> of( K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4 ) {
        var map = of( key1, value1, key2, value2, key3, value3 );
        map.put( key4, value4 );
        return map;
    }

    public static <K, V> HashMap<K, V> of( K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5 ) {
        var map = of( key1, value1, key2, value2, key3, value3, key4, value4 );
        map.put( key5, value5 );
        return map;
    }
}
