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
package oap.testng;

import lombok.SneakyThrows;
import oap.net.Inet;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static oap.testng.TestDirectoryFixture.testDirectory;


public class Env {

    @Deprecated
    public static final String LOCALHOST = Inet.LOCALHOST_NAME;
    /**
     * @see TestDirectoryFixture#testDirectory()
     */
    @Deprecated
    public static final Path tmpRoot = testDirectory();
    private static final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();


    @Deprecated
    public static String tmp( String name ) {
        return TestDirectoryFixture.testPath( name ).toString();
    }

    public static URI tmpURI( String name ) {
        return TestDirectoryFixture.testPath( name ).toUri();
    }

    @SneakyThrows
    public static URL tmpURL( String name ) {
        return tmpURI( name ).toURL();
    }

    /**
     * @see TestDirectoryFixture#testPath(String)
     */
    @Deprecated
    public static Path tmpPath( String name ) {
        return TestDirectoryFixture.testPath( name );
    }

    /**
     * @see TestDirectoryFixture#deployTestData(Class)
     */
    @Deprecated
    public static Path deployTestData( Class<?> contextClass ) {
        return TestDirectoryFixture.deployTestData( contextClass );
    }

    /**
     * @see TestDirectoryFixture#deployTestData(Class, String)
     */
    @Deprecated
    public static Path deployTestData( Class<?> contextClass, String name ) {
        return TestDirectoryFixture.deployTestData( contextClass, name );
    }

    public static int port() {
        return port( "DEFAULT" );
    }

    public static int port( String key ) {
        return ports.computeIfAbsent( key, k -> {
            try( var socket = new ServerSocket( 0 ) ) {
                var localPort = socket.getLocalPort();
                System.out.println( "ENV::key=" + key + "; port = " + localPort );
                return localPort;
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }

    @SneakyThrows
    @Deprecated
    public static ServerSocket serverSocket() {
        return new ServerSocket( 0 );
    }

    /**
     * @see oap.system.Env#env(String, String)
     */
    @Deprecated
    public static String getEnvOrDefault( String name, String defaultValue ) {
        return oap.system.Env.env( name, defaultValue );
    }

    public static void resetPorts() {
        System.out.println( "ENV::ports = []" );
        ports.clear();
    }

    @SneakyThrows
    public static void putEnv( String name, String value ) {
        try {
            Class<?> processEnvironmentClass = Class.forName( "java.lang.ProcessEnvironment" );
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField( "theEnvironment" );
            theEnvironmentField.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            var env = ( Map<Object, Object> ) theEnvironmentField.get( null );

            if( SystemUtils.IS_OS_WINDOWS )
                if( value == null ) env.remove( name );
                else env.put( name, value );
            else {
                var variableClass = Class.forName( "java.lang.ProcessEnvironment$Variable" );
                var convertToVariable = variableClass.getMethod( "valueOf", String.class );
                convertToVariable.setAccessible( true );

                var valueClass = Class.forName( "java.lang.ProcessEnvironment$Value" );
                var convertToValue = valueClass.getMethod( "valueOf", String.class );
                convertToValue.setAccessible( true );

                if( value == null ) env.remove( convertToVariable.invoke( null, name ) );
                else env.put( convertToVariable.invoke( null, name ), convertToValue.invoke( null, value ) );
            }

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField( "theCaseInsensitiveEnvironment" );
            theCaseInsensitiveEnvironmentField.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            Map<String, String> cienv = ( Map<String, String> ) theCaseInsensitiveEnvironmentField.get( null );

            if( value == null ) cienv.remove( name );
            else cienv.put( name, value );
        } catch( NoSuchFieldException e ) {
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for( Class<?> clazz : classes )
                if( "java.util.Collections$UnmodifiableMap".equals( clazz.getName() ) ) {
                    Field field = clazz.getDeclaredField( "m" );
                    field.setAccessible( true );
                    Object obj = field.get( env );
                    @SuppressWarnings( "unchecked" )
                    Map<String, String> map = ( Map<String, String> ) obj;

                    if( value == null ) map.remove( name );
                    else map.put( name, value );
                }
        }
    }
}
