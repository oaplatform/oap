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

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Iterator;

public class PrioritySet<E> extends AbstractSet<E> {
    public static final int PRIORITY_DEFAULT = 0;
    private final SetMultimap<Integer, E> map = MultimapBuilder
        .treeKeys( Integer::compareTo )
        .linkedHashSetValues()
        .build();

    @Override
    public int size() {
        return map.size();
    }

    public boolean add( int priority, E e ) {
        if( map.get( priority ).contains( e ) ) return false;
        if( map.containsValue( e ) ) remove( e );
        return map.put( priority, e );
    }

    @Override
    public boolean add( E e ) {
        return add( PRIORITY_DEFAULT, e );
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return map.values().iterator();
    }

}
