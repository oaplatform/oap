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
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.Types;
import oap.util.FastByteArrayOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V4;

/**
 * single thread, ~1 MB RowBinary buffer; compress/decompress MB/s (of uncompressed data) and ratio
 */
@Test( enabled = false )
public class BufferCompressionPerftest {
    private static final int SIZE = 1024 * 1024;
    private static final int WARMING = 200;
    private static final int SAMPLES = 500;
    private static final String[] COUNTRIES = { "US", "UA", "DE", "GB", "FR", "PL", "IN", "BR" };
    private static final String[] DEVICES = { "phone", "tablet", "desktop", "ctv" };

    private interface Codec {
        byte[] compress( Buffer buffer ) throws IOException;
    }

    private static Buffer rowBinaryBuffer() throws IOException {
        String[] headers = { "TIMESTAMP", "REQUEST_ID", "COUNTRY", "DEVICE", "BID", "PRICE", "DOMAIN", "SEGMENTS" };
        byte[][] types = new byte[headers.length][];
        for( int i = 0; i < types.length; i++ ) types[i] = new byte[] { Types.STRING.id };
        Buffer buffer = new Buffer( SIZE + 64 * 1024, new LogId( "prefix", "type", "host", Map.of(), headers, types ), ROW_BINARY_V4 );

        Random random = new Random( 1 );
        DateTime time = new DateTime( 2026, 11, 14, 13, 0, DateTimeZone.UTC );
        while( buffer.dataLength() < SIZE ) {
            byte[] line = RowBinaryUtils.line( List.of(
                time.plusMillis( random.nextInt( 3_600_000 ) ),
                new UUID( random.nextLong(), random.nextLong() ).toString(),
                COUNTRIES[random.nextInt( COUNTRIES.length )],
                DEVICES[random.nextInt( DEVICES.length )],
                random.nextInt( 10 ) == 0,
                random.nextInt( 5000 ) / 1000.0,
                "site" + random.nextInt( 2000 ) + ".com",
                List.of( "s" + random.nextInt( 50 ), "s" + random.nextInt( 50 ), "s" + random.nextInt( 50 ) ) ) );
            if( !buffer.put( line ) ) break;
        }
        buffer.close( 1L );
        return buffer;
    }

    private static byte[] legacyGzip6( Buffer b ) throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream( b.length() );
        out.write( b.data(), 0, b.headerLength() );
        try( GZIPOutputStream gzip = new GZIPOutputStream( out ) ) {
            gzip.write( b.data(), b.headerLength(), b.dataLength() );
        }
        return out.toByteArray();
    }

    private static void run( String name, Buffer buffer, LogStreamCompression decoder, Codec codec ) throws IOException {
        int headerLength = buffer.headerLength();
        byte[] compressed = null;
        for( int i = 0; i < WARMING; i++ ) compressed = codec.compress( buffer );

        long start = System.nanoTime();
        for( int i = 0; i < SAMPLES; i++ ) compressed = codec.compress( buffer );
        long compressNs = System.nanoTime() - start;

        for( int i = 0; i < WARMING; i++ ) decompress( decoder, compressed, headerLength );
        start = System.nanoTime();
        for( int i = 0; i < SAMPLES; i++ ) decompress( decoder, compressed, headerLength );
        long decompressNs = System.nanoTime() - start;

        double mb = ( double ) buffer.dataLength() * SAMPLES / ( 1024 * 1024 );
        int compressedLength = compressed.length - headerLength;
        System.out.printf( "%-16s compress %8.1f MB/s  decompress %8.1f MB/s  size %8d  ratio %5.2f%n",
            name, mb / ( compressNs / 1e9 ), mb / ( decompressNs / 1e9 ), compressedLength, ( double ) buffer.dataLength() / compressedLength );
    }

    private static byte[] decompress( LogStreamCompression compression, byte[] data, int offset ) throws IOException {
        return switch( compression ) {
            case GZIP -> Compression.ungzip( data, offset, data.length - offset );
            case LZ4 -> Compression.unlz4( data, offset, data.length - offset );
            case ZSTD -> Compression.unzstd( data, offset, data.length - offset );
        };
    }

    @Test( enabled = false )
    public void compression() throws IOException {
        Buffer buffer = rowBinaryBuffer();
        System.out.println( "payload " + buffer.dataLength() + " bytes" );

        try( BufferCompressor compressor = new BufferCompressor() ) {
            run( "gzip 6 legacy", buffer, LogStreamCompression.GZIP, BufferCompressionPerftest::legacyGzip6 );
            for( int level : new int[] { 1, 6 } ) {
                run( "gzip " + level, buffer, LogStreamCompression.GZIP, b -> compressor.compress( b, LogStreamCompression.GZIP, level ) );
            }
            for( int level : new int[] { 0, 9 } ) {
                run( "lz4 " + level, buffer, LogStreamCompression.LZ4, b -> compressor.compress( b, LogStreamCompression.LZ4, level ) );
            }
            for( int level : new int[] { -5, -1, 1, 3 } ) {
                run( "zstd " + level, buffer, LogStreamCompression.ZSTD, b -> compressor.compress( b, LogStreamCompression.ZSTD, level ) );
            }
        }
    }
}
