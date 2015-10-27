package oap.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class StringBits {
    private static final int UNKNOWN = 0;
    private static final HashMap<String, StringBits> instances = new HashMap<>();

    private final HashMap<String, Integer> bits = new HashMap<>();
    private final AtomicInteger bit = new AtomicInteger( 1 );

    private StringBits() {
        bits.put( Strings.UNKNOWN, UNKNOWN );
    }

    public static StringBits l( String name ) {
        return instances.computeIfAbsent( name, n -> new StringBits() );
    }

    public final synchronized int computeIfAbsent( String name ) {
        return bits.computeIfAbsent( name, n -> bit.getAndIncrement() );
    }

    public final int get( String name ) {
        return bits.getOrDefault( name, UNKNOWN );
    }

    public final int[] get( List<String> name ) {
        int[] result = new int[name.size()];

        for( int i = 0; i < result.length; i++ ) {
            result[i] = bits.getOrDefault( name.get( i ), UNKNOWN );
        }

        return result;
    }

    public final BitSet bits( Collection<String> values, boolean fill ) {
        BitSet bitSet = new BitSet( bits.size() );

        if( values == null || values.isEmpty() ) {
            if( fill ) bitSet.set( 0, bits.size() );
            return bitSet;
        }

        values.forEach( v -> bitSet.set( get( v ) ) );
        return bitSet;
    }

    public int size() {
        return bits.size();
    }

    public String valueOf( int bit ) {
        return bits.entrySet()
            .stream()
            .filter( e -> e.getValue() == bit )
            .findAny()
            .map( Map.Entry::getKey )
            .orElse( Strings.UNKNOWN );
    }
}
