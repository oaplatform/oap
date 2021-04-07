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

import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import oap.io.content.ContentReader;
import oap.util.Lists;
import oap.util.Stream;
import oap.util.Try;
import org.apache.commons.collections4.EnumerationUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

public final class Resources {
    public static final boolean IS_WINDOWS = System.getProperty( "os.name" ).contains( "indow" );

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

    public static List<Path> filePaths( Class<?> contextClass, String name ) {
        return Lists.map( urls( contextClass, name ), u -> Paths.get( path( u ) ) );
    }

    public static Optional<URL> url( Class<?> contextClass, String name ) {
        return Optional.ofNullable( contextClass.getResource( name ) );
    }

    public static URL urlOrThrow( Class<?> contextClass, String name ) {
        return url( contextClass, name )
            .orElseThrow( () -> new IllegalArgumentException( "resource not found " + name + " for context class " + contextClass ) );
    }

    public static String resolveResourcePath( Class<?> contextClass, String resourceName ) {
        Class<?> c = contextClass;
        while( c.isArray() ) {
            c = c.getComponentType();
        }
        var baseName = c.getPackageName();
        return '/' + baseName.replace( '.', '/' ) + '/' + c.getSimpleName() + '/' + resourceName;
    }

    private static String resolveName( Class<?> contextClass ) {
        Class<?> c = contextClass;
        while( c.isArray() ) {
            c = c.getComponentType();
        }
        var baseName = c.getPackageName();
        return baseName.replace( '.', '/' );
    }

    /**
     * @see #readStrings(Class, String)
     * @deprecated
     */
    @Deprecated
    public static List<String> readResourcesAsString( Class<?> contextClass, String name ) {
        var ret = new ArrayList<String>();
        try {
            var resourceName = name.startsWith( "/" ) ? name.substring( 1 ) : contextClass.getPackageName().replace( '.', '/' ) + '/' + name;
            var resources = EnumerationUtils.toList( contextClass.getClassLoader().getResources( resourceName ) );
            for( var resource : resources )
                try( var is = resource.openStream() ) {
                    ret.add( ContentReader.read( is, ContentReader.ofString() ) );
                }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return ret;
    }

    /**
     * @see #read(Class, String, ContentReader)
     */
    @Deprecated
    public static Optional<String> readString( Class<?> contextClass, String name ) {
        return read( contextClass, name, ContentReader.ofString() );
    }

    public static <R> R readOrThrow( Class<?> contextClass, String name, ContentReader<R> reader ) {
        return read( contextClass, name, reader )
            .orElseThrow( () -> new IllegalArgumentException( "resource not found " + name + " for context class " + contextClass ) );

    }

    /**
     * @see #readOrThrow(Class, String, ContentReader)
     */
    @Deprecated
    public static String readStringOrThrow( Class<?> contextClass, String name ) {
        return readOrThrow( contextClass, name, ContentReader.ofString() );
    }

    public static <R> Optional<R> read( Class<?> contextClass, String name, ContentReader<R> reader ) {
        return url( contextClass, name ).map( url -> ContentReader.read( url, reader ) );
    }

    /**
     * @see #read(Class, String, ContentReader)
     */
    @Deprecated
    public static Optional<byte[]> read( Class<?> contextClass, String name ) {
        return read( contextClass, name, ContentReader.ofBytes() );
    }

    public static List<String> readStrings( Class<?> contextClass, String name ) {
        return Lists.map( urls( contextClass, name ), Try.map( url -> ContentReader.read( url, ContentReader.ofString() ) ) );
    }

    public static List<String> readStrings( String name ) {
        return Lists.map( urls( name ), Try.map( url -> ContentReader.read( url, ContentReader.ofString() ) ) );
    }

    public static List<String> readLines( String name ) {
        return Stream.of( urls( name ) )
            .<List<String>>foldLeft( new ArrayList<>(), ( l, url ) -> ContentReader.read( url, ContentReader.ofLines( l ) ) );
    }

    public static List<URL> urlsByExts( String... ext ) {
        List<String> extSet = dotPrefix( ext );
        return urls( name -> Lists.anyMatch( extSet, name::endsWith ) );
    }

    private static List<String> dotPrefix( String... ext ) {
        return Lists.map( ext, e -> e.startsWith( "." ) ? e : "." + e );
    }

    @SneakyThrows
    public static List<URL> urls( Class<?> contextClass, String name ) {
        return Collections.list( contextClass.getClassLoader()
            .getResources( name.startsWith( "/" )
                ? name.substring( 1 )
                : resolveName( contextClass ) + "/" + name ) );
    }

    @SneakyThrows
    public static List<URL> urls( String name ) {
        return Collections.list( Thread.currentThread().getContextClassLoader().getResources( name ) );
    }

    public static List<URL> urls( String atPackage, String... ext ) {
        List<String> extSet = dotPrefix( ext );
        String pkg = atPackage.replace( ".", "/" );
        return urls( name -> name.startsWith( pkg ) && Lists.anyMatch( extSet, name::endsWith ) );
    }


    @SuppressWarnings( "UnstableApiUsage" )
    @SneakyThrows
    public static List<URL> urls( Predicate<String> filter ) {
        return Stream.of( ClassPath.from( Thread.currentThread()
            .getContextClassLoader() )
            .getResources() )
            .filter( ri -> filter.test( ri.getResourceName() ) )
            .map( ClassPath.ResourceInfo::url )
            .toList();
    }

    /**
     * @see #read(Class, String, ContentReader)
     */
    @Deprecated
    public static Optional<Stream<String>> lines( Class<?> contextClass, String name ) {
        return read( contextClass, name, ContentReader.ofLinesStream() );
    }

    public static Stream<String> lines( String name ) {
        return Stream.of( urls( name ) ).flatMap( IoStreams::lines );
    }

    /**
     * @see #read(Class, String, ContentReader)
     */
    @Deprecated
    public static Optional<Properties> readProperties( Class<?> contextClass, String name ) {
        return read( contextClass, name, ContentReader.ofProperties() );
    }

    public static Properties readAllProperties( String name ) {
        return Stream.of( Resources.urls( name ) )
            .foldLeft( new Properties(), ( p, url ) -> ContentReader.read( url, ContentReader.ofProperties( p ) ) );
    }
}
