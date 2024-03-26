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
import oap.application.module.Module;
import oap.application.module.Service;
import oap.util.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

@Slf4j( topic = "oap.application.Kernel" )
public class KernelHelper {
    public static final Set<String> THIS = Set.of( "this", "self" );

    public static ServiceConfigurationParameters fixLinksForConstructor( Kernel kernel, ModuleItem thisModuleName,
                                                                         ServiceStorage storage,
                                                                         Service service ) {

        ServiceConfigurationParameters p = new ServiceConfigurationParameters();

        service.parameters.forEach( ( k, v ) -> {
            var res = fixLinks( kernel, thisModuleName, service, storage, v );

            if( !res.ignoreCast ) {
                Object newValue = fixValue( kernel, thisModuleName, service, storage, res.value );

                p.configurationParameters.put( k, newValue );
            } else {
                p.serviceReferenceParameters.put( k, res.value );
            }
        } );

        return p;
    }

    @SuppressWarnings( "unchecked" )
    public static Object fixValue( Kernel kernel, ModuleItem thisModuleItem, Service service, ServiceStorage storage, Object value ) {
        Object newValue;
        if( value instanceof List<?> ) {
            var newList = new ArrayList<>();
            for( var lValue : ( List<?> ) value ) {
                var fixLValue = fixValue( kernel, thisModuleItem, service, storage, lValue );
                if( fixLValue != null ) newList.add( fixLValue );
            }
            newValue = newList;
        } else if( value instanceof Map<?, ?> ) {
            var newMap = new LinkedHashMap<>();

            ( ( Map<String, Object> ) value ).forEach( ( key, mValue ) -> {
                var v = fixValue( kernel, thisModuleItem, service, storage, mValue );
                if( v != null ) newMap.put( key, v );
            } );

            newValue = newMap;
        } else {
            var command = Lists.find2( Kernel.commands, c -> c.matches( value ) );
            if( command != null ) {
                var result = command.getInstance( value, kernel, thisModuleItem, service, storage );
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
    public static ServiceConfigurationParameter fixLinks( Kernel kernel, ModuleItem thisModuleItem, Service service, ServiceStorage storage, final Object value ) {
        if( value instanceof List<?> ) {
            ListIterator<Object> it = ( ( List<Object> ) value ).listIterator();
            while( it.hasNext() ) {
                var oldValue = it.next();
                var v = fixLinks( kernel, thisModuleItem, service, storage, oldValue );
                if( v.value != null ) {
                    it.set( v.value );
                }
            }
        } else if( value instanceof Map<?, ?> ) {
            for( var entry : ( ( Map<?, Object> ) value ).entrySet() ) {
                var v = fixLinks( kernel, thisModuleItem, service, storage, entry.getValue() );
                if( v.value != null ) {
                    entry.setValue( v.value );
                }
            }
        } else if( value instanceof String ) {
            var command = Lists.find2( Kernel.commands, c -> c.matches( value ) );
            if( command != null ) {
                var result = command.getInstance( value, kernel, thisModuleItem, service, storage );
                if( !result.isSuccess() ) {
                    log.trace( "{} not found", value );
                    return new ServiceConfigurationParameter( null, false );
                } else {
                    return new ServiceConfigurationParameter( result.successValue, true );
                }
            }
        }

        return new ServiceConfigurationParameter( value, false );
    }

    public static boolean profileEnabled( LinkedHashSet<String> moduleProfiles, LinkedHashSet<String> systemProfiles ) {
        for( var profile : moduleProfiles ) {
            if( profile.startsWith( "-" ) ) {
                if( systemProfiles.contains( profile.substring( 1 ) ) ) return false;
            } else {
                if( Service.PROFILE_ENABLED.equals( profile ) ) continue;
                if( Service.PROFILE_DISABLED.equals( profile ) ) return false;

                if( !systemProfiles.contains( profile ) ) return false;
            }
        }
        return true;
    }

    public static boolean isModuleEnabled( Module module, LinkedHashSet<String> systemProfiles ) {
        return profileEnabled( module.profiles, systemProfiles );
    }

    public static boolean isServiceEnabled( Service service, LinkedHashSet<String> systemProfiles ) {
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

    record ServiceConfigurationParameter( Object value, boolean ignoreCast ) {
    }

    static class ServiceConfigurationParameters {
        public final LinkedHashMap<String, Object> configurationParameters = new LinkedHashMap<>();
        public final LinkedHashMap<String, Object> serviceReferenceParameters = new LinkedHashMap<>();
    }
}
