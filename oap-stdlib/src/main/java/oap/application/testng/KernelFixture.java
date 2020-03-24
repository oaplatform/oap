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

import oap.application.Kernel;
import oap.application.Module;
import oap.io.Files;
import oap.io.Resources;
import oap.testng.Env;
import oap.testng.Fixture;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static oap.http.testng.HttpAsserts.httpPrefix;

public class KernelFixture implements Fixture {
    private static int kernelN = 0;
    public Kernel kernel;
    private Path conf;
    private String confd;
    private List<URL> additionalModules = List.of();

    public KernelFixture( Path conf ) {
        this.conf = conf;
    }

    public KernelFixture( String conf ) {
        this( conf, List.of() );
    }

    public KernelFixture( String conf, List<URL> additionalModules ) {
        this( Resources.filePath( KernelFixture.class, conf )
            .orElseThrow( () -> new IllegalArgumentException( conf ) ), null, additionalModules );
    }

    public KernelFixture( Path conf, String confd ) {
        this( conf, confd, List.of() );
    }

    public KernelFixture( Path conf, List<URL> additionalModules ) {
        this( conf, null, additionalModules );
    }

    public KernelFixture( Path conf, String confd, List<URL> additionalModules ) {
        this.conf = conf;
        this.confd = confd;
        this.additionalModules = additionalModules;
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
        System.setProperty( "TMP_REMOTE_PORT", String.valueOf( Env.port( "TMP_REMOTE_PORT" ) ) );
        System.setProperty( "HTTP_PORT", String.valueOf( Env.port() ) );
        System.setProperty( "TMP_PATH", Env.tmp( "/" ) );
        System.setProperty( "RESOURCE_PATH", Resources.path( getClass(), "/" ).orElseThrow() );
        System.setProperty( "HTTP_PREFIX", httpPrefix() );
        List<URL> moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
        moduleConfigurations.addAll( additionalModules );
        this.kernel = new Kernel( "FixtureKernel#" + kernelN++, moduleConfigurations );

        if( confd != null ) {
            var confdDeployed = Env.tmpPath( confd );
            Resources.filePaths( getClass(), confd )
                .forEach( path -> Files.copyDirectory( path, confdDeployed ) );
            this.kernel.start( conf, confdDeployed );
        } else this.kernel.start( conf );
    }

    @Override
    public void afterMethod() {
        this.kernel.stop();
    }
}
