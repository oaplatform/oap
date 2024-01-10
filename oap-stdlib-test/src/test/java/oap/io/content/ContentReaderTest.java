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

package oap.io.content;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.content.ContentReader.ofBytes;
import static oap.io.content.ContentReader.ofInputStream;
import static oap.io.content.ContentReader.ofJson;
import static oap.io.content.ContentReader.ofLines;
import static oap.io.content.ContentReader.ofLinesConsumer;
import static oap.io.content.ContentReader.ofLinesStream;
import static oap.io.content.ContentReader.ofProperties;
import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.bytesOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class ContentReaderTest {
    @Test
    public void read() {
        byte[] content = "test1\ntest2\n".getBytes( UTF_8 );
        Assertions.assertThat( ContentReader.read( content, ofInputStream() ) )
            .hasContent( new String( content ) );
        assertThat( ContentReader.read( content, ofBytes() ) )
            .isEqualTo( content );
        assertThat( ContentReader.read( content, ofString() ) )
            .isEqualTo( new String( content ) );
        assertThat( ContentReader.read( content, ofLinesStream() ) )
            .containsExactly( "test1", "test2" );
        assertThat( ContentReader.read( content, ofLines() ) )
            .containsExactly( "test1", "test2" );
        assertThat( ContentReader.read( bytesOfTestResource( getClass(), "test.properties" ), ofProperties() ) )
            .contains( entry( "a", "b" ), entry( "c", "d" ) );
        assertThat( ContentReader.read( "{\"s\":\"aaa\"}".getBytes( UTF_8 ), ofJson( Bean.class ) ) )
            .isEqualTo( new Bean() );
    }

    @Test
    public void testOfLinesConsumer() {
        byte[] content = "test1\ntest2\n".getBytes( UTF_8 );

        var arr = new ArrayList<String>();

        ContentReader.read( content, ofLinesConsumer( arr::add ) );

        assertThat( arr ).containsExactly( "test1", "test2" );
    }

    @Test
    public void testOfBytesConsumer() {
        byte[] content = "test1\ntest2\n".getBytes( UTF_8 );

        var out = new ByteArrayOutputStream();

        byte[] buffer = new byte[2];
        ContentReader.read( content, ofBytes( buffer, size -> out.write( buffer, 0, size ) ) );

        assertThat( out.toByteArray() ).isEqualTo( content );
    }

    @Test
    public void andThen() {
        assertThat( ContentReader.<String>read( "{\"s\":\"aaa\"}".getBytes( UTF_8 ),
            ofJson( Bean.class ).andThen( b -> b.s ) ) )
            .isEqualTo( "aaa" );
    }

    @EqualsAndHashCode
    @ToString
    public static class Bean {
        String s = "aaa";
    }
}
