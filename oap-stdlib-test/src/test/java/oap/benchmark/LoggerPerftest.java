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

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import java.util.Random;

import static oap.benchmark.Benchmark.benchmark;

@Slf4j
@Test( enabled = false )
public class LoggerPerftest {
    @Test( enabled = false )
    public void samePerformance() {
        Random random = new Random();

        benchmark( "slf4j-trace-call", 10000000, () -> {
            int r1 = random.nextInt();
            int r2 = random.nextInt();
            String s1 = String.valueOf( random.nextInt() );
            String s2 = String.valueOf( random.nextInt() );
            log.trace( "test {}, {}, {}, {}", r1, r2, s1, s2 );
        } ).run();
        benchmark( "slf4j-trace-if", 10000000, () -> {
            int r1 = random.nextInt();
            int r2 = random.nextInt();
            String s1 = String.valueOf( random.nextInt() );
            String s2 = String.valueOf( random.nextInt() );
            if( log.isTraceEnabled() ) log.trace( "test {}, {}, {}, {}", r1, r2, s1, s2 );
        } ).run();
    }
}
