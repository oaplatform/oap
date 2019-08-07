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
package oap.util;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AssocList<K, V> extends LinkedHashSet<V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

    public AssocList() {
    }

    public AssocList( Collection<? extends V> c ) {
        super( c );
        for( V v : c ) map.put( keyOf( v ), v );
    }

    public static <K, V> AssocList<K, V> forKey( Function<? super V, ? extends K> keyOf ) {
        return new AssocList<K, V>() {
            @Override
            protected K keyOf( V v ) {
                return keyOf.apply( v );
            }
        };
    }

    protected abstract K keyOf( V v );

    @Override
    public synchronized boolean add( V v ) {
        if( map.put( keyOf( v ), v ) != null ) removeKey( keyOf( v ) );
        super.add( v );

        return true;
    }

    @Override
    public synchronized boolean remove( Object o ) {
        map.values().remove( o );
        return super.remove( o );
    }

    @Override
    public synchronized boolean addAll( Collection<? extends V> c ) {
        for( V v : c ) map.put( keyOf( v ), v );
        return super.addAll( c );
    }

    @Override
    public synchronized boolean removeAll( Collection<?> c ) {
        map.values().removeAll( c );
        return super.removeAll( c );
    }

    @Override
    public synchronized boolean retainAll( Collection<?> c ) {
        map.values().retainAll( c );
        return super.retainAll( c );
    }

    @Override
    public synchronized boolean removeIf( Predicate<? super V> filter ) {
        map.values().removeIf( filter );
        return super.removeIf( filter );
    }

    public Optional<V> get( K key ) {
        return Optional.ofNullable( map.get( key ) );
    }

    public V getOrDefault( K key, V def ) {
        return map.getOrDefault( key, def );
    }

    public boolean removeKey( K key ) {
        return removeIf( o -> Objects.equals( keyOf( o ), key ) );
    }

    /**
     * @see #getOrDefault(Object, Object)
     */
    @Deprecated
    public V unsafeGet( K key ) {
        return map.get( key );
    }

    public Set<V> getAll( Collection<K> keys ) {
        return Stream.of( keys )
            .foldLeft( new LinkedHashSet<>(), ( result, key ) -> {
                V v = map.get( key );
                if( v != null ) result.add( v );
                return result;
            } );
    }

    public synchronized V computeIfAbsent( K key, Supplier<V> supplier ) {
        if( !containsKey( key ) ) {
            V v = supplier.get();
            Preconditions.checkArgument( Objects.equals( key, keyOf( v ) ) );
            add( v );
        }
        return map.get( key );
    }

    public synchronized boolean containsKey( K key ) {
        return map.containsKey( key );
    }
}
