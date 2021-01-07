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
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class AssocList<K, V> extends LinkedHashSet<V> {
    public AssocList() {
    }

    public AssocList( Collection<? extends V> c ) {
        addAll( c );
    }

    public static <K, V> AssocList<K, V> forKey( Function<? super V, ? extends K> keyOf ) {
        return new AssocList<>() {
            @Override
            protected K keyOf( V v ) {
                return keyOf.apply( v );
            }
        };
    }

    protected abstract K keyOf( V v );

    public Optional<V> get( K key ) {
        return this.stream().filter( v -> Objects.equals( key, keyOf( v ) ) ).findAny();
    }

    public V getOrDefault( K key, V def ) {
        return get( key ).orElse( def );
    }

    public boolean removeKey( K key ) {
        return removeIf( o -> hasKey( o, key ) );
    }

    protected boolean hasKey( V o, K key ) {
        return Objects.equals( keyOf( o ), key );
    }

    /**
     * @see #getOrDefault(Object, Object)
     */
    @Deprecated
    public V unsafeGet( K key ) {
        return getOrDefault( key, null );
    }

    public Set<V> getAll( Collection<K> keys ) {
        LinkedHashSet<V> result = new LinkedHashSet<>();
        for( K key : keys ) get( key ).ifPresent( result::add );
        return result;
    }

    public V computeIfAbsent( K key, Supplier<V> supplier ) {
        return get( key ).orElseGet( () -> {
            V v = supplier.get();
            Preconditions.checkArgument( Objects.equals( key, keyOf( v ) ) );
            add( v );
            return v;
        } );
    }

    public boolean containsKey( K key ) {
        return this.stream().anyMatch( o -> hasKey( o, key ) );
    }

    @Override
    public boolean add( V v ) {
        removeKey( keyOf( v ) );
        return super.add( v );
    }

    @Override
    public boolean addAll( Collection<? extends V> c ) {
        for( V v : c ) add( v );
        return !c.isEmpty();
    }
}
