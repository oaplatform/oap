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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

@Slf4j( topic = "oap.application.Kernel" )
public class KernelHelper {
    public static final Set<String> THIS = Set.of( "this", "self" );

    static LinkedHashMap<String, Object> fixLinksForConstructor( Kernel kernel, ModuleItem thisModuleName,
                                                                 ServiceStorage storage,
                                                                 LinkedHashMap<String, Object> parameters ) {
        fixLinks( kernel, thisModuleName, storage, parameters );

        var ret = new LinkedHashMap<String, Object>();

        parameters.forEach( ( name, value ) -> {
            Object newValue = fixValue( kernel, thisModuleName, storage, value );
            ret.put( name, newValue );
        } );

        return ret;
    }

    @SuppressWarnings( "unchecked" )
    static Object fixValue( Kernel kernel, ModuleItem thisModuleItem, ServiceStorage storage, Object value ) {
        Object newValue;
        if( value instanceof List<?> ) {
            var newList = new ArrayList<>();
            for( var lValue : ( List<?> ) value ) {
                var fixLValue = fixValue( kernel, thisModuleItem, storage, lValue );
                if( fixLValue != null ) newList.add( fixLValue );
            }
            newValue = newList;
        } else if( value instanceof Map<?, ?> ) {
            var newMap = new LinkedHashMap<>();

            ( ( Map<String, Object> ) value ).forEach( ( key, mValue ) -> {
                var v = fixValue( kernel, thisModuleItem, storage, mValue );
                if( v != null ) newMap.put( key, v );
            } );

            newValue = newMap;
        } else {
            var command = Lists.find2( Kernel.commands, c -> c.matches( value ) );
            if( command != null ) {
                var result = command.get( value, kernel, thisModuleItem, storage );
                if( result.isSuccess() ) {
                    newValue = result.successValue;
                } else {
                    log.trace( "{} not found", value );
                    newValue = null;
                }
            } else {
                newValue = value;
            }
        }
        return newValue;
    }

    @SuppressWarnings( { "unchecked", "checkstyle:ParameterAssignment" } )
    static Object fixLinks( Kernel kernel, ModuleItem thisModuleItem, ServiceStorage storage, Object value ) {
        if( value instanceof List<?> ) {
            ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
            while( it.hasNext() ) {
                var oldValue = it.next();
                var v = fixLinks( kernel, thisModuleItem, storage, oldValue );
                if( v != oldValue ) {
                    if( v instanceof List<?> ) {
                        replaceAndAddAll( it, ( List<?> ) v );
                    } else {
                        it.set( v );
                    }
                }
            }
        } else if( value instanceof Map<?, ?> ) {
            for( var entry : ( ( Map<?, Object> ) value ).entrySet() ) {
                var v = fixLinks( kernel, thisModuleItem, storage, entry.getValue() );
                if( v != null ) {
                    if( entry.getValue() instanceof Collection<?> ) {
                        entry.setValue( v );
                    } else if( v instanceof Collection<?> && ( ( Collection<?> ) v ).size() == 1 ) {
                        entry.setValue( ( ( Collection<?> ) v ).iterator().next() );
                    } else {
                        entry.setValue( v );
                    }
                }
            }
        } else if( value instanceof String ) {
            var finalValue = value;
            var command = Lists.find2( Kernel.commands, c -> c.matches( finalValue ) );
            if( command != null ) {
                var result = command.get( value, kernel, thisModuleItem, storage );
                if( !result.isSuccess() ) {
                    log.trace( "{} not found", value );
                    value = null;
                } else {
                    value = result.successValue;
                }
            }
        }

        return value;
    }

    public static boolean profileEnabled( Set<String> profiles, Set<String> systemProfiles ) {
        for( var profile : profiles ) {
            if( profile.startsWith( "-" ) ) {
                if( systemProfiles.contains( profile.substring( 1 ) ) ) return false;
            } else {
                if( !systemProfiles.contains( profile ) ) return false;
            }

        }

        return true;
    }

    public static boolean isModuleEnabled( Module module, Set<String> systemProfiles ) {
        return profileEnabled( module.profiles, systemProfiles );
    }

    public static boolean isServiceEnabled( Module.Service service, Set<String> systemProfiles ) {
        return profileEnabled( service.profiles, systemProfiles );
    }

    public static void setThreadNameSuffix( String suffix ) {
        var threadSuffix = StringUtils.replace( suffix, "###", "---" );

        var thread = Thread.currentThread();
        var name = thread.getName();

        var index = name.lastIndexOf( "###" );
        if( index > 0 ) {
            name = name.substring( 0, index );
        }

        thread.setName( name + "###" + threadSuffix );
    }

    public static void restoreThreadName() {
        var thread = Thread.currentThread();
        var name = thread.getName();

        var index = name.lastIndexOf( "###" );
        if( index > 0 ) {
            thread.setName( name.substring( 0, index ) );
        }

    }

    private static void replaceAndAddAll( ListIterator<Object> it, List<?> data ) {
        if( data.isEmpty() ) it.remove();
        else {
            var head = Lists.head2( data );
            it.set( head );
            for( var i = 1; i < data.size(); i++ ) {
                it.add( data.get( i ) );
            }
        }
    }
}
