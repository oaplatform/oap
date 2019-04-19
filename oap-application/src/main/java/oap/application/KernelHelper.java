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

import lombok.extern.slf4j.Slf4j;
import oap.util.Lists;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.commons.collections4.CollectionUtils.subtract;

@Slf4j( topic = "oap.application.Kernel" )
public class KernelHelper {

    static Set<Module> forEachModule( Set<Module> modules, Set<String> initializedModules, Consumer<Module> cons ) {
        var deferred = new LinkedHashSet<Module>();

        for( Module module : modules ) {
            log.debug( "module {}", module.name );

            if( initializedModules.containsAll( module.dependsOn ) ) {
                cons.accept( module );

                initializedModules.add( module.name );
            } else {
                log.debug( "dependencies are not ready - deferring {}: {}",
                    module.name, subtract( module.dependsOn, initializedModules ) );
                deferred.add( module );
            }
        }

        return deferred.size() == modules.size()
            ? deferred
            : forEachModule( deferred, initializedModules, cons );
    }

    static Map<String, Module.Service> forEachService( Set<Module> modules,
                                                       Map<String, Module.Service> services,
                                                       Set<String> initializedServices,
                                                       BiConsumer<String, Module.Service> cons ) {
        var deferred = new LinkedHashMap<String, Module.Service>();

        for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
            var service = entry.getValue();

            List<String> dependsOn = Lists.filter( service.dependsOn, d -> serviceEnabled( modules, d ) );
            if( initializedServices.containsAll( dependsOn ) ) {
                cons.accept( entry.getKey(), service );
            } else {
                log.debug( "dependencies are not ready - deferring {}: {}, initialized {}",
                    service.name, subtract( service.dependsOn, initializedServices ), initializedServices );
                deferred.put( entry.getKey(), service );
            }
        }

        return deferred.size() == services.size() ? deferred
            : forEachService( modules, deferred, initializedServices, cons );
    }

    static boolean serviceEnabled( Set<Module> modules, String name ) {
        for( Module module : modules ) {
            Module.Service service = module.services.get( name );
            if( service != null && !service.enabled ) return false;
        }

        return true;
    }

    static LinkedHashMap<String, Object> fixLinksForConstructor( Kernel kernel, Map<String, ServiceInitialization> initialized,
                                                                 LinkedHashMap<String, Object> parameters ) {
        fixLinks( kernel, initialized, parameters );

        var ret = new LinkedHashMap<String, Object>();

        parameters.forEach( ( name, value ) -> {
            Object newValue = fixValue( kernel, initialized, value );
            ret.put( name, newValue );
        } );

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    static Object fixValue( Kernel kernel, Map<String, ServiceInitialization> initialized, Object value ) {
        Object newValue;
        if( isServiceLink( value ) ) {
            var linkName = referenceName( value );
            var si = initialized.get( linkName );
            newValue = si != null ? si.instance : null;
        } else if( isKernel( value ) ) {
            newValue = kernel;
        } else if( value instanceof String && ( ( String ) value ).matches( "@[^:]+:.+" ) ) {
            newValue = null;
        } else if( value instanceof List<?> ) {
            var newList = new ArrayList<>();
            for( var lValue : ( List<?> ) value ) {
                var fixLValue = fixValue( kernel, initialized, lValue );
                if( fixLValue != null ) newList.add( fixLValue );
            }
            newValue = newList;
        } else if( value instanceof Map<?, ?> ) {
            var newMap = new LinkedHashMap<>();

            ( ( Map<String, Object> ) value ).forEach( ( key, mValue ) -> {
                var v = fixValue( kernel, initialized, mValue );
                if( v != null ) newMap.put( key, v );
            } );

            newValue = newMap;
        } else {
            newValue = value;
        }
        return newValue;
    }

    static boolean isServiceLink( Object value ) {
        return value instanceof String && ( ( String ) value ).startsWith( "@service:" );
    }

    static boolean isKernel( Object value ) {
        return "@kernel".equals( value );
    }

    static boolean isImplementations( Object value ) {
        return value instanceof String && ( ( String ) value ).startsWith( "@implementations:" );
    }

    static String referenceName( Object ref ) {
        return Module.Reference.of( ref ).name;
    }

    static Object fixLinks( Kernel kernel, Map<String, ServiceInitialization> initialized, Object value ) {
        if( isServiceLink( value ) ) {
            var linkName = referenceName( value );
            var si = initialized.get( linkName );
            return si != null ? si.instance : null;
        } else if( isKernel( value ) ) {
            return kernel;
        } else if( value instanceof List<?> ) {
            ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
            while( it.hasNext() ) {
                var v = fixLinks( kernel, initialized, it.next() );
                if( v != null ) it.set( v );
            }
        } else if( value instanceof Map<?, ?> ) {
            for( var entry : ( ( Map<?, Object> ) value ).entrySet() ) {
                var v = fixLinks( kernel, initialized, entry.getValue() );
                if( v != null ) entry.setValue( v );
            }
        }

        return value;
    }
}
