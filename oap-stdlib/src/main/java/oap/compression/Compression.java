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

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
