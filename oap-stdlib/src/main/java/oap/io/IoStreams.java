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
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import oap.archive.Archiver;
import oap.io.ProgressInputStream.Progress;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.function.Try;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.jpountz.lz4.LZ4FrameOutputStream.BLOCKSIZE.SIZE_64KB;
import static oap.io.ProgressInputStream.progress;
import static oap.util.function.Functions.empty.consume;

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
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( stream, UTF_8 ) );
        java.util.stream.Stream<String> ustream = bufferedReader.lines();
        if( autoClose ) {
            ustream = ustream.onClose( Try.run( bufferedReader::close ) );
        }

        return Stream.of( ustream );
    }

    public static void write( Path path, Encoding encoding, java.util.stream.Stream<String> lines ) throws UncheckedIOException {
        Files.ensureFile( path );

        try( OutputStream out = out( path, encoding, DEFAULT_BUFFER, false, false ) ) {
            lines.forEach( line -> {
                try {
                    out.write( line.getBytes() );
                    out.write( '\n' );
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            } );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void write( Path path, Encoding encoding, String value ) throws UncheckedIOException {
        write( path, encoding, value, false );
    }

    public static void write( Path path, Encoding encoding, InputStream in ) throws UncheckedIOException {
        write( path, encoding, in, Progress.EMPTY );
    }

    public static void write( Path path, Encoding encoding, InputStream in, Progress progress ) throws UncheckedIOException {
        write( path, encoding, in, false, false, progress );
    }

    public static void write( Path path, Encoding encoding, InputStream in, Progress progress, boolean append ) throws UncheckedIOException {
        write( path, encoding, in, append, false, progress );
    }

    public static void write( Path path, Encoding encoding, String value, boolean append ) throws UncheckedIOException {
        write( path, encoding, new ByteArrayInputStream( Strings.toByteArray( value ) ), append, false, Progress.EMPTY );

    }

    public static void write( Path path, Encoding encoding, InputStream in, boolean append, boolean safe, Progress progress ) throws UncheckedIOException {
        Files.ensureFile( path );
        try( OutputStream out = out( path, encoding, DEFAULT_BUFFER, append, safe ) ) {
            ByteStreams.copy( new ProgressInputStream( in, progress ), out );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void write( Path path, Encoding encoding, InputStream in, boolean append, boolean safe ) throws UncheckedIOException {
        Files.ensureFile( path );
        try( OutputStream out = out( path, encoding, DEFAULT_BUFFER, append, safe ) ) {
            ByteStreams.copy( in, out );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static OutputStream out( Path path ) throws UncheckedIOException {
        return out( path, Encoding.from( path ), DEFAULT_BUFFER );
    }

    public static OutputStream out( Path path, Encoding encoding ) throws UncheckedIOException {
        return out( path, encoding, DEFAULT_BUFFER );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize ) throws UncheckedIOException {
        return out( path, encoding, bufferSize, false );
    }

    public static OutputStream out( Path path, Encoding encoding, boolean append ) throws UncheckedIOException {
        return out( path, encoding, DEFAULT_BUFFER, append );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize, boolean append ) throws UncheckedIOException {
        return out( path, encoding, bufferSize, append, false );
    }

    public static OutputStream out( Path path, Encoding encoding, int bufferSize, boolean append, boolean safe ) throws UncheckedIOException {
        try {
            checkArgument( !append || encoding.appendable, encoding + " is not appendable" );
            Files.ensureFile( path );
            if( append ) Files.ensureFileEncodingValid( path );
            OutputStream outputStream = safe
                ? new SafeFileOutputStream( path, append, encoding )
                : new FileOutputStream( path.toFile(), append );
            OutputStream fos = bufferSize > 0 && encoding != Encoding.GZIP ? new BufferedOutputStream( outputStream, bufferSize ) : outputStream;
            return switch( encoding ) {
                case GZIP -> {
                    OutputStream gzout = Archiver.gzip( fos, bufferSize > 0 ? bufferSize : 512 );
                    yield bufferSize > 0 ? new BufferedOutputStream( gzout, bufferSize ) : gzout;
                }
                case ZIP -> {
                    var zip = new ZipOutputStream( fos );
                    zip.putNextEntry( new ZipEntry( path.getFileName().toString() ) );
                    yield zip;
                }
                case LZ4_BLOCK -> new LZ4BlockOutputStream( fos );
                case LZ4 -> new LZ4FrameOutputStream( fos, SIZE_64KB );
                case ZSTD -> new ZstdCompressorOutputStream( fos );
                case PLAIN -> fos;
            };
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static InputStream in( Path path, Encoding encoding ) throws UncheckedIOException {
        return in( path, encoding, DEFAULT_BUFFER );
    }

    public static InputStream in( Path path ) throws UncheckedIOException {
        return in( path, DEFAULT_BUFFER );
    }

    public static InputStream in( Path path, int bufferSIze ) throws UncheckedIOException {
        return in( path, Encoding.from( path ), bufferSIze );
    }

    public static InputStream in( Path path, Encoding encoding, int bufferSize ) throws UncheckedIOException {
        try {
            FileInputStream fileInputStream = new FileInputStream( path.toFile() );
            return decoded(
                bufferSize > 0 ? new BufferedInputStream( fileInputStream, bufferSize ) : fileInputStream, encoding );
        } catch( IOException e ) {
            throw new UncheckedIOException( "couldn't open file " + path.toString(), e );
        }
    }

    @SneakyThrows
    public static InputStream in( InputStream stream, Encoding encoding ) {
        return decoded( stream, encoding );
    }

    @SneakyThrows
    public static String asString( InputStream stream, Encoding encoding ) {
        return IOUtils.toString( decoded( stream, encoding ), UTF_8 );
    }

    @SneakyThrows
    private static InputStream decoded( InputStream stream, Encoding encoding ) {
        switch( encoding ) {
            case GZIP:
                try {
                    return Archiver.ungzip( stream );
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
            case LZ4_BLOCK:
                try {
                    return new LZ4BlockInputStream( stream );
                } catch( Exception e ) {
                    stream.close();
                    throw e;
                }
            case LZ4:
                try {
                    return new LZ4FrameInputStream( stream );
                } catch( Exception e ) {
                    stream.close();
                    throw e;
                }
            case ZSTD:
                try {
                    return new ZstdCompressorInputStream( stream );
                } catch( Exception e ) {
                    stream.close();
                    throw e;
                }
            default:
                throw new IllegalArgumentException( "Unknown encoding " + encoding );
        }
    }

    public enum Encoding {
        PLAIN( "", false, true ),
        ZIP( ".zip", true, false ),
        GZIP( ".gz", true, true ),
        ZSTD( ".zst", true, true ),
        LZ4( ".lz4", true, true ),
        LZ4_BLOCK( ".lz4b", true, false );

        public String extension;
        public boolean compressed;
        public boolean appendable;

        Encoding( String extension, boolean compressed, boolean appendable ) {
            this.extension = extension;
            this.compressed = compressed;
            this.appendable = appendable;
        }

        public static Encoding from( Path path ) {
            return from( path.toString() );
        }

        public static Encoding from( URL url ) {
            return from( url.toString() );
        }

        public static Encoding from( String name ) {
            for( var e : values() ) if( e.compressed && StringUtils.endsWithIgnoreCase( name, e.extension ) ) return e;

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
