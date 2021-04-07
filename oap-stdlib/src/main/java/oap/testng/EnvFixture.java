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


import com.google.common.base.Preconditions;
import com.typesafe.config.impl.ConfigImpl;
import lombok.extern.slf4j.Slf4j;
import oap.system.Env;
import oap.util.Strings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class EnvFixture<TEnvFixture extends EnvFixture<TEnvFixture>> extends FixtureWithScope<TEnvFixture> {
    private final ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();
    protected Kind kind = Kind.JAVA;

    public EnvFixture() {
    }

    @SuppressWarnings( "unchecked" )
    public TEnvFixture define( String property, Object value ) {
        properties.put( property, value );

        return ( TEnvFixture ) this;
    }

    public Optional<Object> getProperty( String property ) {
        return Optional.ofNullable( properties.get( property ) );
    }

    public TEnvFixture definePort( String property, String portKey ) {
        return define( property, portFor( portKey ) );
    }

    @SuppressWarnings( "unchecked" )
    public TEnvFixture importEnv( EnvFixture<?> envFixture ) {
        envFixture.ports.forEach( ports::putIfAbsent );
        envFixture.properties.forEach( properties::putIfAbsent );

        return ( TEnvFixture ) this;
    }

    @SuppressWarnings( "unchecked" )
    public TEnvFixture withKind( Kind kind ) {
        Preconditions.checkNotNull( kind );

        this.kind = kind;

        return ( TEnvFixture ) this;
    }

    @Override
    protected void before() {
        properties.forEach( ( variableName, v ) -> {
            var value = Strings.substitute( String.valueOf( v ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );

            switch( kind ) {
                case JAVA -> {
                    log.debug( "system property {} = {}", variableName, value );
                    System.setProperty( variableName, value );
                }
                case ENV -> {
                    log.debug( "env property {} = {}", variableName, value );
                    Env.set( variableName, value );
                }
                case MAP -> log.debug( "map property {} = {}", variableName, value );
                default -> throw new IllegalStateException( "Unknown kind " + kind );
            }
        } );

        switch( kind ) {
            case ENV:
                ConfigImpl.reloadEnvVariablesConfig();
                break;
            case JAVA:
                ConfigImpl.reloadSystemPropertiesConfig();
                break;
            default:
        }
    }

    @Override
    protected void after() {
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
                    log.debug( "{} finding port for key={}... port={}", this.getClass().getSimpleName(), k, localPort );
                    return localPort;
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            } );
        }
    }

    protected Map<String, Object> getProperties() {
        return Collections.unmodifiableMap( properties );
    }

    public enum Kind {
        JAVA, ENV, MAP
    }
}
