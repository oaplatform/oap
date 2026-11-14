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
package oap.compression;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdInputStream;
import lombok.SneakyThrows;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Compression {
    public static void gzip( OutputStream out, byte[] bytes, int offset, int length ) throws IOException {
        try( OutputStream gos = new GZIPOutputStream( out ) ) {
            gos.write( bytes, offset, length );
        }
    }

    public static byte[] gzip( byte[] bytes, int offset, int length ) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        gzip( byteArrayOutputStream, bytes, offset, length );
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gzip( byte[] bytes ) throws IOException {
        return gzip( bytes, 0, bytes.length );
    }

    public static byte[] ungzip( byte[] bytes ) throws IOException {
        return ungzip( bytes, 0, bytes.length );
    }

    public static byte[] ungzip( byte[] bytes, int offset, int length ) throws IOException {
        return new GZIPInputStream( new ByteArrayInputStream( bytes, offset, length ) ).readAllBytes();
    }

    public static void ungzip( byte[] bytes, int offset, int length, OutputStream out ) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream( new ByteArrayInputStream( bytes, offset, length ) );
        IOUtils.copy( gzipInputStream, out );
    }

    /**
     * LZ4 frame format (lz4 CLI / ClickHouse compatible)
     */
    public static byte[] lz4( byte[] bytes, int offset, int length ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try( LZ4FrameOutputStream out = new LZ4FrameOutputStream( baos, LZ4FrameOutputStream.BLOCKSIZE.SIZE_256KB ) ) {
            out.write( bytes, offset, length );
        }
        return baos.toByteArray();
    }

    public static byte[] lz4( byte[] bytes ) throws IOException {
        return lz4( bytes, 0, bytes.length );
    }

    /**
     * supports concatenated frames
     */
    public static byte[] unlz4( byte[] bytes, int offset, int length ) throws IOException {
        try( LZ4FrameInputStream in = new LZ4FrameInputStream( new ByteArrayInputStream( bytes, offset, length ) ) ) {
            return in.readAllBytes();
        }
    }

    public static byte[] unlz4( byte[] bytes ) throws IOException {
        return unlz4( bytes, 0, bytes.length );
    }

    public static byte[] zstd( byte[] bytes, int offset, int length, int level ) {
        try( ZstdCompressCtx ctx = new ZstdCompressCtx() ) {
            ctx.setLevel( level );
            ctx.setContentSize( true );
            byte[] dst = new byte[( int ) Zstd.compressBound( length )];
            int size = ctx.compressByteArray( dst, 0, dst.length, bytes, offset, length );
            return Arrays.copyOf( dst, size );
        }
    }

    public static byte[] zstd( byte[] bytes ) {
        return zstd( bytes, 0, bytes.length, Zstd.defaultCompressionLevel() );
    }

    /**
     * supports concatenated frames
     */
    public static byte[] unzstd( byte[] bytes, int offset, int length ) throws IOException {
        try( ZstdInputStream in = new ZstdInputStream( new ByteArrayInputStream( bytes, offset, length ) ) ) {
            return in.readAllBytes();
        }
    }

    public static byte[] unzstd( byte[] bytes ) throws IOException {
        return unzstd( bytes, 0, bytes.length );
    }

    public static class ContentWriter {
        public static oap.io.content.ContentWriter<String> ofGzip() {
            return new oap.io.content.ContentWriter<>() {
                @Override
                @SneakyThrows
                public void write( OutputStream os, String object ) {
                    try( GZIPOutputStream gos = new GZIPOutputStream( os ) ) {
                        gos.write( object.getBytes( UTF_8 ) );
                    }
                }
            };
        }
    }

    public static class ContentReader {
        public static oap.io.content.ContentReader<byte[]> ofBytes() {
            return new oap.io.content.ContentReader<>() {
                @Override
                @SneakyThrows
                public byte[] read( InputStream is ) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try( GZIPInputStream gos = new GZIPInputStream( is ) ) {
                        IOUtils.copy( gos, baos );
                    }
                    return baos.toByteArray();
                }
            };
        }
    }
}
