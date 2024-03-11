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
import oap.util.Lists;
import oap.util.Strings;

import javax.annotation.Nonnull;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static oap.testng.Asserts.locationOfTestResource;

@Slf4j
public abstract class AbstractFixture<Self extends AbstractFixture<Self>> {
    public static final int MIN_PORT_RANGE = 10000;
    public static final int MAX_PORT_RANGE = 30000;

    public static final FileAtomicLong LAST_PORT = new FileAtomicLong( "/tmp/port.lock", 1, 10000 );
    private static final ConcurrentMap<String, Integer> ports = new ConcurrentHashMap<>();
    protected static ConcurrentHashMap<Class<?>, AbstractFixture<?>> suiteScope = new ConcurrentHashMap<>();
    protected final String prefix;
    private final ConcurrentMap<String, Object> properties = new ConcurrentHashMap<>();
    private final ArrayList<AbstractFixture<?>> children = new ArrayList<>();
    protected Scope scope = Scope.METHOD;

    public AbstractFixture( Class<?> testClass, AbstractFixture<?>... children ) {
        Preconditions.checkNotNull( testClass );

        this.prefix = testClass.getCanonicalName().toUpperCase() + "-" + getClass().getCanonicalName().toUpperCase();

        Lists.addAll( this.children, children );
    }

    public AbstractFixture( String prefix, AbstractFixture<?>... children ) {
        Preconditions.checkNotNull( prefix );
        Preconditions.checkArgument( !prefix.isEmpty() );

        this.prefix = prefix;
        Lists.addAll( this.children, children );
    }

    protected void addChild( AbstractFixture<?> child ) {
        this.children.add( child );
    }

    @SuppressWarnings( "unchecked" )
    public Self withScope( Scope scope ) {
        this.scope = scope;

        if( scope == Scope.SUITE ) {
            return ( Self ) suiteScope.computeIfAbsent( getClass(), c -> this );
        }

        return ( Self ) this;
    }

    public final Scope getScope() {
        return scope;
    }

    public void beforeSuite() {
        if( scope == Scope.SUITE ) {
            children.forEach( AbstractFixture::before );
            before();
        }
    }

    public void afterSuite() {
        if( scope == Scope.SUITE ) {
            after();
            children.forEach( AbstractFixture::after );
        }
    }

    public void beforeClass() {
        if( scope == Scope.CLASS ) {
            children.forEach( AbstractFixture::before );
            before();
        }
    }

    public void afterClass() {
        if( scope == Scope.CLASS ) {
            after();
            children.forEach( AbstractFixture::after );
        }
    }

    public void beforeMethod() {
        if( scope == Scope.METHOD ) {
            children.forEach( AbstractFixture::before );
            before();
        }
    }

    public void afterMethod() {
        if( scope == Scope.METHOD ) {
            after();
            children.forEach( AbstractFixture::after );
        }
    }

    protected void before() {}

    protected void after() {}

    protected void clearProperties() {
        Set<String> propertyNames = properties.keySet();
        for( var propertyName : propertyNames ) {
            System.clearProperty( propertyName );
        }

        ConfigImpl.reloadSystemPropertiesConfig();

        children.forEach( AbstractFixture::clearProperties );
    }

    protected void setProperties() {
        children.forEach( AbstractFixture::setProperties );

        properties.forEach( ( variableName, v ) -> {
            var value = Strings.substitute( String.valueOf( v ),
                k -> System.getenv( k ) == null ? System.getProperty( k ) : System.getenv( k ) );
            log.trace( "set property {} -> {}", variableName, value );
            System.setProperty( variableName, value );
        } );
    }


    @SuppressWarnings( "unchecked" )
    public Self define( String property, Object value ) {
        properties.put( toFixturePropertyName( property ), value );

        return ( Self ) this;
    }

    @Nonnull
    public String toFixturePropertyName( String property ) {
        return prefix + "_FIXTURE_" + property;
    }

    public String toThreadName() {
        return "fixture-" + prefix;
    }

    @SuppressWarnings( "unchecked" )
    public <T> T getProperty( String name ) {
        return ( T ) properties.get( toFixturePropertyName( name ) );
    }

    public int definePort( String property ) throws UncheckedIOException {
        int port = portFor( property );
        define( property, port );
        return port;
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

    public int portFor( Class<?> clazz ) throws UncheckedIOException {
        return portFor( clazz.getName() );
    }

    public int portFor( String key ) throws UncheckedIOException {
        return Threads.withThreadName( toThreadName(), () -> {
            synchronized( LAST_PORT ) {
                return ports.computeIfAbsent( toFixturePropertyName( key ), k -> {
                    int port;
                    do {
                        port = ( int ) LAST_PORT.updateAndGet( previousPort ->
                            previousPort > MAX_PORT_RANGE ? MIN_PORT_RANGE : previousPort + 1 );
                    } while( !Sockets.isTcpPortAvailable( port ) );

                    log.debug( "{} finding port for key={}... port={}", this.getClass().getSimpleName(), k, port );
                    return port;
                } );
            }
        } );
    }

    public void initialize() {
        clearProperties();
        setProperties();
    }

    public void shutdown() {
        clearProperties();
    }

    public enum Scope {
        METHOD, CLASS, SUITE
    }
}
