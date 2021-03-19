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
import java.util.List;
import java.util.function.BiConsumer;

import static oap.application.ServiceStorage.ErrorStatus.MODULE_NOT_FOUND;
import static oap.application.ServiceStorage.ErrorStatus.SERVICE_NOT_FOUND;

/**
 * Created by igor.petrenko on 2021-03-18.
 */
class ServiceInitializationTree implements ServiceStorage {
    private final LinkedHashMap<String, LinkedHashMap<String, ServiceInitialization>> map = new LinkedHashMap<>();

    public void put( String moduleName, String serviceName, ServiceInitialization serviceInitialization ) {
        map.computeIfAbsent( moduleName, mn -> new LinkedHashMap<>() ).put( serviceName, serviceInitialization );
    }

    public void forEach( BiConsumer<String, LinkedHashMap<String, ServiceInitialization>> action ) {
        map.forEach( action );
    }

    public Collection<LinkedHashMap<String, ServiceInitialization>> values() {
        return map.values();
    }

    public LinkedHashMap<String, ServiceInitialization> get( String module ) {
        return map.get( module );
    }

    public ServiceInitialization putIfAbsent( String moduleName, String serviceName, ServiceInitialization si ) {
        return map.computeIfAbsent( moduleName, mn -> new LinkedHashMap<>() ).putIfAbsent( serviceName, si );
    }

    public void clear() {
        map.clear();
    }

    @Override
    public Result<Object, ErrorStatus> findByName( String moduleName, String serviceName ) {
        var moduleServices = map.get( moduleName );
        if( moduleServices == null ) return Result.failure( MODULE_NOT_FOUND );

        var service = moduleServices.get( serviceName );
        if( service == null ) return Result.failure( SERVICE_NOT_FOUND );

        return Result.success( service.instance );
    }

    @Override
    public Result<List<Object>, ErrorStatus> findByGroup( String moduleName, String groupName ) {
        if( ALL_MODULES.contains( moduleName ) ) {
            var ret = new ArrayList<>();
            for( var moduleServices : map.values() ) {
                for( var moduleService : moduleServices.values() ) {
                    if( moduleService.service.groups.contains( groupName ) ) ret.add( moduleService.instance );
                }
            }
            return Result.success( ret );
        } else {
            var moduleServices = map.get( moduleName );
            if( moduleServices == null ) return Result.failure( MODULE_NOT_FOUND );

            var ret = new ArrayList<>();

            for( var moduleService : moduleServices.values() ) {
                if( moduleService.service.groups.contains( groupName ) ) ret.add( moduleService.instance );
            }

            return Result.success( ret );
        }
    }
}
