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

package oap.perf;

import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.Random;

@Slf4j
public class LoggerPerformance extends AbstractPerformance {
    @Test
    public void testSlf4j() {
        final Random random = new Random();

        benchmark( "slf4j-trace-call", 10000000, () -> {
            final int r1 = random.nextInt();
            final int r2 = random.nextInt();
            final String s1 = String.valueOf( random.nextInt() );
            final String s2 = String.valueOf( random.nextInt() );
            log.trace( "test {}, {}, {}, {}", r1, r2, s1, s2 );
        } ).run();
        benchmark( "slf4j-trace-if", 10000000, () -> {
            final int r1 = random.nextInt();
            final int r2 = random.nextInt();
            final String s1 = String.valueOf( random.nextInt() );
            final String s2 = String.valueOf( random.nextInt() );
            if( log.isTraceEnabled() ) log.trace( "test {}, {}, {}, {}", r1, r2, s1, s2 );
        } );
    }
}
