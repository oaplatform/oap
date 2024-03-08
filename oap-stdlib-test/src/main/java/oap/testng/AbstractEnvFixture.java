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
import oap.concurrent.Threads;
import oap.concurrent.atomic.FileAtomicLong;
import oap.io.Sockets;
import oap.util.Strings;

import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static oap.testng.Asserts.locationOfTestResource;

@Slf4j
public abstract class AbstractEnvFixture<Self extends AbstractEnvFixture<Self>> extends AbstractScopeFixture<Self> {
    public static final String NO_PREFIX = "";
    public static final FileAtomicLong LAST_PORT = new FileAtomicLong( "/tmp/port.lock", 1, 10000 );
    private static final ConcurrentMap<String, Integer> ports = new ConcurrentHashMap<>();
    protected final String prefix;
    private final ConcurrentMap<String, Object> properties = new ConcurrentHashMap<>();

    private final IdentityHashMap<AbstractEnvFixture<?>, AbstractEnvFixture<?>> dependencies = new IdentityHashMap<>();

    public AbstractEnvFixture( AbstractEnvFixture<?>... dependencies ) {
        this( NO_PREFIX, dependencies );
    }

    public AbstractEnvFixture( String prefix, AbstractEnvFixture<?>... dependencies ) {
        Preconditions.checkNotNull( prefix );

        this.prefix = prefix;

        for( var dependency : dependencies ) {
            addDependency( dependency );
            dependency.addDependency( this );
        }
    }

    private void addDependency( AbstractEnvFixture<?> env ) {
        this.dependencies.put( env, env );
    }

    private void clearProperties() {
        if( !prefix.isEmpty() ) {
            Set<String> propertyNames = properties.keySet();
            for( var propertyName : propertyNames ) {
                System.clearProperty( propertyName );
            }

            ConfigImpl.reloadSystemPropertiesConfig();
        }
    }

    @SuppressWarnings( "unchecked" )
    public Self define( String property, Object value ) {
        properties.put( prefix + property, value );

        return ( Self ) this;
    }

    @SuppressWarnings( "unchecked" )
    public <T> T getProperty( String name ) {
        return ( T ) properties.get( prefix + name );
    }

    public Self definePort( String property ) throws UncheckedIOException {
        return define( property, portFor( property ) );
    }

    public Self defineLocalClasspath( String property, Class<?> clazz, String resourceName ) {
        return define( property, "classpath(" + locationOfTestResource( clazz, resourceName ) + ")" );
    }

    public Self defineClasspath( String property, String resourceLocation ) {
        return define( property, "classpath(" + resourceLocation + ")" );
    }

    public Self definePath( String property, Path path ) {
        return define( property, "path(" + path + ")" );
    }

    public Self defineURL( String property, URL url ) {
        return define( property, "url(" + url + ")" );
    }

    @Override
    public void start() {
        syncProperties();

        for( var env : dependencies.values() ) {
            env.syncProperties();
        }
    }

    @Override
    public void shutdown() {
        clearProperties();
    }

    protected void syncProperties() {
        clearProperties();
        setProperties();
        ConfigImpl.reloadSystemPropertiesConfig();
    }

    private void setProperties() {
        properties.forEach( ( variableName, v ) -> {
            var value = Strings.substitute( String.valueOf( v ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            System.setProperty( variableName, value );
        } );
    }

    public int portFor( Class<?> clazz ) throws UncheckedIOException {
        return portFor( clazz.getName() );
    }

    public int portFor( String key ) throws UncheckedIOException {
        return Threads.withThreadName( getUniqueName(), () -> {
            synchronized( LAST_PORT ) {
                return ports.computeIfAbsent( prefix + key, k -> {
                    int port;
                    do {
                        port = ( int ) LAST_PORT.updateAndGet( previousPort -> previousPort > 30000 ? 10000
                            : previousPort + 1 );
                    } while( !Sockets.isTcpPortAvailable( port ) );

                    log.debug( "{} finding port for key={}... port={}", this.getClass().getSimpleName(), k, port );
                    return port;
                } );
            }
        } );
    }
}
