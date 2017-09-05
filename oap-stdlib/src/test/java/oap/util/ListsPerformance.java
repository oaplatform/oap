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

import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListsPerformance extends AbstractPerformance {
    @Test
    public void testAllMatch() {
        final List<String> list = IntStream.range( 0, 10 ).mapToObj( i -> "sdfsdf" + i ).collect( Collectors.toList() );

        final AtomicBoolean res = new AtomicBoolean();

        final int samples = 100000000;
        benchmark( "stream-allMatch", samples, () -> {
            final boolean b = list.stream().allMatch( ( v ) -> v.length() < 1000 );
            res.compareAndSet( b, b );
        } ).run();

        benchmark( "foreach-allMatch", samples, () -> {
            final boolean b = Lists.allMatch( list, v -> v.length() < 1000 );
            res.compareAndSet( b, b );
        } ).run();
    }

    @Test
    public void testFilter() {
        final List<Integer> list = IntStream.range( 0, 10 ).boxed().collect( Collectors.toList() );

        final AtomicInteger res = new AtomicInteger();

        final int samples = 100000000;
        benchmark( "stream-filter", samples, () -> {
            final int b = list.stream().filter( ( v ) -> v < 6 ).collect( Collectors.toList() ).size();
            res.addAndGet( b );
        } ).run();

        benchmark( "foreach-filter", samples, () -> {
            final int b = Lists.filter( list, ( v ) -> v < 6 ).size();
            res.addAndGet( b );
        } ).run();
    }
}
