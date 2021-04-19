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

import oap.application.module.Module;
import oap.util.Result;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BiConsumer;

import static oap.application.ServiceStorage.ErrorStatus.MODULE_NOT_FOUND;
import static oap.application.ServiceStorage.ErrorStatus.SERVICE_NOT_FOUND;


public class ServiceInitializationTree extends AbstractMap<String, ServiceInitializationTree.ModuleTree> implements ServiceStorage {
    private final LinkedHashMap<String, ModuleTree> map = new LinkedHashMap<>();

    public void put( Module module, String serviceName, ServiceInitialization serviceInitialization ) {
        map.computeIfAbsent( module.name, mn -> new ModuleTree( module ) ).put( serviceName, serviceInitialization );
    }

    @Override
    public void forEach( BiConsumer<? super String, ? super ModuleTree> action ) {
        map.forEach( action );
    }

    @Override
    public Collection<ModuleTree> values() {
        return super.values();
    }

    @Override
    public Set<Entry<String, ModuleTree>> entrySet() {
        return map.entrySet();
    }

    public ModuleTree get( String module ) {
        return map.get( module );
    }

    public ServiceInitialization putIfAbsent( Module module, String serviceName, ServiceInitialization si ) {
        return map.computeIfAbsent( module.name, mn -> new ModuleTree( module ) ).putIfAbsent( serviceName, si );
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

        return Result.success( service );
    }

    public static class ModuleTree extends AbstractMap<String, ServiceInitialization> {
        public final LinkedHashMap<String, ServiceInitialization> map = new LinkedHashMap<>();
        public final Module module;

        public ModuleTree( Module module ) {
            this.module = module;
        }

        @Override
        public ServiceInitialization put( String serviceName, ServiceInitialization serviceInitialization ) {
            return map.put( serviceName, serviceInitialization );
        }

        public ServiceInitialization get( String serviceName ) {
            return map.get( serviceName );
        }

        @Override
        public ServiceInitialization putIfAbsent( String serviceName, ServiceInitialization si ) {
            return map.putIfAbsent( serviceName, si );
        }

        @Override
        public void forEach( BiConsumer<? super String, ? super ServiceInitialization> action ) {
            map.forEach( action );
        }

        @Override
        public Collection<ServiceInitialization> values() {
            return map.values();
        }

        @Override
        public Set<Entry<String, ServiceInitialization>> entrySet() {
            return map.entrySet();
        }

        @Override
        public ServiceInitialization remove( Object key ) {
            return map.remove( key );
        }

        @SuppressWarnings( "unchecked" )
        public <T> T getExt( String ext ) {
            return ( T ) this.module.ext.get( ext );
        }
    }
}
