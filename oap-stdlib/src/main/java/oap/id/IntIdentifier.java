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

package oap.id;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class IntIdentifier<T> extends AbstractIdentifier<Integer, T> {
    public static final int MAX_ATTEMPTS = 10000;
    public AtomicInteger generator = new AtomicInteger( 0 );

    public IntIdentifier( Function<? super T, Integer> getter, BiConsumer<? super T, Integer> setter ) {
        super( getter, setter );
    }

    @Override
    public Integer getOrInit( T object, Predicate<Integer> conflict ) {
        var id = getter.apply( object );
        if( id == null ) {
            for( int i = 0; i < MAX_ATTEMPTS; i++ ) {
                id = generator.incrementAndGet();
                if( !conflict.test( id ) ) {
                    setter.accept( object, id );
                    return id;
                }
            }
            throw new IllegalStateException( "maximum attemts generating id reached" );
        } else return id;
    }

    @Override
    public Integer fromString( String id ) {
        return Integer.parseInt( id );
    }

    @Override
    public String toString( Integer id ) {
        return id.toString();
    }

    public static <T> IntIdentifierBuilder<T> forId( final Function<T, Integer> getter ) {
        return IntIdentifierBuilder.forId( getter );
    }

    public static <T> IntIdentifierBuilder<T> forId( final Function<T, Integer> getter, BiConsumer<T, Integer> setter ) {
        return IntIdentifierBuilder.forId( getter, setter );
    }
}
