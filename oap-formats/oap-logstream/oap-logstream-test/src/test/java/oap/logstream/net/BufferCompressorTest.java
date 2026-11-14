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

import oap.compression.Compression;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.template.Types;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BufferCompressorTest {
    private static final LogId LOG_ID = new LogId( "prefix", "type", Map.of( "p1", "v1" ),
        new String[] { "h1" }, new byte[][] { new byte[] { Types.STRING.id } } );

    private static byte[] decompress( LogStreamCompression compression, byte[] data, int offset ) throws IOException {
        return switch( compression ) {
            case GZIP -> Compression.ungzip( data, offset, data.length - offset );
            case LZ4 -> Compression.unlz4( data, offset, data.length - offset );
            case ZSTD -> Compression.unzstd( data, offset, data.length - offset );
        };
    }

    private static Buffer buffer( byte[] content ) {
        Buffer buffer = new Buffer( content.length + 1024, LOG_ID, ROW_BINARY_V4 );
        assertThat( buffer.put( content ) ).isTrue();
        buffer.close( 1L );
        return buffer;
    }

    private static byte[] text( int size ) {
        StringBuilder sb = new StringBuilder( size + 64 );
        Random random = new Random( 1 );
        while( sb.length() < size ) {
            sb.append( "line " ).append( random.nextInt( 1000 ) ).append( "\tvalue-" ).append( random.nextInt( 100 ) ).append( '\n' );
        }
        return Arrays.copyOf( sb.toString().getBytes( UTF_8 ), size );
    }

    private static byte[] random( int size ) {
        byte[] bytes = new byte[size];
        new Random( 2 ).nextBytes( bytes );
        return bytes;
    }

    private static void assertCompressed( BufferCompressor compressor, LogStreamCompression compression, int level, byte[] content ) throws IOException {
        Buffer buffer = buffer( content );
        int headerLength = buffer.headerLength();

        byte[] data = compressor.compress( buffer, compression, level );

        assertThat( Arrays.copyOf( data, Buffer.DATE_LENGTH_POSITION ) ).isEqualTo( Arrays.copyOf( buffer.data(), Buffer.DATE_LENGTH_POSITION ) );
        assertThat( ByteBuffer.wrap( data, Buffer.DATE_LENGTH_POSITION, 4 ).getInt() ).isEqualTo( data.length - headerLength );
        assertThat( data[Buffer.COMPRESSION_POSITION] ).isEqualTo( compression.id );
        assertThat( Arrays.copyOfRange( data, Buffer.COMPRESSION_POSITION + 1, headerLength ) )
            .isEqualTo( Arrays.copyOfRange( buffer.data(), Buffer.COMPRESSION_POSITION + 1, headerLength ) );

        assertThat( decompress( compression, data, headerLength ) ).isEqualTo( content );
    }

    @DataProvider
    public Object[][] compressions() {
        return new Object[][] {
            { LogStreamCompression.GZIP, 1 },
            { LogStreamCompression.GZIP, 9 },
            { LogStreamCompression.LZ4, 0 },
            { LogStreamCompression.LZ4, 9 },
            { LogStreamCompression.ZSTD, -5 },
            { LogStreamCompression.ZSTD, -1 },
            { LogStreamCompression.ZSTD, 3 },
        };
    }

    @Test( dataProvider = "compressions" )
    public void roundTrip( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            assertCompressed( compressor, compression, level, text( 100_000 ) );
        }
    }

    @Test( dataProvider = "compressions" )
    public void reuse( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            assertCompressed( compressor, compression, level, text( 200_000 ) );
            assertCompressed( compressor, compression, level, text( 10 ) );
            assertCompressed( compressor, compression, level, random( 50_000 ) );
            assertCompressed( compressor, compression, level, text( 300_000 ) );
        }
    }

    @Test( dataProvider = "compressions" )
    public void empty( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            assertCompressed( compressor, compression, level, new byte[0] );
        }
    }

    @Test( dataProvider = "compressions" )
    public void largerThanLz4Block( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            assertCompressed( compressor, compression, level, text( 9 * 1024 * 1024 + 17 ) );
        }
    }

    @Test( dataProvider = "compressions" )
    public void incompressible( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            assertCompressed( compressor, compression, level, random( 5 * 1024 * 1024 ) );
        }
    }

    @Test
    public void switchCodecAndLevel() throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            byte[] content = text( 500_000 );
            for( Object[] c : compressions() ) {
                assertCompressed( compressor, ( LogStreamCompression ) c[0], ( int ) c[1], content );
            }
            for( Object[] c : compressions() ) {
                assertCompressed( compressor, ( LogStreamCompression ) c[0], ( int ) c[1], content );
            }
        }
    }

    @Test
    public void invalidLevel() {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            Buffer buffer = buffer( text( 10 ) );
            assertThatThrownBy( () -> compressor.compress( buffer, LogStreamCompression.GZIP, 0 ) )
                .isInstanceOf( IllegalArgumentException.class );
            assertThatThrownBy( () -> compressor.compress( buffer, LogStreamCompression.LZ4, 18 ) )
                .isInstanceOf( IllegalArgumentException.class );
            assertThatThrownBy( () -> compressor.compress( buffer, LogStreamCompression.ZSTD, 23 ) )
                .isInstanceOf( IllegalArgumentException.class );
        }
    }

    /**
     * the frame carries the content checksum: a flipped byte in stored (incompressible) content is detected on decompression
     */
    @Test( dataProvider = "compressions" )
    public void contentChecksum( LogStreamCompression compression, int level ) throws IOException {
        try( BufferCompressor compressor = new BufferCompressor() ) {
            Buffer buffer = buffer( random( 100_000 ) );
            int headerLength = buffer.headerLength();
            byte[] data = compressor.compress( buffer, compression, level );

            switch( compression ) {
                case LZ4 -> assertThat( data[headerLength + 4] & 0x04 ).as( "FLG content checksum" ).isNotZero();
                case ZSTD -> assertThat( data[headerLength + 4] & 0x04 ).as( "FHD content checksum" ).isNotZero();
                case GZIP -> { }
            }

            int middle = headerLength + ( data.length - headerLength ) / 2;
            data[middle] ^= 0x55;

            assertThatThrownBy( () -> decompress( compression, data, headerLength ) ).isInstanceOf( IOException.class );
        }
    }
}
