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

package oap.io;

import oap.testng.AbstractTest;
import oap.testng.Env;
import org.apache.commons.lang3.ArrayUtils;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class ByteFifoTest extends AbstractTest {

    @Test
    public void polling() throws IOException {
        try( ByteFifo fifo = ByteFifo.open( Env.tmpPath( "polling.data" ), 30 ) ) {
            byte[] value = "1111".getBytes();
            assertTrue( fifo.offer( value ) );

            fifo.polling( bytes -> assertEquals( bytes, value ) );
            assertTrue( fifo.isEmpty() );

            fifo.polling( bytes -> assertEquals( bytes.length, 0 ) );
            assertTrue( fifo.isEmpty() );
            assertEquals( fifo.used(), 0 );
            assertEquals( fifo.free(), 30 );
        }
    }

    @Test
    public void optimizeAfterPoll() throws Exception {
        try( ByteFifo fifo = ByteFifo.open( Env.tmpPath( "optimize.data" ), 30 ) ) {
            assertTrue( fifo.offer( "111".getBytes() ) );

            fifo.polling( bytes -> {
            } );

            assertEquals( fifo.head, 4 + 4 + 4 );
            assertEquals( fifo.tail1, 4 + 4 + 4 );
            assertTrue( fifo.isEmpty() );
        }
    }

    @Test
    public void complex() throws Exception {
        byte[] e1 = "111".getBytes();
        byte[] e2 = "222".getBytes();
        byte[] e3 = "333".getBytes();

        int length = ByteFifo.sizeOf( e1 ) + ByteFifo.sizeOf( e2 ) + ByteFifo.BODY_POSITION;
        try( ByteFifo fifo = ByteFifo.open( Env.tmpPath( "complex.seg" ), length ) ) {
            assertTrue( fifo.offer( e1 ) );
            assertTrue( fifo.offer( e2 ) );
            assertFalse( fifo.isEmpty() );
            assertFalse( fifo.offer( e3 ) );

            fifo.polling( 1, bytes -> assertEquals( bytes, e1 ) );
            assertFalse( fifo.isEmpty() );

            assertTrue( fifo.offer( e3 ) );
            assertEquals( fifo.used(), ByteFifo.sizeOf( e2 ) + ByteFifo.sizeOf( e3 ) );
            assertEquals( fifo.free(), length - ByteFifo.sizeOf( e2 ) - ByteFifo.sizeOf( e3 ) );

            fifo.polling( bytes -> assertEquals( bytes, ArrayUtils.addAll( e2, e3 ) ) );
            assertTrue( fifo.isEmpty() );
        }
    }

    @Test
    public void persistent() throws IOException {
        byte[] data = "111".getBytes();
        try( ByteFifo fifo = ByteFifo.open( Env.tmpPath( "fifo.data" ), 30 ) ) {
            fifo.offer( data );
        }

        try( ByteFifo fifo = ByteFifo.open( Env.tmpPath( "fifo.data" ), 30 ) ) {
            assertEquals( fifo.poll(), data );
            assertTrue( fifo.isEmpty() );
        }
    }


}
