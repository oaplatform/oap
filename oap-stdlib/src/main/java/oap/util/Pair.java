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
import java.io.Serializable;
import java.util.function.BiFunction;

import static java.util.Objects.deepEquals;
import static oap.util.Strings.deepToString;

public class Pair<K, V> implements Serializable {
    @SuppressWarnings( "checkstyle:MemberName" )
    public final K _1;
    @SuppressWarnings( "checkstyle:MemberName" )
    public final V _2;

    @SuppressWarnings( "checkstyle:ParameterName" )
    public Pair( K _1, V _2 ) {
        this._1 = _1;
        this._2 = _2;
    }

    @SuppressWarnings( { "checkstyle:MethodName", "checkstyle:ParameterName" } )
    public static <K, V> Pair<K, V> __( K _1, V _2 ) {
        return new Pair<>( _1, _2 );
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        Pair<?, ?> pair = ( Pair<?, ?> ) o;

        if( _1 != null ? !deepEquals( _1, pair._1 ) : pair._1 != null ) return false;
        if( _2 != null ? !deepEquals( _2, pair._2 ) : pair._2 != null ) return false;

        return true;
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    public K _1() {
        return _1;
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    public V _2() {
        return _2;
    }

    @SuppressWarnings( "unchecked" )
    public <CK extends Comparable<CK>, CV extends Comparable<CV>> ComparablePair<CK, CV> toComparable() {
        return new ComparablePair<>( ( CK ) _1, ( CV ) _2 );
    }

    @SuppressWarnings( "unchecked" )
    public <CK extends Comparable<CK>> KeyComparablePair<CK, V> toComparableByKey() {
        return new KeyComparablePair<>( ( CK ) _1, _2 );
    }

    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + ( _2 != null ? _2.hashCode() : 0 );
        return result;
    }

    public <KR, VR> Pair<KR, VR> map( BiFunction<K, V, Pair<KR, VR>> mapper ) {
        return mapper.apply( _1, _2 );
    }

    public <R> R fold( BiFunction<K, V, R> mapper ) {
        return mapper.apply( _1, _2 );
    }

    @Override
    public String toString() {
        return "__(" + deepToString( _1 ) + ", " + deepToString( _2 ) + ')';
    }

    public static class ComparablePair<CK extends Comparable<CK>, CV extends Comparable<CV>> extends Pair<CK, CV> implements Comparable<ComparablePair<CK, CV>> {
        @SuppressWarnings( "checkstyle:ParameterName" )
        public ComparablePair( CK _1, CV _2 ) {
            super( _1, _2 );
        }

        @SuppressWarnings( "checkstyle:MethodName" )
        public static <CK extends Comparable<CK>, CV extends Comparable<CV>> ComparablePair<CK, CV> ___( CK k, CV v ) {
            return new ComparablePair<>( k, v );
        }

        @Override
        public int compareTo( @Nonnull ComparablePair<CK, CV> other ) {
            int result = this._1.compareTo( other._1 );
            return result == 0 ? this._2.compareTo( other._2 ) : result;
        }
    }

    public static class KeyComparablePair<K extends Comparable<K>, V> extends Pair<K, V> implements Comparable<KeyComparablePair<K, V>> {
        @SuppressWarnings( "checkstyle:ParameterName" )
        public KeyComparablePair( K _1, V _2 ) {
            super( _1, _2 );
        }

        @SuppressWarnings( "checkstyle:MethodName" )
        public static <K extends Comparable<K>, V> KeyComparablePair<K, V> ___( K k, V v ) {
            return new KeyComparablePair<>( k, v );
        }

        @Override
        public int compareTo( KeyComparablePair<K, V> other ) {
            return this._1.compareTo( other._1 );
        }
    }
}
