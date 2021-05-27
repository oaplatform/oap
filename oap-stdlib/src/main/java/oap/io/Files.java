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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.common.hash.Hashing;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentReader;
import oap.net.Inet;
import oap.time.Time;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
public final class Files {

    public static Path deepPath( Path basePath, String name ) {
        BiFunction<Integer, Integer, String> f = ( hash, bits ) -> String.valueOf( hash & ( ( 1 << bits ) - 1 ) );
        int bitPerDir = 8;

        @SuppressWarnings( "UnstableApiUsage" ) int hash = Hashing.murmur3_32().hashBytes( name.getBytes() ).asInt();
        String f1 = f.apply( hash, bitPerDir );
        hash >>>= bitPerDir;
        String f2 = f.apply( hash, bitPerDir );
        hash >>>= bitPerDir;

        return basePath
            .resolve( f1 )
            .resolve( f2 )
            .resolve( f.apply( hash, bitPerDir ) )
            .resolve( name );
    }

    /**
     * it is NOT compatible with {@link #wildcard(Path, String)}
     * <p>
     * todo make compatible
     */
    public static ArrayList<Path> fastWildcard( String basePath, String wildcard ) {
        return fastWildcard( Paths.get( basePath ), wildcard );
    }

    public static ArrayList<Path> fastWildcard( String basePath, String wildcard, FileWalkerCache cache ) {
        return fastWildcard( Paths.get( basePath ), wildcard, cache );
    }

    public static ArrayList<Path> fastWildcard( Path basePath, String wildcard ) {
        final ArrayList<Path> result = new ArrayList<>();
        new FileWalker( basePath, wildcard ).walkFileTree( result::add );
        return result;
    }

    public static ArrayList<Path> fastWildcard( Path basePath, String wildcard, FileWalkerCache cache ) {
        final ArrayList<Path> result = new ArrayList<>();
        new FileWalker( basePath, wildcard, cache ).walkFileTree( result::add );
        return result;
    }

    public static ArrayList<Path> wildcard( String basePath, String wildcard ) {
        return wildcard( Paths.get( basePath ), wildcard );
    }

    public static ArrayList<Path> wildcard( String basePath, String... wildcards ) {
        return wildcard( Paths.get( basePath ), wildcards );
    }

    public static ArrayList<Path> wildcard( Path basePath, String... wildcards ) {
        ArrayList<PathMatcher> matchers = Lists.map( wildcards, wc -> FileSystems.getDefault()
            .getPathMatcher( ( "glob:" + basePath + File.separator + wc ).replace( "\\", "\\\\" ) ) );
        ArrayList<Path> result = new ArrayList<>();
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
                if( matchers.stream().anyMatch( m -> m.matches( file ) ) ) result.add( file );
                return FileVisitResult.CONTINUE;
            }
        };
        if( java.nio.file.Files.exists( basePath ) && java.nio.file.Files.isExecutable( basePath ) ) {
            try {
                java.nio.file.Files.walkFileTree( basePath, visitor );
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        }
        Collections.sort( result );
        return result;
    }

    public static ArrayList<Path> wildcard( Path basePath, String wildcard ) {
        return wildcard( basePath, new String[] { wildcard } );
    }

    @SneakyThrows
    public static URL toUrl( Path path ) {
        return path.toUri().toURL();
    }

    public static List<Path> deepCollect( Path basePath, Predicate<Path> predicate ) {
        ArrayList<Path> result = new ArrayList<>();
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
                if( predicate.test( file ) ) result.add( file );
                return FileVisitResult.CONTINUE;
            }
        };
        if( exists( basePath ) && java.nio.file.Files.isExecutable( basePath ) ) {
            try {
                java.nio.file.Files.walkFileTree( basePath, visitor );
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        }
        return result;
    }

    /**
     * @see #read(Path, ContentReader)
     */
    @Deprecated
    @SneakyThrows
    public static <T> T readObject( Path path ) {
        try( var is = IoStreams.in( path, Encoding.PLAIN ) ) {
            return ContentReader.read( is, ContentReader.ofObject() );
        }
    }

    /**
     * @see #read(Path, ContentReader)
     */
    @Deprecated
    public static String readString( String path ) {
        return read( Paths.get( path ), Encoding.from( path ), ContentReader.ofString() );
    }

    public static String readString( Path path ) {
        return read( path, Encoding.from( path ), ContentReader.ofString() );
    }

    /**
     * @see #read(Path, Encoding, ContentReader)
     */
    @Deprecated
    public static String readString( Path path, Encoding encoding ) {
        return read( path, encoding, ContentReader.ofString() );
    }

    @Deprecated
    public static byte[] read( Path path ) {
        try {
            return java.nio.file.Files.readAllBytes( path );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @SneakyThrows
    public static <R> R read( Path path, Encoding encoding, ContentReader<R> reader ) {
        try( InputStream in = IoStreams.in( path, encoding ) ) {
            return ContentReader.read( in, reader );
        }
    }

    @SneakyThrows
    public static <R> R read( Path path, ContentReader<R> reader ) {
        try( InputStream in = IoStreams.in( path ) ) {
            return ContentReader.read( in, reader );
        }
    }

    public static void writeString( String path, String value ) {
        writeString( Paths.get( path ), value );
    }

    public static void writeString( Path path, String value ) {
        writeString( path, value, false );
    }

    public static void writeString( Path path, String value, boolean safe ) {
        IoStreams.write( path, Encoding.from( path ), new ByteArrayInputStream( value.getBytes() ), false, safe );
    }

    public static void writeString( Path path, Encoding encoding, String value ) {
        IoStreams.write( path, encoding, value );
    }

    public static void writeString( Path path, Encoding encoding, boolean append, String value ) {
        IoStreams.write( path, encoding, value, append );
    }

    public static void writeBytes( Path path, byte[] value ) {
        writeBytes( path, Encoding.from( path ), value );
    }

    public static void writeBytes( Path path, Encoding encoding, byte[] value ) {
        IoStreams.write( path, encoding, new ByteArrayInputStream( value ), false, false );
    }

    public static void write( Path path, byte[] value ) {
        ensureFile( path );
        try {
            java.nio.file.Files.write( path, value );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }


    public static void writeObject( Path path, Object value ) {
        try( ObjectOutputStream os = new ObjectOutputStream( IoStreams.out( path, Encoding.PLAIN ) ) ) {
            os.writeObject( value );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static Stream<String> lines( Path path ) {
        log.trace( "reading {}...", path );
        final InputStream in = IoStreams.in( path );
        return IoStreams.lines( in, true );
    }

    public static void copyDirectory( Path from, Path to ) throws UncheckedIOException {
        try {
            FileUtils.copyDirectory( from.toFile(), to.toFile() );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void cleanDirectory( Path path ) throws UncheckedIOException {
        try {
            FileUtils.cleanDirectory( path.toFile() );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void deleteSafely( Path path ) {
        try {
            if( path != null ) delete( path );
        } catch( Exception e ) {
            log.warn( e.getMessage(), e );
        }
    }

    public static void delete( Path path ) throws UncheckedIOException {
        var retryer = RetryerBuilder.<FileVisitResult>newBuilder()
            .retryIfException()
            .withStopStrategy( StopStrategies.stopAfterAttempt( 3 ) )
            .build();

        if( java.nio.file.Files.exists( path ) ) try {
            java.nio.file.Files.walkFileTree( path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile( Path path, BasicFileAttributes attrs ) throws IOException {
                    try {
                        return retryer.call( () -> {
                            if( java.nio.file.Files.exists( path ) )
                                java.nio.file.Files.delete( path );
                            return FileVisitResult.CONTINUE;
                        } );
                    } catch( ExecutionException e ) {
                        throw new IOException( e.getCause() );
                    } catch( RetryException e ) {
                        throw new IOException( e.getLastFailedAttempt().getExceptionCause() );
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory( Path path, IOException exc ) throws IOException {
                    if( java.nio.file.Files.exists( path ) )
                        java.nio.file.Files.delete( path );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed( Path file, IOException exc ) {
                    log.error( file.toString(), exc );

                    return FileVisitResult.CONTINUE;
                }
            } );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void deleteEmptyDirectories( Path path, boolean deleteRoot ) throws UncheckedIOException {
        if( java.nio.file.Files.exists( path ) ) try {
            java.nio.file.Files.walkFileTree( path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile( Path path, BasicFileAttributes attrs ) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
                    return super.preVisitDirectory( dir, attrs );
                }

                @Override
                public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
                    try {
                        if( !path.equals( dir ) || deleteRoot ) {
                            java.nio.file.Files.delete( dir );
                        }
                    } catch( DirectoryNotEmptyException ignore ) {
                    }
                    return FileVisitResult.CONTINUE;
                }

            } );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private static void copyOrAppend( Path sourcePath, Encoding sourceEncoding, Path destPath,
                                      Encoding destEncoding, int bufferSize, boolean append ) throws UncheckedIOException {
        ensureFile( destPath );
        try( InputStream is = IoStreams.in( sourcePath, sourceEncoding, bufferSize );
             OutputStream os = IoStreams.out( destPath, destEncoding, bufferSize, append, true ) ) {
            IOUtils.copy( is, os );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void copy( Path sourcePath, Encoding sourceEncoding,
                             Path destPath, Encoding destEncoding, int bufferSize ) {
        copyOrAppend( sourcePath, sourceEncoding, destPath, destEncoding, bufferSize, false );
    }

    public static void copy( Path sourcePath, Encoding sourceEncoding,
                             Path destPath, Encoding destEncoding ) {
        copy( sourcePath, sourceEncoding, destPath, destEncoding, IoStreams.DEFAULT_BUFFER );
    }

    public static void append( Path sourcePath, Encoding sourceEncoding,
                               Path destPath, Encoding destEncoding, int bufferSize ) {
        copyOrAppend( sourcePath, sourceEncoding, destPath, destEncoding, bufferSize, true );
    }

    public static void copyContent( Path basePath, Path destPath ) {
        copyContent( basePath, destPath, Lists.of( "**/*" ), Lists.of() );
    }

    public static void copyContent( Path basePath, Path destPath, List<String> includes, List<String> excludes ) {
        copyContent( basePath, destPath, includes, excludes, false, null );
    }

    public static void copyContent( Path basePath, Path destPath, List<String> includes, List<String> excludes,
                                    boolean filtering, Function<String, Object> mapper ) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( basePath.toFile() );
        scanner.setIncludes( includes.toArray( new String[0] ) );
        scanner.setExcludes( excludes.toArray( new String[0] ) );
        scanner.scan();
        for( String included : scanner.getIncludedFiles() ) {
            Path src = basePath.resolve( included );
            Path dst = destPath.resolve( included );
            if( filtering ) Files.writeString( dst, Strings.substitute( Files.readString( src ), mapper ) );
            else copy( src, Encoding.PLAIN, dst, Encoding.PLAIN );

            if( !Resources.IS_WINDOWS )
                setPosixPermissions( dst, getPosixPermissions( src ) );
        }
    }


    public static void rename( Path sourcePath, Path destPath ) {
        try {
            ensureFile( destPath );
            java.nio.file.Files.move( sourcePath, destPath, ATOMIC_MOVE, REPLACE_EXISTING );
        } catch( IOException e ) {
            throw new UncheckedIOException( "cannot rename " + sourcePath + " to " + destPath, e );
        }
    }

    public static void ensureFile( Path path ) throws UncheckedIOException {
        ensureDirectory( path.getParent() );
    }

    public static Path ensureDirectory( Path path ) throws UncheckedIOException {
        try {
            java.nio.file.Files.createDirectories( path );
            return path;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void move( Path source, Path target, CopyOption... options ) throws UncheckedIOException {
        try {
            Files.ensureFile( target );

            java.nio.file.Files.move( source, target, options );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void setPosixPermissions( Path path, Set<PosixFilePermission> permissions ) throws UncheckedIOException {
        try {
            java.nio.file.Files.setPosixFilePermissions( path, permissions );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void setPosixPermissions( Path path, PosixFilePermission... permissions ) {
        setPosixPermissions( path, Sets.of( permissions ) );
    }

    public static Set<PosixFilePermission> getPosixPermissions( Path path ) {
        try {
            return java.nio.file.Files.getPosixFilePermissions( path, LinkOption.NOFOLLOW_LINKS );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static boolean isDirectoryEmpty( Path directory ) {
        try( DirectoryStream<Path> dirStream = java.nio.file.Files.newDirectoryStream( directory ) ) {
            return !dirStream.iterator().hasNext();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void setLastModifiedTime( Path path, DateTime dateTime ) {
        setLastModifiedTime( path, dateTime.getMillis() );
    }

    public static void setLastModifiedTime( Path path, long ms ) {
        try {
            java.nio.file.Files.setLastModifiedTime( path, FileTime.fromMillis( ms ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static String nameWithoutExtention( URL url ) {
        File path = new File( url.getPath() );
        return Strings.substringBeforeLast( path.getName(), "." );
    }

    public static long usableSpaceAtDirectory( Path path ) {
        ensureDirectory( path );
        return path.toFile().getUsableSpace();
    }

    public static boolean wildcardMatch( String filename, String wildcardMatcher ) {
        var wmPosition = 0;
        var fnPosition = 0;

        var mp = 0;
        var cp = 0;

        var fnLength = filename.length();
        var wmLength = wildcardMatcher.length();

        char wm = 0;

        while( fnPosition < fnLength ) {
            if( wmPosition >= wmLength ) break;
            if( ( wm = wildcardMatcher.charAt( wmPosition ) ) == '*' ) break;
            if( wm != filename.charAt( fnPosition ) && wm != '?' ) return false;

            wmPosition++;
            fnPosition++;
        }

        while( fnPosition < fnLength ) {
            if( ( wmPosition < wmLength ) && ( wm = wildcardMatcher.charAt( wmPosition ) ) == '*' ) {
                if( ++wmPosition >= wmLength ) return true;

                mp = wmPosition;
                cp = fnPosition + 1;
            } else if( wm == filename.charAt( fnPosition ) || wm == '?' ) {
                wmPosition++;
                fnPosition++;
            } else {
                wmPosition = mp;
                fnPosition = cp++;
            }
        }

        boolean noend;

        while( ( noend = wmPosition < wmLength ) && wildcardMatcher.charAt( wmPosition ) == '*' ) {
            wmPosition++;
        }

        return !noend;
    }

    public static void ensureFileEncodingValid( Path path ) {
        try( InputStream is = IoStreams.in( path, Encoding.from( path ) ) ) {
            IOUtils.copy( is, NullOutputStream.NULL_OUTPUT_STREAM );
        } catch( Exception e ) {
            throw new InvalidFileEncodingException( path, e );
        }
    }

    public static boolean isFileEncodingValid( Path path ) {
        try {
            ensureFileEncodingValid( path );
            return true;
        } catch( InvalidFileEncodingException e ) {
            log.trace( e.getMessage() );
            return false;
        }
    }

    @Deprecated
    public static boolean fileNotEmpty( Path path ) {
        return isFileNotEmpty( path );
    }

    public static boolean isFileNotEmpty( final Path path ) {
        try( InputStream is = IoStreams.in( path );
             InputStreamReader isr = new InputStreamReader( is );
             BufferedReader reader = new BufferedReader( isr ) ) {
            String line;
            while( ( line = reader.readLine() ) != null ) if( StringUtils.isNotEmpty( line ) ) return true;
            return false;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static boolean exists( Path path ) {
        return java.nio.file.Files.exists( path );
    }

    public static long getLastModifiedTime( Path path ) {
        try {
            return java.nio.file.Files.getLastModifiedTime( path ).toMillis();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static boolean createFile( Path file ) {
        try {
            java.nio.file.Files.createFile( file );
            return true;
        } catch( FileAlreadyExistsException e ) {
            return false;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static Optional<Path> resolve( String... paths ) {
        for( String path : paths ) {
            Path result = Path.of( path );
            if( result.toFile().exists() ) return Optional.of( result );
        }
        return Optional.empty();
    }

    public static Path format( Path base, String format, Map<String, Object> substitutions ) {
        return base.resolve( Strings.substitute( format, v -> {
            if( "HOST".equals( v ) ) return Inet.HOSTNAME;
            if( v.startsWith( "DATE:" ) ) return Time.format( v.substring( "DATE:".length() ), DateTimeZone.UTC, Dates.nowUtc() );
            return substitutions.get( v );
        } ) );
    }
}
