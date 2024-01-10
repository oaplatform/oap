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

package oap.io.io;

import oap.io.FixedLengthArrayOutputStream;
import org.testng.annotations.Test;

import java.nio.BufferOverflowException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class FixedLengthArrayOutputStreamTest {
    @Test
    public void testByte() {
        var bytes = new byte[4];
        var out = new FixedLengthArrayOutputStream( bytes );

        out.write( 1 );
        assertThat( out.array() ).isEqualTo( new byte[] { 1, 0, 0, 0 } );

        out.write( 2 );
        out.write( 3 );
        out.write( 4 );
        assertThatThrownBy( () -> out.write( 5 ) ).isInstanceOf( BufferOverflowException.class );

        assertThat( out.size() ).isEqualTo( 4 );
        assertThat( out.array() ).isEqualTo( new byte[] { 1, 2, 3, 4 } );
    }

    @Test
    public void testBytes() {
        var bytes = new byte[5];
        var out = new FixedLengthArrayOutputStream( bytes );

        out.write( new byte[] { 1, 2 } );
        assertThat( out.array() ).isEqualTo( new byte[] { 1, 2, 0, 0, 0 } );

        out.write( new byte[] { -1, 3, 4, 5 }, 1, 2 );
        assertThat( out.array() ).isEqualTo( new byte[] { 1, 2, 3, 4, 0 } );

        assertThatThrownBy( () -> out.write( new byte[] { 2, 3 } ) ).isInstanceOf( BufferOverflowException.class );

        assertThat( out.size() ).isEqualTo( 4 );
        assertThat( out.array() ).isEqualTo( new byte[] { 1, 2, 3, 4, 0 } );
    }
}
