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

import oap.testng.AbstractEnvFixture;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class KernelFixture extends AbstractKernelFixture<KernelFixture> {

    public KernelFixture( URL conf, AbstractEnvFixture<?>... dependencies ) {
        super( NO_PREFIX, Scope.METHOD, conf, null, List.of(), dependencies );
    }

    public KernelFixture( URL conf, Path confd, AbstractEnvFixture<?>... dependencies ) {
        super( NO_PREFIX, Scope.METHOD, conf, confd, List.of(), dependencies );
    }

    public KernelFixture( URL conf, List<URL> additionalModules, AbstractEnvFixture<?>... dependencies ) {
        super( NO_PREFIX, Scope.METHOD, conf, null, additionalModules, dependencies );
    }

    public KernelFixture( URL conf, Path confd, List<URL> additionalModules, AbstractEnvFixture<?>... dependencies ) {
        this( Scope.METHOD, conf, confd, additionalModules, dependencies );
    }

    public KernelFixture( Scope scope, URL conf, Path confd, List<URL> additionalModules, AbstractEnvFixture<?>... dependencies ) {
        super( NO_PREFIX, scope, conf, confd, additionalModules, dependencies );
    }
}
