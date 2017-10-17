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

import oap.util.Maps;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static oap.application.Kernel.DEFAULT;

public class Application {
    private static final ConcurrentMap<String, Kernel> kernels = new ConcurrentHashMap<>();

    public static synchronized <T> T service( String name ) {
        return kernel( DEFAULT ).service( name );
    }

    public static synchronized <T> Stream<T> instancesOf( Class<T> clazz ) {
        return kernel( DEFAULT ).ofClass( clazz ).stream();
    }

    public static synchronized <T> T service( Class<T> clazz ) {
        Iterator<T> services = instancesOf( clazz ).iterator();
        return !services.hasNext() ? null : services.next();
    }

    public static synchronized void register( String name, Object service ) {
        kernel( DEFAULT ).register( name, service );
    }

    public static synchronized void unregister( String name ) {
        kernel( DEFAULT ).unregister( name );
    }

    @Deprecated
    public static synchronized void unregisterServices() {
        if(kernels.containsKey( DEFAULT )) kernel( DEFAULT ).unregisterServices();
    }

    public static synchronized void register( Kernel kernel ) {
        if( kernels.putIfAbsent( kernel.name, kernel ) != null )
            throw new ApplicationException( "kernel " + kernel.name + " already registered" );
    }

    public static synchronized Kernel kernel( String name ) {
        return Maps.getOrThrow( kernels, name, () -> new ApplicationException( "kernel " + name + " is not registered" ) );
    }

    public static synchronized void unregister( Kernel kernel ) {
        kernels.remove( kernel.name );
    }

}
