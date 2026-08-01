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

import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorsTest {
    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        ThreadPoolExecutor executor = Executors.newFixedBlockingThreadPool( 2 );

        AtomicInteger c = new AtomicInteger();

        long start = System.currentTimeMillis();

        CompletableFuture[] f = IntStream.range( 0, 5 ).mapToObj( i -> {
            System.out.println( ( System.currentTimeMillis() - start ) + " prerun - " + i );
            return CompletableFuture.runAsync( () -> {
                System.out.println( ( System.currentTimeMillis() - start ) + " run - " + i + " -> " + Thread.currentThread().getName() + "..." );
                Threads.sleepSafely( 400 );
                c.incrementAndGet();
                System.out.println( ( System.currentTimeMillis() - start ) + " run - " + i + " -> " + Thread.currentThread().getName() + "... Done." );
            }, executor );
        } ).toArray( CompletableFuture[]::new );

        CompletableFuture.allOf( f ).get( 10, TimeUnit.SECONDS );
    }

    @Test
    public void testNewFixedBlockingVirtualThreadPerTaskExecutorLimitsConcurrency() throws InterruptedException {
        int threads = 4;
        int tasks = 20;

        ExecutorService executor = Executors.newFixedBlockingVirtualThreadPerTaskExecutor( threads );

        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        CountDownLatch done = new CountDownLatch( tasks );

        for( int i = 0; i < tasks; i++ ) {
            executor.execute( () -> {
                int current = running.incrementAndGet();
                maxRunning.updateAndGet( max -> Math.max( max, current ) );
                try {
                    Thread.sleep( 20 );
                } catch( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                    done.countDown();
                }
            } );
        }

        assertThat( done.await( 10, TimeUnit.SECONDS ) ).isTrue();
        assertThat( maxRunning.get() ).isLessThanOrEqualTo( threads );

        executor.shutdown();
        assertThat( executor.awaitTermination( 10, TimeUnit.SECONDS ) ).isTrue();
    }
}
