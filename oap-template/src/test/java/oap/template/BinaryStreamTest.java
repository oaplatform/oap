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

package oap.template;

import oap.util.function.Try;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class BinaryStreamTest {
    @Test
    public void testTypes() throws Exception {
        check( true, BinaryOutputStream::writeBoolean, BinaryInputStream::readBoolean );
        check( false, BinaryOutputStream::writeBoolean, BinaryInputStream::readBoolean );

        check( ( byte ) 1, BinaryOutputStream::writeByte, BinaryInputStream::readByte );
        check( ( short ) 2, BinaryOutputStream::writeShort, BinaryInputStream::readShort );
        check( ( int ) 3, BinaryOutputStream::writeInt, BinaryInputStream::readInt );
        check( ( long ) 4, BinaryOutputStream::writeLong, BinaryInputStream::readLong );
        check( 5.5f, BinaryOutputStream::writeFloat, BinaryInputStream::readFloat );
        check( 6.6d, BinaryOutputStream::writeDouble, BinaryInputStream::readDouble );
        check( "test", BinaryOutputStream::writeString, BinaryInputStream::readString );
        check( new DateTime( 2022, 12, 12, 14, 3, UTC ), BinaryOutputStream::writeDateTime, BinaryInputStream::readDateTime );

        check( List.of( 1L, 2L, 3L ), BinaryOutputStream::writeList, BinaryInputStream::readList );
        check( List.of( "1", "2", "3" ), BinaryOutputStream::writeList, BinaryInputStream::readList );
        check( List.of( 1.1d, 2.2f, 3L, 4, List.of( "test", 4d ) ), BinaryOutputStream::writeList, BinaryInputStream::readList );
    }

    private <T> void check( T v,
                            Try.ThrowingBiConsumer<BinaryOutputStream, T> write,
                            Try.ThrowingFunction<BinaryInputStream, T> read ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputStream bos = new BinaryOutputStream( baos );
        write.accept( bos, v );

        T rv = read.apply( new BinaryInputStream( new ByteArrayInputStream( baos.toByteArray() ) ) );

        assertThat( v ).isEqualTo( rv );

        baos = new ByteArrayOutputStream();
        bos = new BinaryOutputStream( baos );
        bos.writeObject( v );

        Object rvo = ( new BinaryInputStream( new ByteArrayInputStream( baos.toByteArray() ) ) ).readObject();

        assertThat( v ).isEqualTo( rvo );
    }

    @Test
    public void testLines() throws IOException {
        var bytes = BinaryUtils.lines( List.of( List.of( 1L, "1" ), List.of( 2L, "" ) ) );
        var bais = new ByteArrayInputStream( bytes );
        var bis = new BinaryInputStream( bais );
        assertThat( bis.readObject() ).isEqualTo( 1L );
        assertThat( bis.readObject() ).isEqualTo( "1" );
        assertThat( bis.readObject() ).isEqualTo( BinaryInputStream.EOL );
        assertThat( bis.readObject() ).isEqualTo( 2L );
        assertThat( bis.readObject() ).isEqualTo( "" );
        assertThat( bis.readObject() ).isEqualTo( BinaryInputStream.EOL );
        assertThat( bis.readObject() ).isNull();
        assertThat( bis.readObject() ).isNull();
    }
}
