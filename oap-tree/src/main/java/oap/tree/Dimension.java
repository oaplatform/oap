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
import java.util.stream.Stream;

import static oap.tree.Dimension.OperationType.CONTAINS;
import static oap.tree.Tree.ANY;

/**
 * Created by igor.petrenko on 27.12.2016.
 */
public abstract class Dimension {
    public final String name;

    public OperationType operationType;

    public Dimension( @NonNull String name, OperationType operationType ) {
        this.name = name;
        this.operationType = operationType;
    }

    public static Dimension ARRAY_ENUM( String name, Class<? extends Enum> clazz ) {
        return ENUM( name, clazz, null );
    }

    public static Dimension ENUM( String name, Class<? extends Enum> clazz, OperationType operationType ) {
        final Enum[] enumConstantsSortedByName = clazz.getEnumConstants();
        Arrays.sort( enumConstantsSortedByName, Comparator.comparing( Enum::name ) );

        final String[] sortedToName = new String[enumConstantsSortedByName.length];
        final int[] ordinalToSorted = new int[enumConstantsSortedByName.length];

        for( int i = 0; i < enumConstantsSortedByName.length; i++ ) {
            sortedToName[i] = enumConstantsSortedByName[i].name();
            ordinalToSorted[enumConstantsSortedByName[i].ordinal()] = i;
        }

        return new Dimension( name, operationType ) {
            @Override
            public String toString( long value ) {
                return sortedToName[( int ) value];
            }

            @Override
            public void init( Stream<Object> value ) {
            }

            @Override
            public long getOrDefault( Object value ) {
                if( value == null ) return ANY;
                return ordinalToSorted[( ( Enum ) value ).ordinal()];
            }
        };
    }

    public static Dimension ARRAY_STRING( String name ) {
        return STRING( name, null );
    }

    public static Dimension STRING( String name, OperationType operationType ) {
        final StringBits bits = new StringBits();

        return new Dimension( name, operationType ) {
            @Override
            public String toString( long value ) {
                return bits.valueOf( value );
            }

            @Override
            public void init( Stream<Object> value ) {
                value.sorted().forEach( v -> bits.computeIfAbsent( ( String ) v ) );
            }

            @Override
            public long getOrDefault( Object value ) {
                if( value == null ) return ANY;
                return bits.get( ( String ) value );
            }
        };
    }

    public static Dimension ARRAY_LONG( String name ) {
        return LONG( name, null );
    }

    public static Dimension LONG( String name, OperationType operationType ) {
        return new Dimension( name, operationType ) {
            @Override
            public String toString( long value ) {
                return String.valueOf( value );
            }

            @Override
            public void init( Stream<Object> value ) {
            }

            @Override
            public long getOrDefault( Object value ) {
                if( value == null ) return ANY;
                return ( ( Number ) value ).longValue();
            }
        };
    }

    public abstract String toString( long value );

    public abstract void init( Stream<Object> value );

    public abstract long getOrDefault( Object value );

    @Override
    public String toString() {
        return name;
    }

    @SuppressWarnings( "unchecked" )
    public BitSet toBitSet( List list ) {
        final BitSet bitSet = new BitSet();
        list.forEach( item -> bitSet.set( ( int ) this.getOrDefault( item ) ) );
        return bitSet;
    }

    public enum OperationType {
        CONTAINS, NOT_CONTAINS
    }
}