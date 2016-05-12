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
import oap.util.*;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class Resources {
   private static final boolean IS_WINDOWS = System.getProperty( "os.name" ).contains( "indow" );

   @Deprecated
   public static Path deepPath( Path basePath, String name ) {
      return Files.deepPath( basePath, name );
   }

   private static String path( URL url ) {
      String filePath = url.getPath();
      return IS_WINDOWS && filePath.startsWith( "/" ) ? filePath.substring( 1 ) : filePath;
   }

   public static Optional<String> path( Class<?> contextClass, String name ) {
      return url( contextClass, name ).map( Resources::path );
   }

   public static Optional<Path> filePath( Class<?> contextClass, String name ) {
      return url( contextClass, name ).map( u -> Paths.get( path( u ) ) );
   }

   public static Optional<URL> url( Class<?> contextClass, String name ) {
      return Optional.ofNullable( contextClass.getResource( name ) );
   }

   public static Optional<String> readString( Class<?> contextClass, String name ) {
      try( InputStream is = contextClass.getResourceAsStream( name ) ) {
         return is == null ? Optional.empty()
            : Optional.of( Strings.readString( is ) );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }


   public static Optional<byte[]> read( Class<?> contextClass, String name ) {
      try( InputStream is = contextClass.getResourceAsStream( name ) ) {
         return is == null ? Optional.empty()
            : Optional.of( ByteStreams.toByteArray( is ) );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public static List<String> readStrings( String name ) {
      return Lists.map( urls( name ), Try.map( Strings::readString ) );
   }

   public static List<URL> urls( String name ) {
      try {
         return Collections.list( Thread.currentThread().getContextClassLoader().getResources( name ) );
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public static List<URL> urls( String atPackage, String ext ) {
      final Reflections reflections = new Reflections(
         new ConfigurationBuilder()
            .setUrls( ClasspathHelper.forPackage( atPackage ) )
            .setScanners( new ResourcesScanner() )
      );

      final Set<String> resources = reflections.getResources( name -> name.endsWith( "." + ext ) );
      return new ArrayList<>( Sets.map( resources, r -> Thread.currentThread().getContextClassLoader().getResource( r ) ) );
   }

   public static Optional<Stream<String>> lines( Class<?> contextClass, String name ) {
      return filePath( contextClass, name ).map( Files::lines );
   }
}
