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
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.util.Optionals;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class Linker {

    private Kernel kernel;

    Linker( Kernel kernel ) {
        this.kernel = kernel;
    }

    private static boolean isLink( Object value ) {
        return value instanceof String && ( ( String ) value ).startsWith( "@service:" );
    }

    static String linkedServiceName( String reference ) {
        return reference.substring( "@service:".length() );
    }


    Object link( Module.Service service, Supplier<Object> instantiate ) {
        resolveLinks( service.name, service.parameters );
        Object instance = instantiate.get();
        linkListeners( service, instance );
        return instance;
    }

    @SuppressWarnings( "unchecked" )
    private void linkListeners( Module.Service service, Object instance ) {
        service.listen.forEach( ( listener, reference ) -> {
            log.debug( "setting " + service.name + " to listen to " + reference + " with listener " + listener );
            String methodName = "add" + StringUtils.capitalize( listener ) + "Listener";
            Object linked = kernel.service( linkedServiceName( reference ) );
            if( linked == null )
                throw new ApplicationException( "for " + service.name + " listening object " + reference + " is not found" );
            Optionals.fork( Reflect.reflect( linked.getClass() ).method( methodName ) )
                .ifPresent( m -> m.invoke( linked, instance ) )
                .ifAbsentThrow( () -> new ReflectException( "listener " + listener
                    + " should have method " + methodName + " in " + reference ) );

        } );

    }

    protected Object resolve( String serviceName, String field, String reference, boolean required ) {
        String linkName = linkedServiceName( reference );
        Object linkedService = kernel.service( linkName );
        log.debug( "for {} linking {} -> {} with {}", serviceName, field, reference, linkedService );
        if( linkedService == null && required && kernel.serviceEnabled( linkName ) )
            throw new ApplicationException( "for " + serviceName + " service link " + reference + " is not found" );
        return linkedService;
    }


    @SuppressWarnings( "unchecked" )
    private void resolveLinks( String serviceName, LinkedHashMap<String, Object> map ) {
        for( Map.Entry<String, Object> entry : map.entrySet() ) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if( isLink( value ) ) entry.setValue( resolve( serviceName, key, ( String ) value, true ) );
            else if( value instanceof List<?> ) {
                ListIterator<Object> iterator = ( ( List<Object> ) value ).listIterator();
                while( iterator.hasNext() ) {
                    Object item = iterator.next();
                    if( isLink( item ) ) {
                        Object linkedService = resolve( serviceName, key, ( String ) item, false );
                        if( linkedService == null ) iterator.remove();
                        else iterator.set( linkedService );
                    }
                }
            } else if( value instanceof Map<?, ?> ) {

                Iterator<Map.Entry<String, Object>> iterator = ( ( Map<String, Object> ) value ).entrySet().iterator();
                while( iterator.hasNext() ) {
                    Map.Entry<String, Object> item = iterator.next();
                    if( isLink( item.getValue() ) ) {
                        Object linkedService = resolve( serviceName, key, ( String ) item.getValue(), false );
                        if( linkedService == null ) iterator.remove();
                        else item.setValue( linkedService );
                    }
                }
            }
        }

    }
}
