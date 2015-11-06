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

import oap.io.Fifo.Entries;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class FifoTest extends AbstractTest {

    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        DateTimeUtils.setCurrentMillisFixed( 1 );
    }

    @Test
    public void testPeek() throws IOException {
        try( Fifo fifo = Fifo.newSegment( Env.tmpPath( "segadd.seg" ), 30 ) ) {
            byte[] entry = "1111".getBytes();
            assertTrue( fifo.add( entry ) );

            Entries entries = fifo.peek();
            assertEquals( entries, Lists.of( entry ) );
            assertEquals( fifo.peek(), Lists.of( entry ) );
            assertFalse( fifo.isEmpty() );

            entries.commit();

            assertEquals( fifo.peek(), Collections.emptyList() );
            assertTrue( fifo.isEmpty() );
            assertEquals( fifo.used(), 0 );
            assertEquals( fifo.free(), 30 );
        }
    }

    @Test
    public void optimizeAfterPeek() throws Exception {
        try( Fifo fifo = Fifo.newSegment( Env.tmpPath( "segoptafterpoll.seg" ), 30 ) ) {
            assertTrue( fifo.add( "111".getBytes() ) );

            fifo.peek().commit();

            assertEquals( fifo.head, 4 + 4 + 4 );
            assertEquals( fifo.tail1, 4 + 4 + 4 );
            assertTrue( fifo.isEmpty() );
        }
    }

    @Test
    public void tail() throws Exception {
        byte[] e1 = "111".getBytes();
        byte[] e2 = "222".getBytes();
        byte[] e3 = "333".getBytes();

        int length = Fifo.sizeOf( e1 ) + Fifo.sizeOf( e2 ) + Fifo.BODY_POSITION;
        try( Fifo fifo = Fifo.newSegment( Env.tmpPath( "tail.seg" ), length ) ) {
            fifo.add( e1 );
            fifo.add( e2 );
            assertFalse( fifo.isEmpty() );
            assertFalse( fifo.add( e3 ) );

            Entries entries = fifo.peek( 1 );
            entries.commit();
            assertEquals( entries, Lists.of( e1 ) );
            assertFalse( fifo.isEmpty() );

            assertTrue( fifo.add( e3 ) );
            assertEquals( fifo.used(), Fifo.sizeOf( e2 ) + Fifo.sizeOf( e3 ) );
            assertEquals( fifo.free(), length - Fifo.sizeOf( e2 ) - Fifo.sizeOf( e3 ) );

            Entries entries2 = fifo.peek();
            entries2.commit();
            assertEquals( entries2, Arrays.asList( e2, e3 ) );
            assertTrue( fifo.isEmpty() );
        }
    }

    @Test
    public void reload() throws IOException {
        try( Fifo fifo = Fifo.newSegment( Env.tmpPath( "reload.seg" ), 30 ) ) {
            fifo.add( "111".getBytes() );
        }

        try( Fifo fifo = Fifo.newSegment( Env.tmpPath( "reload.seg" ), 30 ) ) {
            List<byte[]> entries = fifo.peek().commit();
            assertEquals( entries, Lists.of( "111".getBytes() ) );
        }
    }
}
