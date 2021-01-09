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

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static oap.benchmark.Benchmark.benchmark;

@Test( enabled = false )
public class HashMapPerformance {
    @Test
    public void testComputeIfAbsentVsGet() {
        HashMap<String, String> map = new HashMap<>();

        int samples = 5000000;

        benchmark( "computeIfAbsent", samples, i -> map.computeIfAbsent( "key" + i, k -> "key" ) ).run();

        benchmark( "get", samples, i -> map.get( "key" + i ) ).run();
    }

    @Test
    public void testMultiStringKey() {
        int samples = 10000000;

        String[] randoms = IntStream.range( 0, 99 ).mapToObj( i -> RandomStringUtils.random( 5, true, false ) ).toArray( String[]::new );

        HashMap<List<String>, Object> map1 = new HashMap<>();
        HashMap<String, Object> map2 = new HashMap<>();

        for( int i = 0; i < randoms.length; i += 3 ) {
            map1.put( java.util.Arrays.asList( randoms[i], randoms[i + 1], randoms[i + 2] ), 1 );
            map2.put( randoms[i] + randoms[i + 1] + randoms[i + 2], 1 );
        }

        benchmark( "list-string-key", samples, i -> {
            int p = ( i % 33 ) * 3;

            map1.get( java.util.Arrays.asList( randoms[p], randoms[p + 1], randoms[p + 2] ) );
        } ).run();

        benchmark( "one-string-key", samples, i -> {
            int p = ( i % 33 ) * 3;

            map2.get( randoms[p] + randoms[p + 1] + randoms[p + 2] );
        } ).run();
    }
}
