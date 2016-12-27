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

package oap.tree;

import oap.util.StringBits;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 27.12.2016.
 */
public abstract class Dimension<T> {
    public final String name;

    public Dimension( String name ) {
        this.name = name;
    }

    public static <T extends Enum<?>> Dimension<T> ENUM( String name ) {
        final StringBits bits = new StringBits();

        return new Dimension<T>( name ) {
            @Override
            public String toString( long value ) {
                return bits.valueOf( value );
            }

            @Override
            public void init( Stream<T> value ) {
                value.sorted( Comparator.comparing( e -> e.name() ) ).forEach( e -> bits.computeIfAbsent( e.name() ) );
            }

            @Override
            public long getOrDefault( T value ) {
                return bits.get( value.name() );
            }
        };
    }

    public static Dimension<String> STRING( String name ) {
        final StringBits bits = new StringBits();

        return new Dimension<String>( name ) {
            @Override
            public String toString( long value ) {
                return bits.valueOf( value );
            }

            @Override
            public void init( Stream<String> value ) {
                value.sorted().forEach( bits::computeIfAbsent );
            }

            @Override
            public long getOrDefault( String value ) {
                return bits.get( value );
            }
        };
    }

    public static Dimension<Long> LONG( String name ) {
        return new Dimension<Long>( name ) {
            @Override
            public String toString( long value ) {
                return String.valueOf( value );
            }

            @Override
            public void init( Stream<Long> value ) {
            }

            @Override
            public long getOrDefault( Long value ) {
                return value;
            }
        };
    }

    public abstract String toString( long value );

    public abstract void init( Stream<T> value );

    public abstract long getOrDefault( T value );

    @Override
    public String toString() {
        return name;
    }
}