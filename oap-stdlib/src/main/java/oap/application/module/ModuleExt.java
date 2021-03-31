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

package oap.application.module;

import com.google.common.base.Preconditions;
import oap.application.ServiceInitialization;

import java.util.Map;

public class ModuleExt<T> {
    public final Module module;
    public final T ext;
    private final Map<String, ServiceInitialization> moduleServices;

    public ModuleExt( Module module, T ext, Map<String, ServiceInitialization> moduleServices ) {
        this.module = module;
        this.ext = ext;
        this.moduleServices = moduleServices;
    }

    public boolean containsService( String serviceName ) {
        return module.services.containsKey( serviceName );
    }

    public boolean isServiceInitialized( String serviceName ) {
        return moduleServices.containsKey( serviceName );
    }

    public Object getInstance( String serviceName ) {
        var si = moduleServices.get( serviceName );
        Preconditions.checkNotNull( si );
        return si.instance;
    }
}
