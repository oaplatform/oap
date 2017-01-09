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

import lombok.NonNull;
import oap.util.StringBits;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static oap.tree.Tree.ANY_AS_ARRAY;

/**
 * Created by igor.petrenko on 27.12.2016.
 */
public abstract class Dimension {
    public final String name;
    public boolean queryRequired;
    public OperationType operationType;

    public Dimension( @NonNull String name, OperationType operationType, boolean queryRequired ) {
        this.name = name;
        this.operationType = operationType;
        this.queryRequired = queryRequired;
    }

    public static Dimension ARRAY_ENUM( String name, Class<? extends Enum> clazz, boolean queryRequired ) {
        return ENUM( name, clazz, null, queryRequired );
    }

    public static Dimension ENUM( String name, Class<? extends Enum> clazz, OperationType operationType, boolean queryRequired ) {
        final Enum[] enumConstantsSortedByName = clazz.getEnumConstants();
        Arrays.sort( enumConstantsSortedByName, Comparator.comparing( Enum::name ) );

        final String[] sortedToName = new String[enumConstantsSortedByName.length];
        final int[] ordinalToSorted = new int[enumConstantsSortedByName.length];

        for( int i = 0; i < enumConstantsSortedByName.length; i++ ) {
            sortedToName[i] = enumConstantsSortedByName[i].name();
            ordinalToSorted[enumConstantsSortedByName[i].ordinal()] = i;
        }

        return new Dimension( name, operationType, queryRequired ) {
            @Override
            public String toString( long value ) {
                return sortedToName[( int ) value];
            }

            @Override
            protected void _init( Stream<Object> value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                return ordinalToSorted[( ( Enum ) value ).ordinal()];
            }
        };
    }

    public static Dimension ARRAY_STRING( String name, boolean queryRequired ) {
        return STRING( name, null, queryRequired );
    }

    public static Dimension STRING( String name, OperationType operationType, boolean queryRequired ) {
        final StringBits bits = new StringBits();

        return new Dimension( name, operationType, queryRequired ) {
            @Override
            public String toString( long value ) {
                return bits.valueOf( value );
            }

            @Override
            protected void _init( Stream<Object> value ) {
                value.sorted().forEach( v -> bits.computeIfAbsent( ( String ) v ) );
            }

            @Override
            protected long _getOrDefault( Object value ) {
                return bits.get( ( String ) value );
            }
        };
    }

    public static Dimension ARRAY_LONG( String name, boolean queryRequired ) {
        return LONG( name, null, queryRequired );
    }

    public static Dimension LONG( String name, OperationType operationType, boolean queryRequired ) {
        return new Dimension( name, operationType, queryRequired ) {
            @Override
            public String toString( long value ) {
                return String.valueOf( value );
            }

            @Override
            protected void _init( Stream<Object> value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                return ( ( Number ) value ).longValue();
            }
        };
    }

    public static Dimension ARRAY_BOOLEAN( String name, boolean queryRequired ) {
        return BOOLEAN( name, null, queryRequired );
    }

    public static Dimension BOOLEAN( String name, OperationType operationType, boolean queryRequired ) {
        return new Dimension( name, operationType, queryRequired ) {
            @Override
            public String toString( long value ) {
                return value == 0 ? "false" : "true";
            }

            @Override
            protected void _init( Stream<Object> value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                return Boolean.TRUE.equals( value ) ? 1 : 0;
            }
        };
    }

    public abstract String toString( long value );

    public final void init( Stream<Object> value ) {
        _init( value
            .filter( v -> !(v instanceof Optional) || ( ( Optional ) v ).isPresent() )
            .map( v -> v instanceof Optional ? ( ( Optional ) v ).get() : v ) );
    }

    protected abstract void _init( Stream<Object> value );

    public final long[] getOrDefault( Object value ) {
        if( value == null ) return ANY_AS_ARRAY;

        if( value instanceof Optional<?> ) {
            Optional<?> optValue = ( Optional<?> ) value;
            return optValue.map( this::getOrDefault ).orElse( ANY_AS_ARRAY );
        }

        if( value instanceof List ) {
            final List<?> list = ( List<?> ) value;
            return list.isEmpty() ? ANY_AS_ARRAY : list.stream().mapToLong( this::_getOrDefault ).toArray();
        } else {
            return new long[] { _getOrDefault( value ) };
        }
    }

    protected abstract long _getOrDefault( Object value );

    @Override
    public String toString() {
        return name;
    }

    @SuppressWarnings( "unchecked" )
    public BitSet toBitSet( List list ) {
        final BitSet bitSet = new BitSet();
        list.forEach( item -> bitSet.set( ( int ) this._getOrDefault( item ) ) );
        return bitSet;
    }

    public enum OperationType {
        CONTAINS, NOT_CONTAINS
    }
}
