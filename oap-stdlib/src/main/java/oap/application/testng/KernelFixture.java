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
import oap.application.Module;
import oap.io.Resources;
import oap.testng.EnvFixture;
import oap.testng.TestDirectoryFixture;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.http.testng.HttpAsserts.httpPrefix;
import static oap.testng.TestDirectoryFixture.testDirectory;

public class KernelFixture extends EnvFixture {
    public static final String ANY = "*";

    public static final String TEST_REMOTING_PORT = "TEST_REMOTING_PORT";
    public static final String TEST_HTTP_PORT = "TEST_HTTP_PORT";
    public static final String TEST_DIRECTORY = "TEST_DIRECTORY";
    public static final String TEST_RESOURCE_PATH = "TEST_RESOURCE_PATH";
    public static final String TEST_HTTP_PREFIX = "TEST_HTTP_PREFIX";
    private static int kernelN = 0;
    private final URL conf;
    private final List<URL> additionalModules = new ArrayList<>();
    public Kernel kernel;
    private Path confd;

    public KernelFixture( URL conf ) {
        this( conf, null, List.of() );
    }

    public KernelFixture( URL conf, Path confd ) {
        this( conf, confd, List.of() );
    }

    public KernelFixture( URL conf, List<URL> additionalModules ) {
        this( conf, null, additionalModules );
    }

    public KernelFixture( URL conf, Path confd, List<URL> additionalModules ) {
        this( Scope.METHOD, conf, confd, additionalModules );
    }

    private KernelFixture( Scope scope, URL conf, Path confd, List<URL> additionalModules ) {
        this.scope = scope;
        this.conf = conf;
        this.confd = confd;
        this.additionalModules.addAll( additionalModules );

        defineDefaults();
    }

    @Override
    public KernelFixture withKind( Kind kind ) {
        return ( KernelFixture ) super.withKind( kind );
    }

    public int defaultHttpPort() {
        return portFor( TEST_HTTP_PORT );
    }

    private void defineDefaults() {
        define( TEST_REMOTING_PORT, portFor( TEST_REMOTING_PORT ) );
//        deprecated
        define( "TMP_REMOTE_PORT", portFor( TEST_REMOTING_PORT ) );
        var testHttpPort = portFor( TEST_HTTP_PORT );
        define( TEST_HTTP_PORT, testHttpPort );
//        deprecated
        define( "HTTP_PORT", testHttpPort );
        define( TEST_DIRECTORY, testDirectory() );
//        deprecated
        define( "TMP_PATH", testDirectory() );
        String resourcePath = Resources.path( getClass(), "/" ).orElseThrow();
        define( TEST_RESOURCE_PATH, resourcePath );
//        deprecated
        define( "RESOURCE_PATH", resourcePath );
        define( TEST_HTTP_PREFIX, httpPrefix( testHttpPort ) );
//        deprecated
        define( "HTTP_PREFIX", httpPrefix( testHttpPort ) );
    }

    public KernelFixture withConfdResources( Class<?> clazz, String confdResource ) {
        this.confd = TestDirectoryFixture.testPath( "/test-application-conf.d" );

        Resources.filePaths( clazz, confdResource )
            .forEach( path -> oap.io.Files.copyDirectory( path, this.confd ) );

        return this;
    }

    @Override
    public KernelFixture define( String property, Object value ) {
        return ( KernelFixture ) super.define( property, value );
    }

    @Override
    public KernelFixture definePort( String property, String portKey ) {
        return ( KernelFixture ) super.definePort( property, portKey );
    }

    @Nonnull
    public <T> T service( @Nonnull String moduleName, @Nonnull Class<T> klass ) {
        return kernel.serviceOfClass( moduleName, klass )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + moduleName + ":" + klass ) );
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

    @Override
    protected void before() {
        defineDefaults();

        Preconditions.checkArgument( this.kernel == null );
        super.before();

        for( var f : fixtures ) {
            if( f instanceof EnvFixture ) merge( ( EnvFixture ) f );
        }

        var moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
        moduleConfigurations.addAll( additionalModules );
        this.kernel = new Kernel( "FixtureKernel#" + kernelN++, moduleConfigurations );

        var confds = ApplicationConfiguration.getConfdUrls( confd );
        this.kernel.start( ApplicationConfiguration.load( conf, confds, getProperties() ) );
    }

    @Override
    protected void after() {
        this.kernel.stop();
        this.kernel = null;

        super.after();
    }

    @Override
    public KernelFixture withScope( Scope scope ) {
        super.withScope( scope );

        return this;
    }
}
