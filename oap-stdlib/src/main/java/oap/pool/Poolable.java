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

package oap.pool;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode( exclude = "pool" )
@ToString( exclude = "pool" )
public class Poolable<T> implements AutoCloseable {
    private final Pool<T> pool;
    final T value;
    private static final Poolable<?> EMPTY = new Poolable<>( null, null );

    private Poolable( Pool<T> pool, T value ) {
        this.pool = pool;
        this.value = value;
    }

    public T get() {
        if( value == null ) throw new NoSuchElementException( "no value present" );
        return value;
    }

    public void ifPresent( Consumer<? super T> action ) {
        try( this ) {
            action.accept( value );
        }
    }

    static <T> Poolable<T> of( Pool<T> pool, T object ) {
        return new Poolable<>( pool, requireNonNull( object ) );
    }

    @SuppressWarnings( "unchecked" )
    static <T> Poolable<T> empty() {
        return ( Poolable<T> ) EMPTY;
    }

    @Override
    public void close() {
        if( value != null )
            pool.release( this );
    }

    public boolean isEmpty() {
        return value == null;
    }
}
