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

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.xxhash.XXHashFactory;
import oap.logstream.LogStreamCompression;

import java.io.Closeable;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Compresses {@link Buffer} payload: the header is copied as is, the payload is compressed by the selected codec,
 * the data length and compression fields of the header ({@link oap.logstream.LogStreamProtocol.ProtocolVersion#ROW_BINARY_V4}) are updated.
 * <p>
 * The compressor state (Deflater, CRC32, ZstdCompressCtx, scratch array) is reused between calls,
 * the result is an exact-size array (the message sender keeps it in the queue).
 * <p>
 * Not thread-safe.
 */
public class BufferCompressor implements Closeable {
    /**
     * magic, OS = unknown
     */
    private static final byte[] GZIP_HEADER = { 0x1f, ( byte ) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, ( byte ) 0xff };
    private static final int GZIP_TRAILER_LENGTH = 8;

    private static final int LZ4_MAGIC = 0x184D2204;
    /**
     * version 01, block independence, no block checksum, no content size, no content checksum
     */
    private static final byte LZ4_FLG = 0x60;
    /**
     * block max size 4 MB
     */
    private static final byte LZ4_BD = 0x70;
    private static final int LZ4_BLOCK_MAX_SIZE = 4 * 1024 * 1024;
    private static final int LZ4_UNCOMPRESSED_BLOCK = 0x80000000;
    private static final int LZ4_FRAME_HEADER_LENGTH = 7;
    private static final int LZ4_END_MARK_LENGTH = 4;
    private static final byte LZ4_HC = ( byte ) ( ( XXHashFactory.safeInstance().hash32().hash( new byte[] { LZ4_FLG, LZ4_BD }, 0, 2, 0 ) >> 8 ) & 0xFF );

    private final CRC32 crc32 = new CRC32();
    private byte[] scratch = new byte[0];
    private Deflater deflater;
    private LZ4Compressor lz4Compressor;
    private int lz4Level = Integer.MIN_VALUE;
    private ZstdCompressCtx zstdCtx;
    private int zstdLevel = Integer.MIN_VALUE;

    private static void writeIntBE( byte[] array, int pos, int i ) {
        array[pos] = ( byte ) ( i >>> 24 );
        array[pos + 1] = ( byte ) ( i >>> 16 );
        array[pos + 2] = ( byte ) ( i >>> 8 );
        array[pos + 3] = ( byte ) i;
    }

    private static void writeIntLE( byte[] array, int pos, int i ) {
        array[pos] = ( byte ) i;
        array[pos + 1] = ( byte ) ( i >>> 8 );
        array[pos + 2] = ( byte ) ( i >>> 16 );
        array[pos + 3] = ( byte ) ( i >>> 24 );
    }

    public byte[] compress( Buffer buffer, LogStreamCompression compression, int level ) {
        compression.checkLevel( level );

        byte[] data = buffer.data();
        int headerLength = buffer.headerLength();
        int dataLength = buffer.dataLength();

        int end = switch( compression ) {
            case GZIP -> gzip( data, headerLength, dataLength, level );
            case LZ4 -> lz4( data, headerLength, dataLength, level );
            case ZSTD -> zstd( data, headerLength, dataLength, level );
        };

        System.arraycopy( data, 0, scratch, 0, headerLength );
        writeIntBE( scratch, Buffer.DATE_LENGTH_POSITION, end - headerLength );
        scratch[Buffer.COMPRESSION_POSITION] = compression.id;

        return Arrays.copyOf( scratch, end );
    }

    private void ensureCapacity( int capacity ) {
        if( scratch.length < capacity ) {
            scratch = Arrays.copyOf( scratch, Math.max( capacity, scratch.length + ( scratch.length >> 1 ) ) );
        }
    }

    private int gzip( byte[] data, int offset, int length, int level ) {
        if( deflater == null ) {
            deflater = new Deflater( level, true );
        } else {
            deflater.reset();
            deflater.setLevel( level );
        }

        crc32.reset();
        crc32.update( data, offset, length );

        ensureCapacity( offset + GZIP_HEADER.length + length / 2 + 64 + GZIP_TRAILER_LENGTH );
        System.arraycopy( GZIP_HEADER, 0, scratch, offset, GZIP_HEADER.length );
        int pos = offset + GZIP_HEADER.length;

        deflater.setInput( data, offset, length );
        deflater.finish();
        while( !deflater.finished() ) {
            if( pos == scratch.length ) {
                ensureCapacity( scratch.length + 1 );
            }
            pos += deflater.deflate( scratch, pos, scratch.length - pos );
        }

        ensureCapacity( pos + GZIP_TRAILER_LENGTH );
        writeIntLE( scratch, pos, ( int ) crc32.getValue() );
        writeIntLE( scratch, pos + 4, length );

        return pos + GZIP_TRAILER_LENGTH;
    }

    private int lz4( byte[] data, int offset, int length, int level ) {
        if( lz4Compressor == null || lz4Level != level ) {
            LZ4Factory factory = LZ4Factory.fastestInstance();
            lz4Compressor = level == 0 ? factory.fastCompressor() : factory.highCompressor( level );
            lz4Level = level;
        }

        int blocks = ( length + LZ4_BLOCK_MAX_SIZE - 1 ) / LZ4_BLOCK_MAX_SIZE;
        int maxBlockLength = lz4Compressor.maxCompressedLength( Math.min( length, LZ4_BLOCK_MAX_SIZE ) );
        ensureCapacity( offset + LZ4_FRAME_HEADER_LENGTH + blocks * ( 4 + maxBlockLength ) + LZ4_END_MARK_LENGTH );

        int pos = offset;
        writeIntLE( scratch, pos, LZ4_MAGIC );
        scratch[pos + 4] = LZ4_FLG;
        scratch[pos + 5] = LZ4_BD;
        scratch[pos + 6] = LZ4_HC;
        pos += LZ4_FRAME_HEADER_LENGTH;

        int srcPos = offset;
        int srcEnd = offset + length;
        while( srcPos < srcEnd ) {
            int blockLength = Math.min( LZ4_BLOCK_MAX_SIZE, srcEnd - srcPos );
            int compressed = lz4Compressor.compress( data, srcPos, blockLength, scratch, pos + 4, maxBlockLength );
            if( compressed < blockLength ) {
                writeIntLE( scratch, pos, compressed );
                pos += 4 + compressed;
            } else {
                writeIntLE( scratch, pos, blockLength | LZ4_UNCOMPRESSED_BLOCK );
                System.arraycopy( data, srcPos, scratch, pos + 4, blockLength );
                pos += 4 + blockLength;
            }
            srcPos += blockLength;
        }

        writeIntLE( scratch, pos, 0 );

        return pos + LZ4_END_MARK_LENGTH;
    }

    private int zstd( byte[] data, int offset, int length, int level ) {
        if( zstdCtx == null ) {
            zstdCtx = new ZstdCompressCtx();
            zstdCtx.setChecksum( false );
            zstdCtx.setContentSize( true );
        }
        if( zstdLevel != level ) {
            zstdCtx.setLevel( level );
            zstdLevel = level;
        }

        int bound = ( int ) Zstd.compressBound( length );
        ensureCapacity( offset + bound );

        return offset + zstdCtx.compressByteArray( scratch, offset, bound, data, offset, length );
    }

    @Override
    public void close() {
        if( deflater != null ) {
            deflater.end();
            deflater = null;
        }
        if( zstdCtx != null ) {
            zstdCtx.close();
            zstdCtx = null;
        }
        scratch = new byte[0];
    }
}
