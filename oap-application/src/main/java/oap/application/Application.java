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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Application {
    private static final Map<String, Object> services = new HashMap<>();
    private static final Set<String> profiles = new HashSet<>();

    @SuppressWarnings( "unchecked" )
    @Nullable
    public static <T> T service( String name ) {
        return ( T ) services.get( name );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> Stream<T> instancesOf( Class<T> clazz ) {
        return services
            .values()
            .stream()
            .filter( clazz::isInstance )
            .map( x -> ( T ) x );
    }

    public static <T> T service( Class<T> clazz ) {
        Iterator<T> services = instancesOf( clazz ).iterator();
        return !services.hasNext() ? null : services.next();
    }

    public synchronized static void register( String name, Object service ) throws DuplicateServiceException {
        if( services.containsKey( name ) ) throw new DuplicateServiceException( name );

        services.put( name, service );
    }

    public static synchronized void unregister( String name ) {
        services.remove( name );
    }

    public static synchronized void unregisterServices() {
        services.clear();
    }

    public static synchronized void registerProfiles( final Collection<String> inputProfiles ) {
        profiles.addAll( inputProfiles );
    }

    public static Set<String> getProfiles() {
        return Collections.unmodifiableSet( profiles );
    }

    public static class DuplicateServiceException extends RuntimeException {
        public DuplicateServiceException( String service ) {
            super( "Service " + service + " is already registered" );
        }
    }

}
