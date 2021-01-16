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
        try( Pool<P> pool = new Pool<>( 2 ) {
            public P create() {
                return new P();
            }
        } ) {
            var p1 = pool.borrow();
            var p2 = pool.borrow();
            ForkJoinPool.commonPool().execute( () -> {
                Threads.sleepSafely( 150 );
                p1.close();
            } );
            assertThat( pool.borrow( 100, MILLISECONDS ).isEmpty() ).isTrue();
            Threads.sleepSafely( 100 );
            Poolable<P> p3 = pool.borrow();
            assertThat( p3 ).isSameAs( p1 );
            p2.close();
            p3.close();
        }
    }

    @Test
    public void borrowOne() {
        try( Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        } ) {
            var p = pool.borrow();
            ForkJoinPool.commonPool().execute( () -> {
                Threads.sleepSafely( 100 );
                p.close();
            } );
            Poolable<P> p2 = pool.borrow();
            assertThat( p2 ).isSameAs( p );
            p2.close();
        }
    }

    @Test
    public void resource() {
        try( Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        } ) {
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
    }

    @Test
    public void ifPresent() {
        try( Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }
        } ) {
            pool.borrow().ifPresent( p -> p.s += "+" );
            pool.borrow().ifPresent( p -> p.s += "+" );
            pool.borrow().ifPresent( p -> p.s += "+" );
            Poolable<P> p = pool.borrow();
            assertThat( p.get().s ).isEqualTo( "+++" );
            p.close();
        }
    }

    @Test
    public void invalid() {
        try( Pool<P> pool = new Pool<>( 1 ) {
            public P create() {
                return new P();
            }

            public boolean valid( P p ) {
                return !"invalid".equals( p.s );
            }

            public void discarded( P p ) {
                p.s = "discarded";
            }
        } ) {
            Poolable<P> p1 = pool.borrow();
            p1.ifPresent( p -> p.s = "invalid" );
            Poolable<P> p2 = pool.borrow();
            assertThat( p2 ).isNotSameAs( p1 );
            assertThat( p1.get().s ).isEqualTo( "discarded" );
            p2.close();
        }
    }

    @Test
    public void afterClose() {
        Pool<P> pool = new Pool<>( 3 ) {
            public P create() {
                return new P();
            }
        };
        pool.close();
        assertThat( pool.borrow().isEmpty() ).isTrue();
    }

    @Test
    public void close() {
        Poolable<P> p1;
        Poolable<P> p2;
        Poolable<P> p3;
        try( Pool<P> pool = new Pool<>( 3 ) {
            public P create() {
                return new P();
            }

            public void discarded( P p ) {
                p.s = "discarded";
            }
        } ) {
            p1 = pool.borrow();
            p2 = pool.borrow();
            p3 = pool.borrow();
            p1.close();
            p2.close();
            ForkJoinPool.commonPool().execute( () -> {
                Threads.sleepSafely( 100 );
                p3.close();
            } );
        }
        assertThat( p1.get().s ).isEqualTo( "discarded" );
        assertThat( p2.get().s ).isEqualTo( "discarded" );
        assertThat( p3.get().s ).isEqualTo( "discarded" );
    }

    private static class P {
        String s = "";
    }
}
