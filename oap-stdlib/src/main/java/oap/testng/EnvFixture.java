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


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.extern.slf4j.Slf4j;
import oap.util.Pair;
import oap.util.Strings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

import static oap.testng.Fixture.Scope.CLASS;
import static oap.testng.Fixture.Scope.METHOD;
import static oap.testng.Fixture.Scope.SUITE;
import static oap.util.Pair.__;

@Slf4j
public class EnvFixture implements Fixture {
    public static final String TEST_HTTP_PORT = "TEST_HTTP_PORT";

    private final ListMultimap<Scope, Pair<String, Object>> properties = ArrayListMultimap.create();
    private final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();

    public EnvFixture define( String property, Object value ) {
        return define( METHOD, property, value );
    }

    public EnvFixture define( Scope scope, String property, Object value ) {
        properties.get( scope ).add( __( property, value ) );
        return this;
    }

    public EnvFixture definePort( String property, String portKey ) {
        return define( property, portFor( portKey ) );
    }

    private void init( Scope scope ) {
        properties.get( scope ).forEach( p -> {
            String value = Strings.substitute( String.valueOf( p._2 ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            log.debug( "system property {} = {}", p._1, value );
            System.setProperty( p._1, value );
        } );
    }

    @Override
    public void beforeSuite() {
        init( SUITE );
    }

    @Override
    public void beforeClass() {
        init( CLASS );
    }

    @Override
    public void beforeMethod() {
        init( METHOD );
    }

    @Override
    public void afterMethod() {
        clearPorts();
    }

    public int defaultHttpPort() {
        return portFor( TEST_HTTP_PORT );
    }

    public int portFor( Class<?> clazz ) {
        return portFor( clazz.getName() );
    }

    public int portFor( String key ) {
        return ports.computeIfAbsent( key, k -> {
            try( var socket = new ServerSocket() ) {
                socket.setReuseAddress( true );
                socket.bind( new InetSocketAddress( 0 ) );
                var localPort = socket.getLocalPort();
                log.debug( "finding port for key=" + key + "... port=" + localPort );
                return localPort;
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        } );
    }

    public void clearPorts() {
        log.debug( "clear ports" );
        ports.clear();
    }
}
