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

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import oap.util.Lists;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
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
import java.util.Set;
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

      int hash = Hashing.murmur3_32().hashBytes( name.getBytes() ).asInt();
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

   public static ArrayList<Path> wildcard( Path basePath, String wildcard ) {
      try {
         PathMatcher pm = FileSystems.getDefault()
            .getPathMatcher( ( "glob:" + basePath + File.separator + wildcard ).replace( "\\", "\\\\" ) );
         ArrayList<Path> result = new ArrayList<>();
         SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
               if( pm.matches( file ) ) result.add( file );
               return FileVisitResult.CONTINUE;
            }
         };
         if( java.nio.file.Files.exists( basePath ) && java.nio.file.Files.isExecutable( basePath ) )
            java.nio.file.Files.walkFileTree( basePath, visitor );
         Collections.sort( result );
         return result;
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public static List<Path> deepCollect( Path basePath, Predicate<Path> predicate ) {
      ArrayList<Path> result = new ArrayList<>();
      SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
         @Override
         public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
            if( predicate.test( file ) ) result.add( file );
            return FileVisitResult.CONTINUE;
         }
      };
      if( java.nio.file.Files.exists( basePath ) && java.nio.file.Files.isExecutable( basePath ) )
         try {
            java.nio.file.Files.walkFileTree( basePath, visitor );
         } catch( IOException e ) {
            throw new UncheckedIOException( e );
         }
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
      return readString( Paths.get( path ), IoStreams.Encoding.from( path ) );
   }

   public static String readString( Path path ) {
      return readString( path, IoStreams.Encoding.from( path ) );
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
      writeString( path, IoStreams.Encoding.from( path ), value );
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

   public static Stream<String> lines( Path path ) {
      return Stream.of( new BufferedReader( new InputStreamReader( IoStreams.in( path ) ) ).lines() );
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

   private static void copyOrAppend( Path sourcePath, IoStreams.Encoding sourceEncoding, Path destPath,
                                     IoStreams.Encoding destEncoding, int bufferSize, boolean append ) {
      destPath.getParent().toFile().mkdirs();
      try( InputStream is = IoStreams.in( sourcePath, sourceEncoding, bufferSize );
           OutputStream os = IoStreams.out( destPath, destEncoding, bufferSize, append ) ) {
         IOUtils.copy( is, os );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public static void copy( Path sourcePath, IoStreams.Encoding sourceEncoding,
                            Path destPath, IoStreams.Encoding destEncoding, int bufferSize ) {
      copyOrAppend( sourcePath, sourceEncoding, destPath, destEncoding, bufferSize, false );
   }

   public static void copy( Path sourcePath, IoStreams.Encoding sourceEncoding,
                            Path destPath, IoStreams.Encoding destEncoding ) {
      copy( sourcePath, sourceEncoding, destPath, destEncoding, IoStreams.DEFAULT_BUFFER );
   }

   public static void append(Path sourcePath, IoStreams.Encoding sourceEncoding,
                             Path destPath, IoStreams.Encoding destEncoding, int bufferSize) {
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

   public static boolean wildcardMatch( final String filename, final String wildcardMatcher ) {
      int wmPosition = 0;
      int fnPosition = 0;

      int mp = 0;
      int cp = 0;

      final int fnLength = filename.length();

      char wm;

      while( fnPosition < fnLength && ( ( wm = wildcardMatcher.charAt( wmPosition ) ) != '*' ) ) {
         if( wm != filename.charAt( fnPosition ) && wm != '?' ) {
            return false;
         }
         wmPosition++;
         fnPosition++;
      }

      final int wmLength = wildcardMatcher.length();

      while( fnPosition < fnLength ) {
         if( ( wm = wildcardMatcher.charAt( wmPosition ) ) == '*' ) {
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
}
