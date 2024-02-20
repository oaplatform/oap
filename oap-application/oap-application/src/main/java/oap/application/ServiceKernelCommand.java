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

package oap.application;

import com.google.common.base.Preconditions;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.application.ServiceStorage.ErrorStatus;
import oap.application.module.Reference;
import oap.application.module.Service;
import oap.util.Result;

import javax.annotation.Nullable;
import java.util.Set;


@ToString( callSuper = true )
@Slf4j
public class ServiceKernelCommand extends AbstractKernelCommand<ServiceInitialization> {
    public static final Set<String> THIS = Set.of( "this", "self" );

    public static final ServiceKernelCommand INSTANCE = new ServiceKernelCommand();

    private ServiceKernelCommand() {
        super( "^<?modules\\.([^.]*)\\.(.+)>?$" );
    }

    @Override
    public Result<ServiceInitialization, ErrorStatus> get( Object value, Kernel kernel, @Nullable ModuleItem moduleItem,
                                                           Service service, ServiceStorage storage ) {
        var reference = reference( ( String ) value, moduleItem );

        return storage.findByName( reference.module, reference.service ).mapSuccess( v -> ( ServiceInitialization ) v );
    }

    public Reference reference( String value, @Nullable ModuleItem moduleItem ) {
        var matcher = pattern.matcher( value );
        Preconditions.checkArgument( matcher.find(), "invalid reference " + value + ", pattern = " + pattern );

        var moduleName = matcher.group( 1 );
        if( moduleItem != null && THIS.contains( moduleName ) ) moduleName = moduleItem.getName();
        if( "".equals( moduleName ) ) moduleName = "*";
        var linkName = matcher.group( 2 );

        if( linkName.endsWith( ">" ) ) {
            linkName = linkName.substring( 0, linkName.length() - 1 );
        }

        return new Reference( moduleName, linkName );
    }

    @Override
    public Result<Object, ErrorStatus> getInstance( Object value, Kernel kernel, @Nullable ModuleItem moduleItem,
                                                    Service service, ServiceStorage storage ) {
        return get( value, kernel, moduleItem, service, storage ).mapSuccess( v -> v.instance );
    }
}
