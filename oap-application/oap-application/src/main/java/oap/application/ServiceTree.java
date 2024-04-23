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

import oap.util.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static oap.application.ServiceStorage.ErrorStatus.MODULE_NOT_FOUND;
import static oap.application.ServiceStorage.ErrorStatus.SERVICE_NOT_FOUND;


public class ServiceTree implements ServiceStorage {
    // JPathWS
    public final LinkedHashMap<String, LinkedHashMap<String, ModuleItem.ServiceItem>> moduleMap = new LinkedHashMap<>();

    public void add( ModuleItem.ServiceItem serviceItem ) {
        var services = moduleMap.computeIfAbsent( serviceItem.getModuleName(), _ -> new LinkedHashMap<>() );
        services.put( serviceItem.serviceName, serviceItem );
        services.putIfAbsent( serviceItem.serviceName, serviceItem );
    }

    public void forEach( Consumer<? super ModuleItem.ServiceItem> action ) {
        values().forEach( action );
    }

    public Collection<ModuleItem.ServiceItem> values() {
        LinkedHashSet<ModuleItem.ServiceItem> ret = new LinkedHashSet<>();

        moduleMap.forEach( ( _, services ) -> ret.addAll( services.values() ) );

        return ret;
    }

    public LinkedHashMap<String, ModuleItem.ServiceItem> getServices( String moduleName ) {
        return moduleMap.get( moduleName );
    }

    public ModuleItem.ServiceItem putIfAbsent( ModuleItem.ServiceItem key, String serviceName, ModuleItem.ServiceItem value ) {
        return moduleMap.computeIfAbsent( key.getModuleName(), n -> new LinkedHashMap<>() ).putIfAbsent( serviceName, value );
    }

    public void clear() {
        moduleMap.clear();
    }

    @Override
    public Result<Object, ErrorStatus> findByName( String moduleName, String serviceName ) {
        var moduleServices = moduleMap.get( moduleName );
        if( moduleServices == null ) return Result.failure( MODULE_NOT_FOUND );

        var service = moduleServices.get( serviceName );
        if( service == null ) return Result.failure( SERVICE_NOT_FOUND );

        return Result.success( service );
    }

    public ModuleItem.ServiceItem get( String moduleName, String serviceName ) {
        var services = getServices( moduleName );
        if( services == null ) return null;

        return services.get( serviceName );
    }

    public Optional<ModuleItem.ServiceItem> findFirstServiceByName( String serviceName ) {
        for( var s : moduleMap.values() ) {
            var r = s.get( serviceName );
            if( r != null ) return Optional.of( r );
        }

        return Optional.empty();
    }

    public List<ModuleItem.ServiceItem> findAllServicesByName( String serviceName ) {
        var ret = new ArrayList<ModuleItem.ServiceItem>();
        for( var s : moduleMap.values() ) {
            var r = s.get( serviceName );
            if( r != null ) ret.add( r );
        }

        return ret;
    }

    public void removeService( String moduleName, String serviceName ) {
        var mod = moduleMap.get( moduleName );

        if( mod == null ) return;

        mod.remove( serviceName );
    }

    public int size() {
        int size = 0;

        for( LinkedHashMap<String, ModuleItem.ServiceItem> services : moduleMap.values() ) {
            size += services.size();
        }

        return size;
    }
}
