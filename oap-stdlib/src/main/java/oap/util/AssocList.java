/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract class AssocList<K, V> extends ArrayList<V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

    public AssocList() {
    }

    public AssocList( Collection<? extends V> c ) {
        super( c );
        for( V v : c ) map.put( keyOf( v ), v );
    }

    protected abstract K keyOf( V v );

    public static <K, V> AssocList<K, V> forKey( Function<? super V, ? extends K> keyOf ) {
        return new AssocList<K, V>() {
            @Override
            protected K keyOf( V v ) {
                return keyOf.apply( v );
            }
        };
    }

    @Override
    public synchronized V set( int index, V v ) {
        V old = super.set( index, v );
        map.remove( keyOf( old ) );
        map.put( keyOf( v ), v );
        return old;

    }

    @Override
    public synchronized boolean add( V v ) {
        map.put( keyOf( v ), v );
        return super.add( v );
    }

    @Override
    public synchronized void add( int index, V v ) {
        map.put( keyOf( v ), v );
        super.add( index, v );
    }

    @Override
    public synchronized V remove( int index ) {
        V old = super.remove( index );
        map.values().remove( old );
        return old;
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
    public synchronized boolean addAll( int index, Collection<? extends V> c ) {
        for( V v : c ) map.put( keyOf( v ), v );
        return super.addAll( index, c );
    }

    @Override
    protected synchronized void removeRange( int fromIndex, int toIndex ) {
        map.values().removeAll( subList( fromIndex, toIndex ) );
        super.removeRange( fromIndex, toIndex );
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

    @Override
    public synchronized void replaceAll( UnaryOperator<V> operator ) {
        super.replaceAll( operator );
        map.clear();
        for( V v : this ) map.put( keyOf( v ), v );
    }

    public Optional<V> get( K key ) {
        return Optional.ofNullable( map.get( key ) );
    }

    public List<V> getAll( Collection<K> keys ) {
        return Stream.of( keys )
            .foldLeft( new ArrayList<>(), ( result, key ) -> {
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
