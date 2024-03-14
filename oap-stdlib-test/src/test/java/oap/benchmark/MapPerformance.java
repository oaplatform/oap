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

package oap.benchmark;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Test( enabled = false )
public class MapPerformance {

    public static final int SAMPLES = 100000;
    public static final int COUNT = 100;

    @Test( enabled = false )
    public void test() {
        var map = new HashMap<String, Integer>();

        for( var i = 0; i < SAMPLES; i++ ) {
            map.put( "str" + i, i );
        }

        Benchmark.benchmark( "hashmap", SAMPLES, () -> {
            var res = 0;
            for( var i = 0; i < COUNT; i++ ) {
                res += map.get( "str" + i );
            }

        } ).run();

        var imap = Map.copyOf( map );
        Benchmark.benchmark( "imap", SAMPLES, () -> {
            var res = 0;
            for( var i = 0; i < COUNT; i++ ) {
                res += imap.get( "str" + i );
            }

        } ).run();
    }
}
