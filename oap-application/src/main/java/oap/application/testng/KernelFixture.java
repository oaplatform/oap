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
import oap.io.Resources;
import oap.testng.Env;
import oap.testng.Fixture;

import java.nio.file.Path;

import static oap.http.testng.HttpAsserts.httpPrefix;

public class KernelFixture implements Fixture {
    private static int kernelN = 0;
    public Kernel kernel;
    private Path conf;
    private String confCatalog;

    public KernelFixture( Path conf ) {
        this.conf = conf;
    }

    public KernelFixture( Path conf, String confCatalog ) {
        this.confCatalog = confCatalog;
        this.conf = conf;
    }

    @Override
    public void beforeMethod() {

        System.setProperty( "TMP_REMOTE_PORT", String.valueOf( Env.port( "TMP_REMOTE_PORT" ) ) );
        System.setProperty( "HTTP_PORT", String.valueOf( Env.port() ) );
        System.setProperty( "TMP_PATH", Env.tmp( "/" ) );
        System.setProperty( "HTTP_PREFIX", httpPrefix() );
        this.kernel = new Kernel( "FixtureKernel#" + kernelN++, Module.CONFIGURATION.urlsFromClassPath() );

        if( confCatalog != null ) {
            var toConfD = Env.tmpPath( confCatalog );
            Resources.filePaths( getClass(), confCatalog )
                .forEach( ( path ) -> oap.io.Files.copyDirectory( path, toConfD ) );
            this.kernel.start( conf, toConfD );
        } else {
            this.kernel.start( conf );
        }
    }

    @Override
    public void afterMethod() {
        this.kernel.stop();
    }
}
