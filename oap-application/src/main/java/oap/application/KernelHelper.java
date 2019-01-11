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
import lombok.val;
import oap.util.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        val deferred = new HashSet<Module>();

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
        HashMap<String, Module.Service> deferred = new HashMap<>();

        for( Map.Entry<String, Module.Service> entry : services.entrySet() ) {
            val service = entry.getValue();

            List<String> dependsOn = Lists.filter( service.dependsOn, d -> serviceEnabled( modules, d ) );
            if( initializedServices.containsAll( dependsOn ) ) {
                log.debug( "initializing {} as {}", entry.getKey(), service.name );

                cons.accept( entry.getKey(), service );

                initializedServices.add( service.name );
            } else {
                log.debug( "dependencies are not ready - deferring {}: {}",
                    service.name, subtract( service.dependsOn, initializedServices ) );
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

    static LinkedHashMap<String, Object> fixLinksForConstructor( Map<String, ServiceInitialization> initialized,
                                                                 LinkedHashMap<String, Object> parameters ) {
        fixLinks( initialized, parameters );

        val ret = new LinkedHashMap<String, Object>();

        parameters.forEach( ( name, value ) -> {
            Object newValue = fixValue( initialized, value );
            ret.put( name, newValue );
        } );

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    static Object fixValue( Map<String, ServiceInitialization> initialized, Object value ) {
        Object newValue;
        if( isServiceLink( value ) ) {
            val linkName = referenceName( value );
            val si = initialized.get( linkName );
            newValue = si != null ? si.instance : null;
        } else if( value instanceof String && ( ( String ) value ).matches( "@[^:]+:.+" ) ) {
            newValue = null;
        } else if( value instanceof List<?> ) {
            val newList = new ArrayList<>();
            for( val lValue : ( List<?> ) value ) {
                val fixLValue = fixValue( initialized, lValue );
                if( fixLValue != null ) newList.add( fixLValue );
            }
            newValue = newList;
        } else if( value instanceof Map<?, ?> ) {
            val newMap = new LinkedHashMap<Object, Object>();

            ( ( Map<String, Object> ) value ).forEach( ( key, mValue ) -> {
                val v = fixValue( initialized, mValue );
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

    static boolean isImplementations( Object value ) {
        return value instanceof String && ( ( String ) value ).startsWith( "@implementations:" );
    }

    static String referenceName( Object ref ) {
        return Module.Reference.of( ref ).name;
    }

    static Object fixLinks( Map<String, ServiceInitialization> initialized, Object value ) {
        if( isServiceLink( value ) ) {
            val linkName = referenceName( value );
            val si = initialized.get( linkName );
            return si != null ? si.instance : null;
        } else if( value instanceof List<?> ) {
            ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
            while( it.hasNext() ) {
                val v = fixLinks( initialized, it.next() );
                if( v != null ) it.set( v );
            }
        } else if( value instanceof Map<?, ?> ) {
            for( val entry : ( ( Map<?, Object> ) value ).entrySet() ) {
                val v = fixLinks( initialized, entry.getValue() );
                if( v != null ) entry.setValue( v );
            }
        }

        return value;
    }
}
