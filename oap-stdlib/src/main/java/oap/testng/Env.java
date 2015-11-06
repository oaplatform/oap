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

import com.google.common.base.Throwables;
import oap.io.Files;
import oap.io.Resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Env {
    public static final String LOCALHOST;
    static final Path tmp = Files.path( "/tmp/test" );
    public static final Path tmpRoot = tmp.resolve( "temp-" + System.currentTimeMillis() );
    private static Map<String, Integer> ports = new ConcurrentHashMap<>();

    static {
        try {
            LOCALHOST = InetAddress.getByName( "127.0.0.1" ).getCanonicalHostName();
        } catch( UnknownHostException e ) {
            throw Throwables.propagate( e );
        }
    }

    public static String tmp( String name ) {
        return tmpPath( name ).toString();
    }

    public static Path tmpPath( String name ) {
        return tmpRoot.resolve( name.startsWith( "/" ) || name.startsWith( "\\" ) ? name.substring( 1 ) : name );
    }

    public static Path deployTestData( Class<?> contextClass ) {
        return deployTestData( contextClass, "" );
    }

    public static Path deployTestData( Class<?> contextClass, String name ) {
        Path to = tmpPath( name );
        Resources.filePath( contextClass, contextClass.getSimpleName() + "/" + name )
            .ifPresent( path -> {
                System.out.println( path );
                Files.copyDirectory( path, to );
            } );
        return to;
    }

    public static int port() {
        return port( "DEFAULT" );
    }

    public static int port( String key ) {
        return ports.computeIfAbsent( key, k -> {
            try( ServerSocket socket = new ServerSocket( 0 ) ) {
                return socket.getLocalPort();
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }
}
