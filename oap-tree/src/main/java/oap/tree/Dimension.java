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

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import oap.util.StringBits;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static oap.tree.Consts.ANY_AS_ARRAY;

@EqualsAndHashCode
public abstract class Dimension {
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_LOW = Integer.MIN_VALUE;


    public final String name;
    public final int priority;
    public final long[] nullAsLong;
    public final boolean emptyAsFailed;
    public OperationType operationType;

    public Dimension( @NonNull String name, OperationType operationType, int priority, long[] nullAsLong, boolean emptyAsFailed ) {
        this.name = name;
        this.operationType = operationType;
        this.priority = priority;
        this.nullAsLong = nullAsLong;
        this.emptyAsFailed = emptyAsFailed;
    }

    public static <T extends Enum> Dimension ARRAY_ENUM( String name, Class<T> clazz, T nullValue, boolean emptyAsFailed ) {
        return ENUM( name, clazz, null, PRIORITY_DEFAULT, nullValue, false );
    }

    public static <T extends Enum> Dimension ARRAY_ENUM( String name, Class<T> clazz, T nullValue ) {
        return ENUM( name, clazz, null, PRIORITY_DEFAULT, nullValue, false );
    }

    public static <T extends Enum> Dimension ENUM( String name, Class<T> clazz, OperationType operationType, T nullValue ) {
        return ENUM( name, clazz, operationType, PRIORITY_DEFAULT, nullValue, false );
    }

    public static <T extends Enum> Dimension ENUM( String name, Class<T> clazz, OperationType operationType, int priority, T nullValue, boolean emptyAsFailed ) {
        final Enum[] enumConstantsSortedByName = clazz.getEnumConstants();
        Arrays.sort( enumConstantsSortedByName, Comparator.comparing( Enum::name ) );

        final String[] sortedToName = new String[enumConstantsSortedByName.length];
        final int[] ordinalToSorted = new int[enumConstantsSortedByName.length];

        for( int i = 0; i < enumConstantsSortedByName.length; i++ ) {
            sortedToName[i] = enumConstantsSortedByName[i].name();
            ordinalToSorted[enumConstantsSortedByName[i].ordinal()] = i;
        }

        return new Dimension( name, operationType, priority,
            nullValue == null ? ANY_AS_ARRAY : new long[] { ordinalToSorted[nullValue.ordinal()] }, emptyAsFailed ) {
            @Override
            public String toString( long value ) {
                return sortedToName[( int ) value];
            }

            @Override
            protected void _init( Object value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                Preconditions.checkArgument( value instanceof Enum, "[" + name + "] value (" + value + " ) must be Enum" );

                return ordinalToSorted[( ( Enum<?> ) value ).ordinal()];
            }
        };
    }

    public static Dimension ARRAY_STRING( String name, boolean emptyAsFailed ) {
        return STRING( name, null, PRIORITY_DEFAULT, emptyAsFailed );
    }

    public static Dimension ARRAY_STRING( String name ) {
        return STRING( name, null, PRIORITY_DEFAULT, false );
    }

    public static Dimension STRING( String name, OperationType operationType ) {
        return STRING( name, operationType, PRIORITY_DEFAULT, false );

    }

    public static Dimension STRING( String name, OperationType operationType, int priority, boolean emptyAsFailed ) {
        final StringBits bits = new StringBits();

        return new Dimension( name, operationType, priority, new long[] { StringBits.UNKNOWN }, emptyAsFailed ) {
            @Override
            public String toString( long value ) {
                return bits.valueOf( value );
            }

            @Override
            protected void _init( Object value ) {
                bits.computeIfAbsent( ( String ) value );
            }

            @Override
            protected long _getOrDefault( Object value ) {
                assert value instanceof String : "[" + name + "] value (" + value.getClass() + " ) must be String";

                return bits.get( ( String ) value );
            }
        };
    }

    public static Dimension ARRAY_LONG( String name, Long nullValue ) {
        return LONG( name, null, PRIORITY_DEFAULT, nullValue, false );
    }

    public static Dimension ARRAY_LONG( String name, Long nullValue, boolean emptyAsFailed ) {
        return LONG( name, null, PRIORITY_DEFAULT, nullValue, emptyAsFailed );
    }

    public static Dimension LONG( String name, OperationType operationType, Long nullValue ) {
        return LONG( name, operationType, PRIORITY_DEFAULT, nullValue, false );
    }

    public static Dimension LONG( String name, OperationType operationType, int priority, Long nullValue, boolean emptyAsFailed ) {
        return new Dimension( name, operationType, priority,
            nullValue == null ? ANY_AS_ARRAY : new long[] { nullValue }, emptyAsFailed ) {
            @Override
            public String toString( long value ) {
                return String.valueOf( value );
            }

            @Override
            protected void _init( Object value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                assert value instanceof Number : "[" + name + "] value (" + value.getClass() + " ) must be Number";

                return ( ( Number ) value ).longValue();
            }
        };
    }

    public static Dimension ARRAY_BOOLEAN( String name, Boolean nullValue ) {
        return BOOLEAN( name, null, PRIORITY_DEFAULT, nullValue, false );
    }

    public static Dimension ARRAY_BOOLEAN( String name, Boolean nullValue, boolean emptyAsFailed ) {
        return BOOLEAN( name, null, PRIORITY_DEFAULT, nullValue, emptyAsFailed );
    }

    public static Dimension BOOLEAN( String name, OperationType operationType, Boolean nullValue ) {
        return BOOLEAN( name, operationType, PRIORITY_DEFAULT, nullValue, false );
    }

    public static Dimension BOOLEAN( String name, OperationType operationType, int priority, Boolean nullValue, boolean emptyAsFailed ) {
        final long[] nullAsLong = nullValue == null ? ANY_AS_ARRAY : new long[] { ( nullValue ? 1 : 0 ) };

        return new Dimension( name, operationType, priority, nullAsLong, emptyAsFailed ) {
            @Override
            public String toString( long value ) {
                return value == 0 ? "false" : "true";
            }

            @Override
            protected void _init( Object value ) {
            }

            @Override
            protected long _getOrDefault( Object value ) {
                assert value instanceof Boolean : "[" + name + "] value (" + value.getClass() + " ) must be Boolean";

                return Boolean.TRUE.equals( value ) ? 1 : 0;
            }
        };
    }

    public abstract String toString( long value );

    final void init( Object value ) {
        if( value == null ) return;
        if( value instanceof Optional<?> ) {
            ( ( Optional<?> ) value ).ifPresent( this::_init );
        } else
            _init( value );
    }

    protected abstract void _init( Object value );

    final long[] getOrNullValue( Object value ) {
        return getOrDefault( value, nullAsLong );
    }

    final long[] getOrDefault( Object value, long[] emptyValue ) {
        if( value == null ) return emptyValue;

        if( value instanceof Optional<?> ) {
            Optional<?> optValue = ( Optional<?> ) value;
            return optValue.map( v -> getOrDefault( v, emptyValue ) ).orElse( emptyValue );
        }

        if( value instanceof Collection ) {
            final Collection<?> list = ( Collection<?> ) value;
            return list.isEmpty() ? emptyValue : list.stream().mapToLong( this::_getOrDefault ).sorted().toArray();
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
    BitSet toBitSet( List list ) {
        final BitSet bitSet = new BitSet();
        list.forEach( item -> bitSet.set( ( int ) this._getOrDefault( item ) ) );
        return bitSet;
    }

    final int direction( long[] qValue, long nodeValue ) {
        final int qValueLength = qValue.length;

        final long head = qValue[0];
        switch( operationType ) {
            case CONTAINS:
            case CONTAINS_ALL:
                if( qValueLength == 1 ) {
                    if( head > nodeValue ) return Direction.RIGHT;
                    else if( head < nodeValue ) return Direction.LEFT;
                    else return Direction.EQUAL;
                } else {
                    final long last = qValue[qValueLength - 1];

                    int v = 0;
                    if( last > nodeValue ) v |= Direction.RIGHT;
                    if( head < nodeValue ) v |= Direction.LEFT;

                    if( Arrays.binarySearch( qValue, nodeValue ) >= 0 ) {
                        v |= Direction.EQUAL;
                    }

                    return v;
                }

            case NOT_CONTAINS:
                return qValueLength > 1 || head != nodeValue
                    ? Direction.EQUAL | Direction.LEFT | Direction.RIGHT
                    : Direction.LEFT | Direction.RIGHT;

            case GREATER_THEN:
                assert qValueLength == 1;

                if( head < nodeValue ) return Direction.RIGHT | Direction.EQUAL | Direction.LEFT;
                return Direction.RIGHT;

            case GREATER_THEN_OR_EQUAL_TO:
                assert qValueLength == 1;

                if( head < nodeValue ) return Direction.EQUAL | Direction.RIGHT | Direction.LEFT;
                else if( head == nodeValue ) return Direction.EQUAL | Direction.RIGHT;
                else return Direction.RIGHT;

            case LESS_THEN_OR_EQUAL_TO:
                assert qValueLength == 1;

                if( head > nodeValue ) return Direction.EQUAL | Direction.RIGHT | Direction.LEFT;
                else if( head == nodeValue ) return Direction.EQUAL | Direction.LEFT;
                else return Direction.LEFT;

            case LESS_THEN:
                assert qValueLength == 1;

                if( head > nodeValue ) return Direction.RIGHT | Direction.EQUAL | Direction.LEFT;
                return Direction.LEFT;

            case BETWEEN_INCLUSIVE:
                assert qValueLength == 2;

                int ret = 0;
                final long right = qValue[1];
                if( right > nodeValue ) ret |= Direction.RIGHT;
                if( head < nodeValue ) ret |= Direction.LEFT;
                if( right == nodeValue || head == nodeValue || ( ret == ( Direction.RIGHT | Direction.LEFT ) ) )
                    ret |= Direction.EQUAL;

                return ret;

            default:
                throw new IllegalStateException( "Unknown OperationType " + operationType );
        }
    }

    public enum OperationType {
        CONTAINS,
        CONTAINS_ALL,
        NOT_CONTAINS,
        GREATER_THEN,
        GREATER_THEN_OR_EQUAL_TO,
        LESS_THEN,
        LESS_THEN_OR_EQUAL_TO,
        BETWEEN_INCLUSIVE;
    }

    public interface Direction {
        int NONE = 0;
        int LEFT = 1;
        int EQUAL = 1 << 1;
        int RIGHT = 1 << 2;
    }
}
