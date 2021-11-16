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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.split;

public class BitSet extends java.util.BitSet implements Iterable<Integer> {
    public final int nbits;

    public BitSet() {
        nbits = Integer.MAX_VALUE;
    }

    public BitSet( Enum[] e, ArrayList<? extends Enum<?>> enums, boolean fill ) {
        super( e.length );
        nbits = e.length;

        if( enums == null || enums.isEmpty() ) {
            if( fill ) set( 0, e.length );
        } else
            enums.forEach( ee -> set( ee.ordinal() ) );
    }

    public BitSet( int nbits ) {
        super( nbits );
        this.nbits = nbits;
    }

    public BitSet( List<Integer> bits ) {
        this( Lists.max( bits ) );

        for( var bit : bits ) set( bit, true );
    }

    public BitSet( String bitset ) {
        this();

        for( var b : split( bitset, ',' ) ) {
            var s = split( b, '-' );
            if( s.length == 1 ) {
                set( Integer.parseInt( b.trim() ) );
            } else {
                var f = Integer.parseInt( s[0].trim() );
                var t = Integer.parseInt( s[1].trim() );
                set( f, t + 1 );
            }
        }
    }

    public boolean getAnd( int[] bitIndex ) {
        for( int i : bitIndex ) {
            if( !get( i ) ) return false;
        }

        return true;
    }

    public boolean getAnd( long[] bitIndex ) {
        for( long i : bitIndex ) {
            if( !get( i ) ) return false;
        }

        return true;
    }

    public boolean xorAnd( int[] bitIndex ) {
        for( int i : bitIndex ) {
            if( get( i ) ) return false;
        }

        return true;
    }

    public final boolean getOr( int[] bitIndex ) {
        for( int i : bitIndex ) {
            if( get( i ) ) return true;
        }

        return false;
    }

    public final boolean getOr( long[] bitIndex ) {
        for( long i : bitIndex ) {
            if( get( i ) ) return true;
        }

        return false;
    }

    public final boolean get( long bitIndex ) {
        return get( ( int ) bitIndex );
    }

    public final int max() {
        return previousSetBit( size() - 1 );
    }

    @Override
    public Iterator<Integer> iterator() {
        return stream().iterator();
    }
}
