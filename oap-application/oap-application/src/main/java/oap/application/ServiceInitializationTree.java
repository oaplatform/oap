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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import static oap.application.ServiceStorage.ErrorStatus.MODULE_NOT_FOUND;
import static oap.application.ServiceStorage.ErrorStatus.SERVICE_NOT_FOUND;


public class ServiceInitializationTree extends AbstractMap<ModuleItem.ServiceItem, ServiceInitialization> implements ServiceStorage {
    // JPathWS
    public final LinkedHashMap<String, LinkedHashMap<String, ServiceInitialization>> moduleMap = new LinkedHashMap<>();
    private final LinkedHashMap<ModuleItem.ServiceItem, ServiceInitialization> map = new LinkedHashMap<>();

    @Override
    public ServiceInitialization put( ModuleItem.ServiceItem key, ServiceInitialization value ) {
        var services = moduleMap.computeIfAbsent( key.getModuleName(), n -> new LinkedHashMap<>() );
        services.put( key.serviceName, value );
        services.putIfAbsent( key.getName(), value );
        return map.put( key, value );
    }

    @Override
    public void forEach( BiConsumer<? super ModuleItem.ServiceItem, ? super ServiceInitialization> action ) {
        map.forEach( action );
    }

    @Override
    public Collection<ServiceInitialization> values() {
        return map.values();
    }

    @Override
    public Set<Entry<ModuleItem.ServiceItem, ServiceInitialization>> entrySet() {
        return map.entrySet();
    }

    public LinkedHashMap<String, ServiceInitialization> getServices( String moduleName ) {
        return moduleMap.get( moduleName );
    }

    public ServiceInitialization putIfAbsent( ModuleItem.ServiceItem key, String serviceName, ServiceInitialization value ) {
        var ret = moduleMap.computeIfAbsent( key.getModuleName(), n -> new LinkedHashMap<>() ).putIfAbsent( serviceName, value );
        map.putIfAbsent( key, value );

        return ret;
    }

    public void clear() {
        map.clear();
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

    public ServiceInitialization get( String moduleName, String serviceName ) {
        var services = getServices( moduleName );
        if( services == null ) return null;

        return services.get( serviceName );
    }

    public Optional<ServiceInitialization> findFirstServiceByName( String serviceName ) {
        for( var s : moduleMap.values() ) {
            var r = s.get( serviceName );
            if( r != null ) return Optional.of( r );
        }

        return Optional.empty();
    }

    public List<ServiceInitialization> findAllServicesByName( String serviceName ) {
        var ret = new ArrayList<ServiceInitialization>();
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

        map.entrySet().removeIf( e -> e.getKey().getModuleName().equals( moduleName ) && e.getKey().serviceName.equals( serviceName ) );
    }
}
