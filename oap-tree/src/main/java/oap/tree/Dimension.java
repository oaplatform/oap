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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 27.12.2016.
 */
public abstract class Dimension<T> {
    public final String name;

    public Dimension( String name ) {
        this.name = name;
    }

    public static <T extends Enum<?>> Dimension<T> ENUM( String name, Class<T> clazz ) {
        final T[] enumConstants = clazz.getEnumConstants();
        final Map<Integer, String> hash = Stream.of( enumConstants ).collect( Collectors.toMap( e -> e.ordinal(), e -> e.name() ) );

        return new Dimension<T>( name ) {
            @Override
            public String toString( long value ) {
                return hash.get( ( int ) value );
            }

            @Override
            public long toLong( Enum value ) {
                return value.ordinal();
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
            public long toLong( Long value ) {
                return value;
            }
        };
    }

    public abstract String toString( long value );

    public abstract long toLong( T value );

    @Override
    public String toString() {
        return name;
    }
}