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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import oap.concurrent.Executors;
import oap.util.Lists;
import oap.util.Sets;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Try;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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

    @SneakyThrows
    public static List<URL> urls( Class<?> contextClass, String name ) {
        if( name.startsWith( "/" ) ) name = name.substring( 1 );
        else name = resolveName( contextClass ) + "/" + name;
        return Collections.list( contextClass.getClassLoader().getResources( name ) );
    }

    private static String resolveName( Class<?> contextClass ) {
        Class<?> c = contextClass;
        while( c.isArray() ) {
            c = c.getComponentType();
        }
        var baseName = c.getPackageName();
        return baseName.replace( '.', '/' );
    }

    public static Optional<String> readString( Class<?> contextClass, String name ) {
        try( InputStream is = contextClass.getResourceAsStream( name ) ) {
            return is == null ? Optional.empty()
                : Optional.of( Strings.readString( is ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static String readStringOrThrow( Class<?> contextClass, String name ) {
        return Resources.readString( contextClass, name )
            .orElseThrow( () -> new IllegalArgumentException( "resource not found " + name + " for context class " + contextClass ) );
    }

    public static Optional<byte[]> read( Class<?> contextClass, String name ) {
        try( InputStream is = contextClass.getResourceAsStream( name ) ) {
            return is == null ? Optional.empty()
                : Optional.of( ByteStreams.toByteArray( is ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static List<String> readStrings( Class<?> contextClass, String name ) {
        return Lists.map( urls( name ), Try.map( Strings::readString ) );
    }

    public static List<String> readStrings( String name ) {
        return Lists.map( urls( name ), Try.map( Strings::readString ) );
    }

    public static List<String> readLines( String name ) {
        List<String> result = new ArrayList<>();
        for( URL url : urls( name ) ) result.addAll( Strings.readLines( url ) );
        return result;
    }

    public static List<URL> urls( String name ) {
        try {
            return Collections.list( Thread.currentThread().getContextClassLoader().getResources( name ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static List<URL> urls( String atPackage, String ext ) {
        final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat( "reflections-%d" ).build()
        );
        try {
            final ConfigurationBuilder configuration = new ConfigurationBuilder()
                .setUrls( ClasspathHelper.forPackage( atPackage ) )
                .setScanners( new ResourcesScanner() )
                .filterInputsBy( new FilterBuilder().includePackage( atPackage ) )
                .setExecutorService( executorService );
            final Reflections reflections = new Reflections( configuration );

            final Set<String> resources = reflections.getResources( in -> in.endsWith( "." + ext ) );
            return new ArrayList<>( Sets.map( resources, r -> Thread.currentThread().getContextClassLoader().getResource( r ) ) );
        } finally {
            executorService.shutdown();
        }
    }

    public static Optional<Stream<String>> lines( Class<?> contextClass, String name ) {
        return url( contextClass, name ).map( IoStreams::lines );
    }

    public static Stream<String> lines( String name ) {
        return Stream.of( urls( name ) ).flatMap( IoStreams::lines );
    }

    public static Optional<Properties> readProperties( Class<?> contextClass, String name ) {
        return Resources.read( contextClass, name ).map( bytes -> {
            try {
                Properties properties = new Properties();
                properties.load( new ByteArrayInputStream( bytes ) );
                return properties;
            } catch( IOException ignore ) {
                return null;
            }
        } );
    }
}
