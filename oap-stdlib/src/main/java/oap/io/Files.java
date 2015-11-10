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

import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Files {
    public static ArrayList<Path> wildcard( String basePath, String wildcard ) {
        return wildcard( path( basePath ), wildcard );
    }

    public static ArrayList<Path> wildcard( Path basePath, String wildcard ) {
        try {
            PathMatcher pm = FileSystems.getDefault()
                .getPathMatcher( ("glob:" + basePath + File.separator + wildcard).replace( "\\", "\\\\" ) );
            ArrayList<Path> result = new ArrayList<>();
            SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                    if( pm.matches( file ) ) result.add( file );
                    return FileVisitResult.CONTINUE;
                }
            };
            if( basePath.toFile().exists() && basePath.toFile().canExecute() )
                java.nio.file.Files.walkFileTree( basePath, visitor );
            Collections.sort( result );
            return result;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static String version( Path path ) {
        return Optional.of( path.getFileName().toString() ).map( s -> {
            Matcher m = java.util.regex.Pattern.compile( "_(v.*?)-" ).matcher( s );
            return m.find() ? m.group( 1 ) : "";
        } ).get();
    }

    public static String readString( String path ) {
        return readString( path( path ), IoStreams.Encoding.PLAIN );
    }

    public static String readString( Path path ) {
        return readString( path, IoStreams.Encoding.PLAIN );
    }

    public static String readString( Path path, IoStreams.Encoding encoding ) {
        try( InputStream in = IoStreams.in( path, encoding ) ) {
            return Strings.readString( in );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static byte[] read( Path path ) {
        try {
            return java.nio.file.Files.readAllBytes( path );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void writeString( String path, String value ) {
        writeString( path( path ), value );
    }

    public static void writeString( Path path, String value ) {
        writeString( path, IoStreams.Encoding.PLAIN, value );
    }

    public static void writeString( Path path, IoStreams.Encoding encoding, String value ) {
        IoStreams.write( path, encoding, value );
    }

    public static Path path( String path, String... more ) {
        return FileSystems.getDefault().getPath( path, more );
    }

    public static Stream<String> lines( Path path ) {
        try {
            return Stream.of( java.nio.file.Files.lines( path ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void copyDirectory( Path from, Path to ) {
        try {
            FileUtils.copyDirectory( from.toFile(), to.toFile() );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void delete( Path path ) {
        try {
            if( path.toFile().exists() )
                java.nio.file.Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile( Path path, BasicFileAttributes attrs ) throws IOException {
                        java.nio.file.Files.delete( path );
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory( Path path, IOException exc ) throws IOException {
                        java.nio.file.Files.delete( path );
                        return FileVisitResult.CONTINUE;
                    }

                } );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void copy( Path sourcePath, IoStreams.Encoding sourceEncoding,
        Path destPath, IoStreams.Encoding destEncoding ) {
        destPath.getParent().toFile().mkdirs();
        try( InputStream is = IoStreams.in( sourcePath, sourceEncoding );
             OutputStream os = IoStreams.out( destPath, destEncoding ) ) {
            IOUtils.copy( is, os );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void chmod( Path path, PosixFilePermission... permissions ) {
        try {
            java.nio.file.Files.setPosixFilePermissions( path, Sets.of( permissions ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void rename( Path sourcePath, Path destPath ) {
        if( destPath.toFile().exists() ) delete( destPath );
        if( !sourcePath.toFile().renameTo( destPath.toFile() ) )
            throw new UncheckedIOException( new IOException( "cannot rename " + sourcePath + " to " + destPath ) );
    }
}
