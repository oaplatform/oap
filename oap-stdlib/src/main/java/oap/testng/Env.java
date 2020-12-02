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
import org.apache.commons.lang3.NotImplementedException;

import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import static oap.testng.TestDirectoryFixture.testDirectory;
import static oap.testng.TestDirectoryFixture.testPath;
import static oap.testng.TestDirectoryFixture.testUri;
import static oap.testng.TestDirectoryFixture.testUrl;

@Deprecated
public class Env {

    @Deprecated
    public static final String LOCALHOST = Inet.LOCALHOST_NAME;
    /**
     * @see TestDirectoryFixture#testDirectory()
     */
    @Deprecated
    public static final Path tmpRoot = testDirectory();

    @Deprecated
    public static String tmp( String name ) {
        return testPath( name ).toString();
    }

    @Deprecated
    public static URI tmpURI( String name ) {
        return testUri( name );
    }

    @Deprecated
    public static URL tmpURL( String name ) {
        return testUrl( name );
    }

    /**
     * @see TestDirectoryFixture#testPath(String)
     */
    @Deprecated
    public static Path tmpPath( String name ) {
        return testPath( name );
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

    @Deprecated
    public static int port() {
        throw new NotImplementedException( "this thing is deprecated" );
    }

    @Deprecated
    public static int port( String key ) {
        throw new NotImplementedException( "this thing is deprecated" );
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

    @Deprecated
    public static void resetPorts() {
        throw new NotImplementedException( "this thing is deprecated" );
    }

    @Deprecated
    public static void putEnv( String name, String value ) {
        oap.system.Env.set( name, value );
    }
}
