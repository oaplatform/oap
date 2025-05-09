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

import lombok.extern.slf4j.Slf4j;

import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static oap.testng.Asserts.locationOfTestResource;

@Slf4j
public abstract class AbstractFixture<Self extends AbstractFixture<Self>> {
    protected final LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    protected final ArrayList<AbstractFixture<?>> children = new ArrayList<>();
    protected Scope scope = Scope.METHOD;

    protected AbstractFixture() {
    }

    protected void addChild( AbstractFixture<?> child ) {
        this.children.add( child );
    }

    @SuppressWarnings( "unchecked" )
    public Self withScope( Scope scope ) {
        this.scope = scope;

        if( scope == Scope.SUITE ) {
            throw new IllegalArgumentException( "use Fixtures#suiteFixture" );
        }

        children.forEach( f -> f.withScope( scope ) );

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


    @SuppressWarnings( "unchecked" )
    public Self define( String property, Object value ) {
        properties.put( property, value );

        return ( Self ) this;
    }

    public String toThreadName() {
        return "fixture-" + getClass();
    }

    @SuppressWarnings( "unchecked" )
    public <T> T getProperty( String name ) {
        return ( T ) properties.get( name );
    }

    public int definePort( String property ) throws UncheckedIOException {
        int port = Ports.getFreePort( getClass() );
        define( property, port );
        return port;
    }

    public Self defineLocalClasspath( String property, Class<?> clazz, String resourceName ) {
        return defineLocalClasspath( property, clazz, resourceName, false );
    }

    public Self defineLocalClasspath( String property, Class<?> clazz, String resourceName, boolean withSystemProperties ) {
        return define( property, ( withSystemProperties ? "classpath-wsp(" : "classpath(" ) + locationOfTestResource( clazz, resourceName ) + ")" );
    }

    public Self defineClasspath( String property, String resourceLocation ) {
        return defineClasspath( property, resourceLocation, false );
    }

    public Self defineClasspath( String property, String resourceLocation, boolean withSystemProperties ) {
        return define( property, ( withSystemProperties ? "classpath-wsp(" : "classpath(" ) + resourceLocation + ")" );
    }

    public Self definePath( String property, Path path ) {
        return define( property, "path(" + path + ")" );
    }

    public Self defineURL( String property, URL url ) {
        return define( property, "url(" + url + ")" );
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public enum Scope {
        METHOD, CLASS, SUITE
    }
}
