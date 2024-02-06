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

package oap.storage;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Test {
    public static void main( String[] args ) throws ExecutionException, InterruptedException {
        var pool = new ThreadPoolExecutor( 2, 2, 1, TimeUnit.MINUTES, new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat( "request-processor-%d" ).build(), new ThreadPoolExecutor.AbortPolicy() );
        List<Future> futures = new ArrayList<>();
        for( int i = 0; i < 200; i++ ) {
            int finalI = i;
            try {
                futures.add( CompletableFuture.runAsync( new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        Thread.sleep( 2000L );
                        System.out.println( "XXXXX:" + finalI );
                    }
                }, pool ) );
            } catch( RejectedExecutionException e ) {
                System.out.println( "rj - " + i );
            }
        }
        for( var i = 0; i < futures.size(); i++ ) {
            var future = futures.get( i );
            try {
                future.get();
                System.out.println( "ok - " + i );
            } catch( InterruptedException e ) {
                System.out.println( "in - " + i );
            } catch( ExecutionException e ) {
                System.out.println( "execution - " + i );
            }
        }
        System.out.println( "done" );
        pool.shutdownNow();
    }
}
