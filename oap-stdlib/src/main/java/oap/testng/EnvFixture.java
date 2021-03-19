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


import com.typesafe.config.impl.ConfigImpl;
import lombok.extern.slf4j.Slf4j;
import oap.util.Strings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import static oap.testng.Fixture.Scope.CLASS;
import static oap.testng.Fixture.Scope.METHOD;
import static oap.testng.Fixture.Scope.SUITE;

@Slf4j
public class EnvFixture implements Fixture {
    private static final HashMap<String, Integer> ports = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    protected Scope scope = METHOD;

    public EnvFixture define( String property, Object value ) {
        properties.put( property, value );
        return this;
    }

    public EnvFixture definePort( String property, String portKey ) {
        return define( property, portFor( portKey ) );
    }

    private void init() {
        properties.forEach( ( n, v ) -> {
            String value = Strings.substitute( String.valueOf( v ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            log.debug( "system property {} = {}", n, value );
            System.setProperty( n, value );
        } );

        ConfigImpl.reloadEnvVariablesConfig();
        ConfigImpl.reloadSystemPropertiesConfig();
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public void beforeSuite() {
        if( scope == SUITE ) init();
    }

    @Override
    public void beforeClass() {
        if( scope == CLASS ) init();
    }

    @Override
    public void beforeMethod() {
        if( scope == METHOD ) init();
    }

    @Override
    public void afterMethod() {
        if( scope == METHOD ) clearPorts();
    }

    @Override
    public void afterClass() {
        if( scope == CLASS ) clearPorts();
    }

    @Override
    public void afterSuite() {
        if( scope == SUITE ) clearPorts();
    }

    public int portFor( Class<?> clazz ) {
        return portFor( clazz.getName() );
    }

    public int portFor( String key ) {
        synchronized( ports ) {
            return ports.computeIfAbsent( key, k -> {
                try( var socket = new ServerSocket() ) {
                    socket.setReuseAddress( true );
                    socket.bind( new InetSocketAddress( 0 ) );
                    var localPort = socket.getLocalPort();
                    log.debug( "{} finding port for key={}... port={}", this.getClass().getSimpleName(), key, localPort );
                    return localPort;
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            } );
        }
    }

    public void clearPorts() {
        synchronized( ports ) {
            log.debug( "clear ports" );
            ports.clear();
        }
    }
}
