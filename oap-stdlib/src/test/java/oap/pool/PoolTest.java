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

package oap.pool;

import oap.concurrent.Threads;
import org.testng.annotations.Test;

import java.util.concurrent.ForkJoinPool;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class PoolTest {
    @Test
    public void borrow() {
        Pool<P> pool = new Pool<>( 2 ) {
            public P create() {
                return new P();
            }
        };
        var p = pool.borrow();
        var ignored = pool.borrow();
        ForkJoinPool.commonPool().execute( () -> {
            Threads.sleepSafely( 150 );
            p.close();
        } );
        assertThat( pool.borrow( 100, MILLISECONDS ).isEmpty() ).isTrue();
        Threads.sleepSafely( 100 );
        assertThat( pool.borrow() ).isSameAs( p );
    }

    @Test
    public void borrowOne() {
        Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        };
        var p = pool.borrow();
        ForkJoinPool.commonPool().execute( () -> {
            Threads.sleepSafely( 100 );
            p.close();
        } );
        assertThat( pool.borrow() ).isSameAs( p );

    }

    @Test
    public void resource() {
        Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        };
        try( var ignored = pool.borrow() ) {
            System.out.println( "borrow" );
        }
        try( var ignored = pool.borrow() ) {
            System.out.println( "borrow" );
        }
        try( var ignored = pool.borrow() ) {
            System.out.println( "borrow" );
        }
    }

    @Test
    public void ifPresent() {
        Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        };
        pool.borrow().ifPresent( p -> p.s += "+" );
        pool.borrow().ifPresent( p -> p.s += "+" );
        pool.borrow().ifPresent( p -> p.s += "+" );
        assertThat( pool.borrow().get().s ).isEqualTo( "+++" );
    }

    @Test
    public void invalid() {
        Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }

            public boolean valid( P p ) {
                return !"invalid".equals( p.s );
            }

            @Override
            public void discarded( P p ) {
                p.s = "discarded";
            }
        };
        Poolable<P> p1 = pool.borrow();
        p1.ifPresent( p -> p.s = "invalid" );
        assertThat( pool.borrow() ).isNotSameAs( p1 );
        assertThat( p1.get().s ).isEqualTo( "discarded" );
    }

    private static class P {
        String s = "";
    }
}
