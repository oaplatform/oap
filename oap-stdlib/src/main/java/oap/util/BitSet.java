package oap.util;

import java.util.ArrayList;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class BitSet extends java.util.BitSet {
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

    public boolean getAnd( int[] bitIndex ) {
        for( int i : bitIndex ) {
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

    public boolean getOr( int[] bitIndex ) {
        for( int i : bitIndex ) {
            if( get( i ) ) return true;
        }

        return false;
    }

    public final int max() {
        return previousSetBit( size() - 1 );
    }

}
