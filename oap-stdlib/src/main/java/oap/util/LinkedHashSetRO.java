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

import javax.annotation.Nonnull;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LinkedHashSetRO<T> implements Set<T>, Externalizable {
    private final ArrayList<T> list;
    private final HashSet<T> set;

    public LinkedHashSetRO() {
        set = new HashSet<>();
        list = new ArrayList<>();
    }

    public LinkedHashSetRO( int initialCapacity ) {
        set = new HashSet<>( initialCapacity );
        list = new ArrayList<>( initialCapacity );
    }

    public LinkedHashSetRO( Collection<? extends T> c ) {
        this( c.size() );
        addAll( c );
    }

    @Override
    public boolean add( T t ) {
        var add = set.add( t );
        if( add ) list.add( t );
        return add;
    }

    @Override
    public void clear() {
        throw new IllegalAccessError();
    }

    @Override
    public boolean contains( Object o ) {
        return set.contains( o );
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    @Nonnull
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public boolean remove( Object o ) {
        throw new IllegalAccessError();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        throw new IllegalAccessError();
    }

    @Override
    public boolean addAll( Collection<? extends T> c ) {
        var res = false;
        for( var i : c ) {
            if( add( i ) ) res = true;
        }

        return res;
    }

    @Override
    public boolean containsAll( @Nonnull Collection<?> c ) {
        return set.containsAll( c );
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    @Nonnull
    public <T1> T1[] toArray( T1[] a ) {
        return list.toArray( a );
    }

    @Override
    public boolean removeIf( Predicate<? super T> filter ) {
        throw new IllegalAccessError();
    }

    @Override
    public void forEach( Consumer<? super T> action ) {
        list.forEach( action );
    }

    @Override
    public <T1> T1[] toArray( IntFunction<T1[]> generator ) {
        return list.toArray( generator );
    }

    @Override
    public Stream<T> stream() {
        return list.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return list.parallelStream();
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        throw new IllegalAccessError();
    }

    public void trim() {
        list.trimToSize();
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeObject( list );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        addAll( ( List<T> ) in.readObject() );
    }
}
