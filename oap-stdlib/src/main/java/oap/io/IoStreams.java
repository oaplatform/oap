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

import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import oap.archive.Archiver;
import oap.io.ProgressInputStream.Progress;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static oap.io.ProgressInputStream.progress;
import static oap.util.Functions.empty.consume;

@Slf4j
public class IoStreams {

    public static final int DEFAULT_BUFFER = 8192;

    public static Stream<String> lines( URL url ) {
        return lines( url, Encoding.from( url ), consume() );
    }

    public static Stream<String> lines( URL url, Consumer<Integer> progress ) {
        return lines( url, Encoding.from( url ), progress );
    }

    @SneakyThrows
    public static Stream<String> lines( URL url, Encoding encoding, Consumer<Integer> progress ) {
        log.trace( "loading {}...", url );
        URLConnection connection = url.openConnection();
        InputStream stream = connection.getInputStream();
        return lines( stream, encoding, progress( connection.getContentLengthLong(), progress ) )
            .onClose( Try.run( stream::close ) );
    }

    public static Stream<String> lines( Path path ) {
        return lines( path, Encoding.from( path ), consume() );
    }

    public static Stream<String> lines( Path path, Encoding encoding ) {
        return lines( path, encoding, p -> {} );
    }

    public static Stream<String> lines( Path path, Encoding encoding, Consumer<Integer> progress ) {
        InputStream stream = in( path, Encoding.PLAIN );
        return lines( stream, encoding, progress( path.toFile().length(), progress ) )
            .onClose( Try.run( stream::close ) );
    }

    private static Stream<String> lines( InputStream stream, Encoding encoding, Progress progress ) {
        return lines( in( new ProgressInputStream( stream, progress ), encoding ) );
    }

    public static Stream<String> lines( InputStream stream ) {
        return lines( stream, false );
    }

    public static Stream<String> lines( InputStream stream, boolean autoClose ) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( stream, StandardCharsets.UTF_8 ) );
        java.util.stream.Stream<String> ustream = bufferedReader.lines();
        if( autoClose ) {
            ustream = ustream.onClose( Try.run( bufferedReader::close ) );
        }

        return Stream.of( ustream );
    }

    public static void write( Path path, Encoding encoding, String value ) {
        write( path, encoding, value, false );
    }

    public static void write( Path path, Encoding encoding, InputStream in ) {
        write( path, encoding, in, Progress.EMPTY );
    }

    public static void write( Path path, Encoding encoding, InputStream in, Progress progress ) {
        write( path, encoding, in, false, false, progress );
    }

    public static void write( Path path, Encoding encoding, InputStream in, Progress progress, boolean append ) {
        write( path, encoding, in, append, false, progress );
    }

    public static void write( Path path, Encoding encoding, String value, boolean append ) {
        write( path, encoding, new ByteArrayInputStream( Strings.toByteArray( value ) ), append, false, Progress.EMPTY );

    }

    @SneakyThrows
    public static void write( Path path, Encoding encoding, InputStream in, boolean append, boolean safe, Progress progress ) {
        Files.ensureFile( path );
        try( OutputStream out = out( path, encoding, DEFAULT_BUFFER, append, safe ) ) {
            ByteStreams.copy( new ProgressInputStream( in, progress ), out );
        }
    }

    @SneakyThrows
    public static void write( Path path, Encoding encoding, InputStream in, boolean append, boolean safe ) {
        Files.ensureFile( path );
        try( OutputStream out = out( path, encoding, DEFAULT_BUFFER, append, safe ) ) {
            ByteStreams.copy( in, out );
        }
    }

    public static OutputStream out( Path path ) {
        return out( path, Encoding.from( path ), DEFAULT_BUFFER );
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

    @SneakyThrows
    public static OutputStream out( Path path, Encoding encoding, int bufferSize, boolean append, boolean safe ) {

        Files.ensureFile( path );
        if( append ) Files.ensureFileEncodingValid( path );
        OutputStream outputStream = safe
            ? new SafeFileOutputStream( path, append, encoding )
            : new FileOutputStream( path.toFile(), append );
        OutputStream fos = bufferSize > 0 ? new BufferedOutputStream( outputStream, bufferSize ) : outputStream;
        switch( encoding ) {
            case GZIP:
                return Archiver.ungzip( fos );
            case ZIP:
                if( append ) throw new IllegalArgumentException( "cannot append zip file" );
                var zip = new ZipOutputStream( fos );
                zip.putNextEntry( new ZipEntry( path.getFileName().toString() ) );
                return zip;
            case LZ4:
                return new LZ4BlockOutputStream( fos );
            case PLAIN:
                return fos;
            default:
                throw new IllegalArgumentException( "unknown encoding " + encoding );
        }
    }

    public static InputStream in( Path path, Encoding encoding ) {
        return in( path, encoding, DEFAULT_BUFFER );
    }

    public static InputStream in( Path path ) {
        return in( path, DEFAULT_BUFFER );
    }

    public static InputStream in( Path path, int bufferSIze ) {
        return in( path, Encoding.from( path ), bufferSIze );
    }

    @SneakyThrows
    public static InputStream in( Path path, Encoding encoding, int bufferSize ) {
        try {
            final FileInputStream fileInputStream = new FileInputStream( path.toFile() );
            return decoded(
                bufferSize > 0 ? new BufferedInputStream( fileInputStream, bufferSize ) : fileInputStream, encoding );
        } catch( IOException e ) {
            throw new IOException( "couldn't open file " + path.toString(), e );
        }
    }

    @SneakyThrows
    public static InputStream in( InputStream stream, Encoding encoding ) {
        return decoded( stream, encoding );
    }

    @SneakyThrows
    public static String asString( InputStream stream, Encoding encoding ) {
        return IOUtils.toString( decoded( stream, encoding ), StandardCharsets.UTF_8 );
    }

    @SneakyThrows
    private static InputStream decoded( InputStream stream, Encoding encoding ) {
        switch( encoding ) {
            case GZIP:
                try {
                    return Archiver.gzip( stream );
                } catch( Exception e ) {
                    stream.close();
                }
            case ZIP:
                try {
                    ZipInputStream zip = new ZipInputStream( stream );
                    if( zip.getNextEntry() == null )
                        throw new IllegalArgumentException( "zip stream contains no entries" );
                    return zip;
                } catch( Exception e ) {
                    stream.close();
                }
            case PLAIN:
                return stream;
            case LZ4:
                try {
                    return new LZ4BlockInputStream( stream );
                } catch( Exception e ) {
                    stream.close();
                    throw e;
                }
            default:
                throw new IllegalArgumentException( "Unknown encoding " + encoding );
        }
    }

    public enum Encoding {
        PLAIN( "", false ),
        ZIP( ".zip", true ),
        GZIP( ".gz", true ),
        LZ4( ".lz4", true );

        public final String extension;
        public final boolean compressed;

        Encoding( String extension, boolean compressed ) {
            this.extension = extension;
            this.compressed = compressed;
        }

        public static Encoding from( Path path ) {
            return from( path.toString() );
        }

        public static Encoding from( URL url ) {
            return from( url.toString() );
        }

        public static Encoding from( String name ) {
            for( var e : values() ) {
                if( e.compressed && StringUtils.endsWithIgnoreCase( name, e.extension ) ) return e;
            }

            return PLAIN;
        }

        public Path resolve( Path path ) {
            String s = path.toString();
            return Paths.get( s.substring( 0, s.length() - from( path ).extension.length() ) + extension );
        }
    }

    @FunctionalInterface
    public interface ThrowingIOExceptionConsumer<T> {
        void accept( T t ) throws IOException;

        default Consumer<T> asConsumer() {
            return t -> {
                try {
                    this.accept( t );
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            };
        }
    }
}
