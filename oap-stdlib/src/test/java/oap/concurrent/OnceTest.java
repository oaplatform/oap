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

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class OnceTest extends AbstractTest {
    @Test
    public void executeOnce() {
        AtomicInteger count0 = new AtomicInteger();
        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();
        AtomicInteger count3 = new AtomicInteger();
        for( int i = 0; i < 10; i++ )
            Once.executeOnce( () -> {
                System.out.println( "x" );
                count0.incrementAndGet();
            } );
        for( int i = 0; i < 10; i++ ) Once.executeOnce( count1::incrementAndGet );
        for( int i = 0; i < 10; i++ ) Once.executeOnce( count2::incrementAndGet );
        Runnable incrementAndGet = count3::incrementAndGet;
        for( int i = 0; i < 10; i++ ) Once.executeOnce( incrementAndGet );
        Once.executeOnce( incrementAndGet );
        Once.executeOnce( incrementAndGet );
        assertThat( count0.get() ).isEqualTo( 1 );
        assertThat( count1.get() ).isEqualTo( 1 );
        assertThat( count2.get() ).isEqualTo( 1 );
        assertThat( count3.get() ).isEqualTo( 1 );
    }

    @Test
    public void once() {
        AtomicInteger count0 = new AtomicInteger();
        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();
        AtomicInteger count3 = new AtomicInteger();

        Runnable count0f = Once.once( () -> {
            System.out.println( "x" );
            count0.incrementAndGet();
        } );
        Runnable count1f = Once.once( count1::incrementAndGet );
        Runnable count2f = Once.once( count2::incrementAndGet );
        Runnable incrementAndGet = count3::incrementAndGet;
        Runnable count3f = Once.once( incrementAndGet );

        for( int i = 0; i < 10; i++ ) count0f.run();
        for( int i = 0; i < 10; i++ ) count1f.run();
        for( int i = 0; i < 10; i++ ) count2f.run();
        for( int i = 0; i < 10; i++ ) count3f.run();
        count3f.run();
        count3f.run();
        assertThat( count0.get() ).isEqualTo( 1 );
        assertThat( count1.get() ).isEqualTo( 1 );
        assertThat( count2.get() ).isEqualTo( 1 );
        assertThat( count3.get() ).isEqualTo( 1 );
    }
}
