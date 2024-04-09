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

package oap.logstream.net;

import oap.logstream.LogId;
import oap.template.Types;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static oap.logstream.LogStreamProtocol.ProtocolVersion.BINARY_V2;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

public class BufferTest {

    @Test
    public void data() throws IOException {
        var buffer = new Buffer( 200, new LogId( "s", "l", "h", Map.of(),
            new String[] { "h1" }, new byte[][] { new byte[] { Types.STRING.id } } ), BINARY_V2 );

        assertTrue( buffer.putInt( 10 ) );
        assertTrue( buffer.putLong( 10 ) );
        assertTrue( buffer.putUTF( "aaaa" ) );
        assertTrue( buffer.putInt( 20 ) );
        assertTrue( buffer.putInt( 30 ) );

        buffer.close( 1 );

        var dis = new DataInputStream( new ByteArrayInputStream( Arrays.copyOf( buffer.data(), buffer.length() ) ) );

        assertThat( dis.readLong() ).isEqualTo( 1L );
        assertThat( dis.readInt() ).isEqualTo( 26 );
        assertString( dis.readUTF() ).isEqualTo( "s" ); // filePrefixPattern
        assertString( dis.readUTF() ).isEqualTo( "l" ); // logType
        assertString( dis.readUTF() ).isEqualTo( "h" ); // clientHostname
        assertThat( dis.readInt() ).isEqualTo( 1 ); // headers size
        assertString( dis.readUTF() ).isEqualTo( "h1" ); // header 0
        assertThat( dis.readByte() ).isEqualTo( ( byte ) 1 ); // type 0 size
        assertThat( dis.readByte() ).isEqualTo( Types.STRING.id ); // type id
        assertThat( dis.readByte() ).isEqualTo( ( byte ) 0 ); // properties size
        assertThat( dis.readInt() ).isEqualTo( 10 );
        assertThat( dis.readLong() ).isEqualTo( 10L );
        assertString( dis.readUTF() ).isEqualTo( "aaaa" );
        assertThat( dis.readInt() ).isEqualTo( 20 );
        assertThat( dis.readInt() ).isEqualTo( 30 );
        assertThat( dis.read() ).isEqualTo( -1 );
    }
}
