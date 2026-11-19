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

package oap.message.client;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.apache.commons.codec.digest.DigestUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class SpoolSegmentTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public SpoolSegmentTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    private static SpoolSegment write( Path path, String... messages ) throws IOException {
        Files.createDirectories( path.getParent() );
        SpoolSegment segment = new SpoolSegment( path, 0 );
        try( SpoolSegment.Writer writer = segment.writer() ) {
            long time = 1000;
            for( String message : messages ) {
                byte[] data = ( "--" + message + "--" ).getBytes( UTF_8 );
                // offset / length: only the message without the dashes
                writer.append( ( byte ) 1, ( short ) 2, time++, DigestUtils.md5( message ), data, 2, data.length - 4 );
            }
            writer.force();
        }
        return segment;
    }

    private static List<String> read( SpoolSegment segment, boolean corrupted ) throws IOException {
        List<String> result = new ArrayList<>();
        try( SpoolSegment.Reader reader = segment.reader() ) {
            SpoolSegment.Record record;
            while( ( record = reader.next() ) != null ) {
                String data = new String( record.data(), UTF_8 );
                assertThat( record.messageType() ).isEqualTo( ( byte ) 1 );
                assertThat( record.version() ).isEqualTo( ( short ) 2 );
                assertThat( record.md5() ).isEqualTo( DigestUtils.md5( data ) );
                result.add( data );
            }
            assertThat( reader.corrupted ).isEqualTo( corrupted );
        }
        return result;
    }

    @Test
    public void roundTrip() throws IOException {
        SpoolSegment segment = write( testDirectoryFixture.testPath( "p0/1.seg" ), "a", "bb", "" );

        assertThat( segment.records ).isEqualTo( 3 );
        assertThat( segment.firstTime ).isEqualTo( 1000 );
        assertThat( segment.bytes ).isEqualTo( Files.size( segment.path ) );
        assertThat( read( segment, false ) ).containsExactly( "a", "bb", "" );

        SpoolSegment scanned = new SpoolSegment( segment.path, 0 );
        SpoolSegment.scan( scanned );
        assertThat( scanned.records ).isEqualTo( 3 );
        assertThat( scanned.firstTime ).isEqualTo( 1000 );
        assertThat( scanned.bytes ).isEqualTo( segment.bytes );
    }

    @Test
    public void crc() throws IOException {
        SpoolSegment segment = write( testDirectoryFixture.testPath( "p0/1.seg" ), "first", "second", "third" );

        // a byte of the data of the second record
        long position = SpoolSegment.HEADER_LENGTH
            + SpoolSegment.RECORD_HEADER_LENGTH + "first".length() + SpoolSegment.CRC_LENGTH
            + SpoolSegment.RECORD_HEADER_LENGTH + 1;
        try( FileChannel channel = FileChannel.open( segment.path, StandardOpenOption.WRITE ) ) {
            channel.write( java.nio.ByteBuffer.wrap( new byte[] { 'X' } ), position );
        }

        assertThat( read( segment, true ) ).containsExactly( "first" );
    }

    @Test
    public void tornTail() throws IOException {
        SpoolSegment segment = write( testDirectoryFixture.testPath( "p0/1.seg" ), "first", "second", "third" );
        try( FileChannel channel = FileChannel.open( segment.path, StandardOpenOption.WRITE ) ) {
            channel.truncate( channel.size() - 1 );
        }

        assertThat( read( segment, true ) ).containsExactly( "first", "second" );

        SpoolSegment scanned = new SpoolSegment( segment.path, 0 );
        SpoolSegment.scan( scanned );
        assertThat( scanned.records ).isEqualTo( 2 );
    }

    @Test
    public void invalidHeader() throws IOException {
        Path path = testDirectoryFixture.testPath( "p0/1.seg" );
        Files.createDirectories( path.getParent() );
        Files.write( path, "garbage-garbage-garbage-garbage-garbage".getBytes( UTF_8 ) );

        SpoolSegment segment = new SpoolSegment( path, 0 );
        assertThat( read( segment, true ) ).isEmpty();
        SpoolSegment.scan( segment );
        assertThat( segment.records ).isEqualTo( 0 );
    }

    @Test
    public void ackAndDelete() {
        SpoolSegment segment = new SpoolSegment( Path.of( "x.seg" ), 0 );
        segment.records = 2;

        segment.read();
        segment.read();
        assertThat( segment.ack() ).isFalse();
        assertThat( segment.readDone() ).isFalse();
        assertThat( segment.pending() ).isEqualTo( 1 );
        assertThat( segment.ack() ).isTrue();
        assertThat( segment.pending() ).isEqualTo( 0 );
    }
}
