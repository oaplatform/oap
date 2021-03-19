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
import oap.util.Result;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Created by igor.petrenko on 2021-03-16.
 */
@ToString( callSuper = true )
@Slf4j
public class ServiceKernelCommand extends AbstractKernelCommand<Object> {
    public static final Set<String> THIS = Set.of( "this", "self" );

    public static final ServiceKernelCommand INSTANCE = new ServiceKernelCommand();

    private ServiceKernelCommand() {
        super( "^modules\\.([^.]*)\\.(.+)$" );
    }

    @Override
    public Result<Object, ErrorStatus> get( Object value, Kernel kernel, @Nullable ModuleItem moduleItem, ServiceStorage storage ) {
        var reference = reference( ( String ) value, moduleItem );

        return storage.findByName( reference.module, reference.service );
    }

    public Module.Reference reference( String value, @Nullable ModuleItem moduleItem ) {
        var matcher = pattern.matcher( value );
        Preconditions.checkArgument( matcher.find(), "invalid reference " + value + ", pattern = " + pattern );

        var moduleName = matcher.group( 1 );
        if( moduleItem != null && THIS.contains( moduleName ) ) moduleName = moduleItem.getName();
        if( "".equals( moduleName ) ) moduleName = "*";
        var linkName = matcher.group( 2 );

        return new Module.Reference( moduleName, linkName );
    }
}
