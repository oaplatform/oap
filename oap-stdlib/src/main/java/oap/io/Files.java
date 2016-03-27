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

import lombok.extern.slf4j.Slf4j;
import oap.util.Lists;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
public final class Files {
   @Deprecated
   public static ArrayList<Path> fastWildcard( String basePath, String wildcard ) {
      return wildcard( Paths.get( basePath ), wildcard );
   }

   @Deprecated
   public static ArrayList<Path> fastWildcard( Path basePath, String wildcard ) {
      return wildcard( basePath, wildcard );
   }

   public static ArrayList<Path> wildcard( String basePath, String wildcard ) {
      return wildcard( Paths.get( basePath ), wildcard );
   }

   public static ArrayList<Path> wildcard( Path basePath, String wildcard ) {
      final ArrayList<Path> result = new ArrayList<>();
      new FileWalker( basePath, wildcard ).walkFileTree( result::add );
      return result;
   }

   @SuppressWarnings( "unchecked" )
   public static <T> T readObject( Path path ) {
      try( ObjectInputStream is = new ObjectInputStream( IoStreams.in( path, IoStreams.Encoding.PLAIN ) ) ) {
         return ( T ) is.readObject();
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      } catch( ClassNotFoundException e ) {
         throw new UncheckedIOException( new IOException( e ) );
      }
   }

   public static String readString( String path ) {
      return readString( Paths.get( path ), IoStreams.Encoding.PLAIN );
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
      writeString( Paths.get( path ), value );
   }

   public static void writeString( Path path, String value ) {
      writeString( path, IoStreams.Encoding.PLAIN, value );
   }

   public static void writeString( Path path, IoStreams.Encoding encoding, String value ) {
      IoStreams.write( path, encoding, value );
   }

   public static void writeObject( Path path, Object value ) {
      try( ObjectOutputStream os = new ObjectOutputStream( IoStreams.out( path, IoStreams.Encoding.PLAIN ) ) ) {
         os.writeObject( value );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   @Deprecated
   public static Path path( String path, String... more ) {
      return Paths.get( path, more );
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
      scanner.setIncludes( includes.toArray( new String[includes.size()] ) );
      scanner.setExcludes( excludes.toArray( new String[excludes.size()] ) );
      scanner.scan();
      for( String included : scanner.getIncludedFiles() ) {
         Path src = basePath.resolve( included );
         Path dst = destPath.resolve( included );
         if( filtering ) Files.writeString( dst, Strings.substitute( Files.readString( src ), mapper ) );
         else copy( src, IoStreams.Encoding.PLAIN, dst, IoStreams.Encoding.PLAIN );
         setPosixPermissions( dst, getPosixPermissions( src ) );
      }
   }

   public static void setPosixPermissions( Path path, Set<PosixFilePermission> permissions ) {
      try {
         java.nio.file.Files.setPosixFilePermissions( path, permissions );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }

   }

   public static void rename( Path sourcePath, Path destPath ) {
      try {
         java.nio.file.Files.move( sourcePath, destPath, ATOMIC_MOVE, REPLACE_EXISTING );
      } catch( IOException e ) {
         throw new UncheckedIOException( "cannot rename " + sourcePath + " to " + destPath, e );
      }
   }

   public static void ensureFile( Path path ) {
      ensureDirectory( path.getParent() );
   }

   public static void ensureDirectory( Path path ) {
      try {
         java.nio.file.Files.createDirectories( path );
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
}
