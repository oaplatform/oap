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
package oap.archive;

import lombok.SneakyThrows;
import oap.io.IoStreams;
import oap.util.Try;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;

public class Archiver {
    private static final Function<InputStream, InputStream> gzipInputStreamSupplyer;
    private static final BiFunction<OutputStream, Integer, OutputStream> gzipOutputStreamSupplier;

    static {
        CompressorStreamFactory factory = new CompressorStreamFactory( true );
        if( "apache".equals( System.getProperty( "oap.io.gzip", "apache" ) ) ) {
            gzipInputStreamSupplyer = Try.map( is -> factory.createCompressorInputStream( CompressorStreamFactory.GZIP, is ) );
            gzipOutputStreamSupplier = Try.biMap( ( os, bufferSize ) -> factory.createCompressorOutputStream( CompressorStreamFactory.GZIP, os ) );
        } else {
            gzipInputStreamSupplyer = Try.map( GZIPInputStream::new );
            gzipOutputStreamSupplier = Try.biMap( GZIPOutputStream::new );
        }
    }

    public static OutputStream gzip( OutputStream os, int bufferSize ) {
        return gzipOutputStreamSupplier.apply( os, bufferSize );
    }

    public static InputStream ungzip( InputStream is ) {
        return gzipInputStreamSupplyer.apply( is );
    }

    @SneakyThrows
    public void unpack( Path archive, Path dest, ArchiveType type ) {
        if( type == ArchiveType.TAR_GZ ) {
            try( TarArchiveInputStream tar = new TarArchiveInputStream( IoStreams.in( archive, GZIP ) ) ) {
                ArchiveEntry entry;
                while( ( entry = tar.getNextEntry() ) != null ) {
                    Path path = dest.resolve( entry.getName() );
                    if( entry.isDirectory() ) path.toFile().mkdirs();
                    else IoStreams.write( path, PLAIN, tar );
                }
            }
        } else throw new IllegalArgumentException( String.valueOf( type ) );
    }

    public enum ArchiveType {
        TAR_GZ
    }
}
