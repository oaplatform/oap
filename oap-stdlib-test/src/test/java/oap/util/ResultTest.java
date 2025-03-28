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

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ResultTest {
    @Test( expectedExceptions = InterruptedException.class )
    public void catchingInterruptible() throws InterruptedException {
        Result.catchingInterruptible( () -> {
            throw new InterruptedException( "Somebody interrupted me" );
        } );
    }

    @Test
    public void catchingInterruptibleSuccess() throws InterruptedException {
        Result<String, Throwable> result = Result.catchingInterruptible( () -> "im ok" );
        assertTrue( result.isSuccess() );
        assertEquals( result.successValue, "im ok" );
    }

    @Test
    public void catchingInterruptibleException() throws InterruptedException {
        Result<String, Throwable> result = Result.catchingInterruptible( () -> {
            throw new IllegalArgumentException( "some reason" );
        } );

        assertFalse( result.isSuccess() );
        assertEquals( result.failureValue.getClass(), IllegalArgumentException.class );
    }

    @Test
    public void resultToString() {
        assertThat( Result.success( "aaaa" ).toString() ).isEqualTo( "Result.success (reason: aaaa)" );
        assertThat( Result.failure( "aaaa" ).toString() ).isEqualTo( "Result.failure (reason: aaaa)" );
    }
}
