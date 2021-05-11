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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractEnvFixture<Self extends AbstractEnvFixture<Self>> extends AbstractScopeFixture<Self> {
    public static final String NO_PREFIX = "";
    private final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();
    protected final String prefix;

    public AbstractEnvFixture() {
        this( NO_PREFIX );
    }

    public AbstractEnvFixture( String prefix ) {
        this.prefix = prefix;
    }

    @SuppressWarnings( "unchecked" )
    public Self define( String property, Object value ) {
        properties.put( prefix + property, value );

        return ( Self ) this;
    }

    public Self definePort( String property ) {
        return define( property, portFor( property ) );
    }

    @Override
    protected void before() {
        properties.forEach( ( variableName, v ) -> {
            var value = Strings.substitute( String.valueOf( v ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            System.setProperty( variableName, value );
            ConfigImpl.reloadSystemPropertiesConfig();
        } );
    }

    public int portFor( Class<?> clazz ) {
        return portFor( clazz.getName() );
    }

    public int portFor( String key ) {
        synchronized( ports ) {
            return ports.computeIfAbsent( prefix + key, k -> {
                try( var socket = new ServerSocket() ) {
                    socket.setReuseAddress( true );
                    socket.bind( new InetSocketAddress( 0 ) );
                    var localPort = socket.getLocalPort();
                    log.debug( "{} finding port for key={}... port={}", this.getClass().getSimpleName(), k, localPort );
                    return localPort;
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            } );
        }
    }
}
