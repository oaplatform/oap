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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

@Slf4j( topic = "oap.application.Kernel" )
public class KernelHelper {

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

    @SuppressWarnings( "unchecked" )
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
}
