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
package oap.io;

import com.google.common.io.CountingInputStream;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Try;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class IoStreams {

    public static final int DEFAULT_BUFFER = 8192;

    public enum Encoding {
        PLAIN, ZIP, GZIP
    }

    public static Stream<String> lines( URL url ) {
        return lines( url, Encoding.PLAIN, p -> {
        } );
    }

    public static Stream<String> lines( URL url, Consumer<Integer> progressCallback ) {
        return lines( url, Encoding.PLAIN, progressCallback );
    }

    public static Stream<String> lines( URL url, Encoding encoding, Consumer<Integer> progressCallback ) {
        try {
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            return lines( stream, connection.getContentLengthLong(), encoding, progressCallback )
                .onClose( Try.run( stream::close ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static Stream<String> lines( Path path ) {
        return lines( path, Encoding.PLAIN, p -> {
        } );
    }

    public static Stream<String> lines( Path path, Encoding encoding ) {
        return lines( path, encoding, p -> {
        } );
    }

    public static Stream<String> lines( Path path, Encoding encoding, Consumer<Integer> progressCallback ) {
        InputStream stream = in( path, Encoding.PLAIN );
        return lines( stream, path.toFile().length(), encoding, progressCallback )
            .onClose( Try.run( stream::close ) );
    }

    private static Stream<String> lines( InputStream stream, long size, Encoding encoding,
                                         Consumer<Integer> progressCallback ) {
        long percent = size / 100;
        AtomicInteger lastReport = new AtomicInteger();
        CountingInputStream counting = new CountingInputStream( stream );
        return lines( in( counting, encoding ) )
            .map( l -> {
                if( percent > 0 && counting.getCount() / percent > lastReport.get() ) {
                    lastReport.set( ( int ) ( counting.getCount() / percent ) );
                    progressCallback.accept( lastReport.get() );
                }
                return l;
            } );
    }

    public static Stream<String> lines( InputStream stream ) {
        return Stream.of( new BufferedReader( new InputStreamReader( stream, StandardCharsets.UTF_8 ) ).lines() );
    }

    public static void write( Path path, Encoding encoding, String value ) {
        path.toAbsolutePath().getParent().toFile().mkdirs();
        try( OutputStream os = out( path, encoding ) ) {
            os.write( Strings.toByteArray( value ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }

    }

    public static OutputStream out( Path path, Encoding encoding ) {
        return out( path, encoding, DEFAULT_BUFFER );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize ) {
        return out( path, encoding, bufferSize, false );
    }

    public static OutputStream out( Path path, Encoding encoding, boolean append ) {
        return out( path, encoding, DEFAULT_BUFFER, append );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize, boolean append ) {
        return out( path, encoding, bufferSize, append, false );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize, boolean append, boolean safe ) {
        path.toAbsolutePath().getParent().toFile().mkdirs();
        try {
            OutputStream fos = new BufferedOutputStream( safe ?
                new SafeFileOutputStream( path, append ) :
                new FileOutputStream( path.toFile(), append ),
                bufferSize );
            switch( encoding ) {
                case GZIP:
                    return new GZIPOutputStream( fos );
                case ZIP:
                    if( append ) throw new IllegalArgumentException( "cannot append zip file" );
                    ZipOutputStream zip = new ZipOutputStream( fos );
                    zip.putNextEntry( new ZipEntry( path.getFileName().toString() ) );
                    return zip;
                default:
                    return fos;
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static InputStream in( Path path, Encoding encoding ) {
        try {
            return in( new BufferedInputStream( new FileInputStream( path.toFile() ) ), encoding );
        } catch( FileNotFoundException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static InputStream in( InputStream stream, Encoding encoding ) {
        try {
            switch( encoding ) {
                case GZIP:
                    return new GZIPInputStream( stream );
                case ZIP:
                    ZipInputStream zip = new ZipInputStream( stream );
                    if( zip.getNextEntry() == null )
                        throw new IllegalArgumentException( "zip stream contains no entries" );
                    return zip;
                default:
                    return stream;
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }

    }


}
