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

import com.typesafe.config.impl.ConfigImpl;
import oap.application.Kernel;
import oap.application.Module;
import oap.io.Files;
import oap.io.Resources;
import oap.testng.Env;
import oap.testng.EnvFixture;
import oap.testng.TestDirectoryFixture;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.http.testng.HttpAsserts.httpPrefix;

public class KernelFixture extends EnvFixture {
    private static int kernelN = 0;
    public Kernel kernel;
    private final Path conf;
    private final String confd;
    private final List<URL> additionalModules = new ArrayList<>();

    public KernelFixture( Path conf ) {
        this( conf, null, List.of() );
    }

    public KernelFixture( String conf ) {
        this( conf, null, List.of() );
    }

    public KernelFixture( String conf, List<URL> additionalModules ) {
        this( conf, null, additionalModules );
    }

    public KernelFixture( Path conf, String confd ) {
        this( conf, confd, List.of() );
    }

    public KernelFixture( String conf, String confd ) {
        this( conf, confd, List.of() );
    }

    public KernelFixture( Path conf, List<URL> additionalModules ) {
        this( conf, null, additionalModules );
    }

    public KernelFixture( String conf, String confd, List<URL> additionalModules ) {
        this( Resources.filePath( KernelFixture.class, conf )
            .orElseThrow( () -> new IllegalArgumentException( conf ) ), confd, additionalModules );
    }

    public KernelFixture( Path conf, String confd, List<URL> additionalModules ) {
        this.conf = conf;
        this.confd = confd;
        this.additionalModules.addAll( additionalModules );
        define( "TEST_REMOTING_PORT", Env.port( "TEST_REMOTING_PORT" ) );
//        deprecated
        define( "TMP_REMOTE_PORT", "${TEST_REMOTING_PORT}" );
        define( "TEST_HTTP_PORT", Env.port() );
//        deprecated
        define( "HTTP_PORT", "${TEST_HTTP_PORT}" );
        define( "TEST_DIRECTORY", TestDirectoryFixture.testDirectory() );
//        deprecated
        define( "TMP_PATH", "${TEST_DIRECTORY}" );
        define( "TEST_RESOURCE_PATH", Resources.path( getClass(), "/" ).orElseThrow() );
//        deprecated
        define( "RESOURCE_PATH", "${TEST_RESOURCE_PATH}" );
        define( "TEST_HTTP_PREFIX", httpPrefix() );
//        deprecated
        define( "HTTP_PREFIX", httpPrefix() );

    }

    @Override
    public KernelFixture define( String property, Object value ) {
        return ( KernelFixture ) super.define( property, value );
    }

    @Override
    public KernelFixture define( Scope scope, String property, Object value ) {
        return ( KernelFixture ) super.define( scope, property, value );
    }

    @Nonnull
    public <T> T service( @Nonnull Class<T> klass ) {
        return kernel.serviceOfClass( klass ).orElseThrow( () -> new IllegalArgumentException( "unknown service " + klass ) );
    }

    @Nonnull
    public <T> T service( @Nonnull String name ) {
        return kernel.<T>service( name ).orElseThrow( () -> new IllegalArgumentException( "unknown service " + name ) );
    }

    @Override
    public void beforeMethod() {
        ConfigImpl.reloadSystemPropertiesConfig();
        ConfigImpl.reloadEnvVariablesConfig();

        super.beforeMethod();
        List<URL> moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
        moduleConfigurations.addAll( additionalModules );
        this.kernel = new Kernel( "FixtureKernel#" + kernelN++, moduleConfigurations );

        if( confd != null ) {
            var confdDeployed = TestDirectoryFixture.testPath( confd );
            Resources.filePaths( getClass(), confd )
                .forEach( path -> Files.copyDirectory( path, confdDeployed ) );
            this.kernel.start( conf, confdDeployed );
        } else this.kernel.start( conf );
    }

    @Override
    public void afterMethod() {
        this.kernel.stop();

        ConfigImpl.reloadSystemPropertiesConfig();
        ConfigImpl.reloadEnvVariablesConfig();
    }
}
