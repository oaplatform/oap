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

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ThreadSafe
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class AssocList<K, V> implements Collection<V>, SequencedCollection<V>, Cloneable {
    private final CopyOnWriteArrayList<V> list = new CopyOnWriteArrayList<>();

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

    public V get( int index ) {
        return list.get( index );
    }

    public V getOrDefault( K key, V def ) {
        return get( key ).orElse( def );
    }

    public boolean removeKey( K key ) {
        return removeIf( o -> hasKey( o, key ) );
    }

    @Override
    public boolean remove( Object o ) {
        return list.remove( o );
    }

    public V remove( int index ) {
        return list.remove( index );
    }

    public V set( int index, V element ) {
        return list.set( index, element );
    }

    public V getFirst() {
        return list.getFirst();
    }

    public V getLast() {
        return list.getLast();
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

    public synchronized V computeIfAbsent( K key, Supplier<V> supplier ) {
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
    public synchronized boolean add( V v ) {
        removeKey( keyOf( v ) );
        return list.add( v );
    }

    @Override
    public synchronized boolean addAll( Collection<? extends V> c ) {
        for( V v : c ) add( v );
        return !c.isEmpty();
    }

    @Override
    public Iterator<V> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach( Consumer<? super V> action ) {
        list.forEach( action );
    }

    @Override
    public Spliterator<V> spliterator() {
        return list.spliterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains( Object o ) {
        return list.contains( o );
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray( T[] a ) {
        return list.toArray( a );
    }

    @Override
    public <T> T[] toArray( IntFunction<T[]> generator ) {
        return list.toArray( generator );
    }

    @Override
    public boolean containsAll( Collection<?> c ) {
        return list.containsAll( c );
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        return list.removeAll( c );
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        return list.retainAll( c );
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean removeIf( Predicate<? super V> filter ) {
        return list.removeIf( filter );
    }

    @Override
    public Stream<V> stream() {
        return list.stream();
    }

    @Override
    public Stream<V> parallelStream() {
        return list.parallelStream();
    }

    @Override
    public void addFirst( V v ) {
        list.addFirst( v );
    }

    @Override
    public void addLast( V v ) {
        list.addLast( v );
    }

    @Override
    public V removeFirst() {
        return list.removeFirst();
    }

    @Override
    public SequencedCollection<V> reversed() {
        return list.reversed();
    }

    @Override
    public V removeLast() {
        return list.removeLast();
    }
}
