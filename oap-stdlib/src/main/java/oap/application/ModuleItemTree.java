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

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static oap.application.ServiceStorage.ErrorStatus.MODULE_NOT_FOUND;
import static oap.application.ServiceStorage.ErrorStatus.SERVICE_NOT_FOUND;

/**
 * Created by igor.petrenko on 2021-03-18.
 */
class ModuleItemTree extends AbstractMap<String, ModuleItem> implements ServiceStorage {
    private final LinkedHashMap<String, ModuleItem> map = new LinkedHashMap<>();

    ModuleItemTree() {
    }

    ModuleItemTree( Map<String, ModuleItem> map ) {
        this.map.putAll( map );
    }

    public ModuleItem remove( String moduleName ) {
        return map.remove( moduleName );
    }

    @Override
    public ModuleItem put( String moduleName, ModuleItem moduleItem ) {
        return map.put( moduleName, moduleItem );
    }

    @Override
    public Collection<ModuleItem> values() {
        return map.values();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Set<Map.Entry<String, ModuleItem>> entrySet() {
        return map.entrySet();
    }

    public void set( LinkedHashMap<String, ModuleItem> newMap ) {
        map.clear();
        map.putAll( newMap );
    }

    public @Nonnull
    ModuleItem findModule( ModuleItem fromModule, String name ) throws ApplicationException {
        var moduleItem = map.get( name );

        if( moduleItem == null )
            throw new ApplicationException( "[" + fromModule.module.name + "]: dependsOn not found: " + name );

        return moduleItem;
    }

    @Override
    public ModuleItemTree clone() {
        return new ModuleItemTree( map );
    }

    public ModuleItem get( String moduleName ) {
        return map.get( moduleName );
    }

    @Override
    public Result<Object, ErrorStatus> findByName( String moduleName, String serviceName ) {
        var moduleInfo = map.get( moduleName );
        if( moduleInfo == null ) return Result.failure( MODULE_NOT_FOUND );

        var service = moduleInfo.services.get( serviceName );
        if( service == null ) return Result.failure( SERVICE_NOT_FOUND );

        return Result.success( service );
    }
}
