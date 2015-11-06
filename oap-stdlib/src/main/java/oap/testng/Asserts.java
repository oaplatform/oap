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
package oap.testng;

import com.google.common.base.Throwables;
import oap.concurrent.Threads;
import oap.util.Try;
import org.testng.Assert;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.testng.Assert.fail;

public final class Asserts {
    public static void assertException( Class<? extends Throwable> e, Runnable code ) {
        try {
            code.run();
            Assert.fail( e.getName() + " should be thrown" );
        } catch( Throwable t ) {
            if( !e.isInstance( t ) ) throw t;
        }
    }

    public static void assertException( Throwable e, Runnable code ) {
        try {
            code.run();
            Assert.fail( e + " should be thrown" );
        } catch( Throwable t ) {
            Assert.assertEquals( t.getClass(), e.getClass() );
            Assert.assertEquals( t.getCause(), e.getCause() );
            Assert.assertEquals( t.getMessage(), e.getMessage() );
        }
    }

    public static void assertEventually( long retryTimeout, int retries, Try.ThrowingRunnable asserts ) {
        boolean passed = false;
        Throwable exception = null;
        int count = retries;

        while( !passed && count >= 0 ) {
            try {
                asserts.run();
                passed = true;
            } catch( Throwable e ) {
                exception = e;
                Threads.sleepSafely( retryTimeout );
                count--;
            }
        }
        if( !passed )
            if( exception != null ) Throwables.propagate( exception );
            else throw new AssertionError( "timeout" );
    }

    public static <A> void assertEquals( Stream<? extends A> actual, Stream<? extends A> expected ) {
        if( actual == null && expected != null ) fail( "actual stream is null" );
        else if( actual != null && expected != null )
            Assert.assertEquals( actual.collect( toList() ), expected.collect( toList() ) );
    }

    public static void assertEquals( int[] actual, int[] expected ) {
        Assert.assertNotNull( actual );
        Assert.assertNotNull( expected );
        Assert.assertEquals( actual.length, expected.length, "array length" );
        for( int i = 0; i < actual.length; i++ ) {
            Assert.assertEquals( actual[i], expected[i], " at index " + i );
        }

    }
}
