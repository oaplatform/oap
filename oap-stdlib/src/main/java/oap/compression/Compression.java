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
import oap.util.function.Try;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Compression {
    public static final int DEFAULT_BUFFER_SIZE = 512;
    private static final Function<InputStream, InputStream> gzipInputStreamSupplyer;
    private static final BiFunction<OutputStream, Integer, OutputStream> gzipOutputStreamSupplier;

    static {
        if( "apache".equals( System.getProperty( "oap.io.gzip", "apache" ) ) ) {
            CompressorStreamFactory factory = new CompressorStreamFactory( true );
            gzipInputStreamSupplyer = Try.map( is -> factory.createCompressorInputStream( CompressorStreamFactory.GZIP, is ) );
            gzipOutputStreamSupplier = Try.biMap( ( os, bufferSize ) -> factory.createCompressorOutputStream( CompressorStreamFactory.GZIP, os ) );
        } else {
            gzipInputStreamSupplyer = Try.map( GZIPInputStream::new );
            gzipOutputStreamSupplier = Try.biMap( GZIPOutputStream::new );
        }
    }

    public static OutputStream gzip( OutputStream os ) {
        return gzip( os, DEFAULT_BUFFER_SIZE );
    }

    public static OutputStream gzip( OutputStream os, int bufferSize ) {
        return gzipOutputStreamSupplier.apply( os, bufferSize );
    }

    public static InputStream ungzip( InputStream is ) {
        return gzipInputStreamSupplyer.apply( is );
    }

    public static class ContentWriter {
        public static oap.io.content.ContentWriter<String> ofGzip() {
            return new oap.io.content.ContentWriter<>() {
                @Override
                @SneakyThrows
                public void write( OutputStream os, String object ) {
                    try( var gos = gzip( os ) ) {
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
                    var baos = new ByteArrayOutputStream();
                    try( var gos = ungzip( is ) ) {
                        IOUtils.copy( gos, baos );
                    }
                    return baos.toByteArray();
                }
            };
        }
    }
}
