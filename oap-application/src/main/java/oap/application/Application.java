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
        return instancesOf( DEFAULT, clazz );
    }

    public static synchronized <T> Stream<T> instancesOf( String kernelName, Class<T> clazz ) {
        return kernel( kernelName ).ofClass( clazz ).stream();
    }

    public static synchronized <T> T service( Class<T> clazz ) {
        return service( DEFAULT, clazz );
    }

    public static synchronized <T> T service( String kernelName, Class<T> clazz ) {
        Iterator<T> services = instancesOf( kernelName, clazz ).iterator();
        return !services.hasNext() ? null : services.next();
    }

    public static synchronized void register( String name, Object service ) {
        register( DEFAULT, name, service );
    }

    public static synchronized void register( String kernelName, String name, Object service ) {
        kernel( kernelName ).register( name, service );
    }

    public static synchronized void unregister( String name ) {
        unregister( DEFAULT, name );
    }

    public static synchronized void unregister( String kernelName, String name ) {
        kernel( kernelName ).unregister( name );
    }

    public static synchronized void unregisterServices() {
        unregister( DEFAULT );
    }

    public static synchronized void unregisterServices( String kernelName ) {
        if( kernels.containsKey( kernelName ) ) kernel( kernelName ).unregisterServices();
    }

    public static synchronized void register( Kernel kernel ) {
        if( kernels.putIfAbsent( kernel.name, kernel ) != null )
            throw new ApplicationException( "kernel " + kernel.name + " already registered" );
    }

    public static synchronized Kernel kernel( String name ) {
        return Maps.getOrThrow( kernels, name, () -> new ApplicationException( "kernel " + name + " is not registered" ) );
    }

    public static synchronized boolean containsKernel( String name ) {
        return kernels.containsKey( name );
    }

    public static synchronized void unregister( Kernel kernel ) {
        kernels.remove( kernel.name );
    }

}
