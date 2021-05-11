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

package oap.application.testng;

import com.google.common.base.Preconditions;
import oap.application.ApplicationConfiguration;
import oap.application.Kernel;
import oap.application.module.Module;
import oap.io.Resources;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.testng.AbstractEnvFixture;
import oap.testng.TestDirectoryFixture;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static oap.http.testng.HttpAsserts.httpPrefix;
import static oap.testng.TestDirectoryFixture.testDirectory;

public abstract class AbstractKernelFixture<Self extends AbstractKernelFixture<Self>> extends AbstractEnvFixture<Self> {
    public static final String ANY = "*";
    public static final String TEST_REMOTING_PORT = "TEST_REMOTING_PORT";
    public static final String TEST_HTTP_PORT = "TEST_HTTP_PORT";
    public static final String TEST_DIRECTORY = "TEST_DIRECTORY";
    public static final String TEST_RESOURCE_PATH = "TEST_RESOURCE_PATH";
    public static final String TEST_HTTP_PREFIX = "TEST_HTTP_PREFIX";
    private static int kernelN = 0;
    protected final URL conf;
    protected final List<URL> additionalModules = new ArrayList<>();
    private final LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
    public Kernel kernel;
    protected Path confd;

    public AbstractKernelFixture( String prefix, URL conf ) {
        this( prefix, Scope.METHOD, conf, null, List.of() );
    }

    public AbstractKernelFixture( String prefix, URL conf, Path confd ) {
        this( prefix, Scope.METHOD, conf, confd, List.of() );
    }

    public AbstractKernelFixture( String prefix, URL conf, List<URL> additionalModules ) {
        this( prefix, Scope.METHOD, conf, null, additionalModules );
    }

    public AbstractKernelFixture( String prefix, Scope scope, URL conf, Path confd, List<URL> additionalModules ) {
        super( prefix );
        this.scope = scope;
        this.conf = conf;
        this.confd = confd;
        this.additionalModules.addAll( additionalModules );

        defineDefaults();
    }

    @SuppressWarnings( "unchecked" )
    public Self withFileProperties( URL location ) {
        var map = Binder.Format.of( location, true ).binder.unmarshal( new TypeRef<Map<String, Object>>() {}, location );
        properties.putAll( map );

        return ( Self ) this;
    }

    public int defaultHttpPort() {
        return portFor( TEST_HTTP_PORT );
    }

    protected void defineDefaults() {
        define( TEST_REMOTING_PORT, portFor( TEST_REMOTING_PORT ) );
        var testHttpPort = portFor( TEST_HTTP_PORT );
        define( TEST_HTTP_PORT, testHttpPort );
        define( TEST_DIRECTORY, testDirectory() );
        String resourcePath = Resources.path( getClass(), "/" ).orElseThrow();
        define( TEST_RESOURCE_PATH, resourcePath );
        define( TEST_HTTP_PREFIX, httpPrefix( testHttpPort ) );
//        deprecated
        define( "HTTP_PREFIX", httpPrefix( testHttpPort ) );
    }

    @SuppressWarnings( "unchecked" )
    public Self withConfdResources( Class<?> clazz, String confdResource ) {
        this.confd = TestDirectoryFixture.testPath( "/application.test.confd" );

        Resources.filePaths( clazz, confdResource )
            .forEach( path -> oap.io.Files.copyDirectory( path, this.confd ) );

        return ( Self ) this;
    }

    @Nonnull
    public <T> T service( @Nonnull String moduleName, @Nonnull Class<T> clazz ) {
        return kernel.serviceOfClass( moduleName, clazz )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + moduleName + ":" + clazz ) );
    }

    @Nonnull
    public <T> T service( @Nonnull String moduleName, @Nonnull String serviceName ) {
        return kernel.<T>service( moduleName, serviceName )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + moduleName + ":" + serviceName ) );
    }

    @Nonnull
    public <T> T service( @Nonnull String reference ) {
        return kernel.<T>service( reference )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + reference ) );
    }

    public <T> List<T> ofClass( Class<T> clazz ) {
        return kernel.ofClass( clazz );
    }

    public <T> List<T> ofClass( String moduleName, Class<T> clazz ) {
        return kernel.ofClass( moduleName, clazz );
    }

    @Override
    protected void before() {
        defineDefaults();

        Preconditions.checkArgument( this.kernel == null );
        super.before();

        var moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
        moduleConfigurations.addAll( additionalModules );
        this.kernel = new Kernel( "FixtureKernel#" + kernelN++ + "#" + prefix, moduleConfigurations );

        var confds = ApplicationConfiguration.getConfdUrls( confd );

        this.kernel.start( ApplicationConfiguration.load( conf, confds, properties ) );
    }

    @Override
    protected void after() {
        this.kernel.stop();
        this.kernel = null;
        super.after();
    }
}
