package oap.util;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class Countries {
    public static final String[] iso3Countries;
    private static final HashMap<String, Integer> bitMap = new HashMap<>();
    private static final HashMap<String, String> twoChTo3Ch = new HashMap<>();

    public static final int count;

    static {

        List<Locale> locales = java.util.stream.Stream.of( Locale.getISOCountries() )
            .map( a2 -> new Locale( "", a2 ) )
            .collect( toList() );

        iso3Countries = locales
            .stream()
            .map( l -> l.getISO3Country().intern() )
            .toArray( String[]::new );


        locales.forEach( l -> twoChTo3Ch.put( l.getCountry().intern(), l.getISO3Country().intern() ) );


        bitMap.put( Strings.UNKNOWN, 0 );

        for( int i = 0; i < iso3Countries.length; i++ ) {
            bitMap.put( iso3Countries[i], i + 1 );
        }

        count = iso3Countries.length + 1;
    }

    public static String toIso3( String iso2 ) {
        return twoChTo3Ch.getOrDefault( iso2, iso2 );
    }

    public static String normalize( String country ) {
        return country == null ? Strings.UNKNOWN : bitMap.containsKey( country ) ? country : Strings.UNKNOWN;
    }

    public static int ordinal( String country ) {
        return bitMap.getOrDefault( country, 0 );
    }

    private Countries() {
    }

    public static Object valueOf( long ordinal ) {
        if(ordinal == 0) return Strings.UNKNOWN;
        return iso3Countries[((int) ordinal - 1)];
    }
}
