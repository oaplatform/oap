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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

public final class Countries {
    public static final String[] iso3Countries;
    private static final HashMap<String, Integer> bitMap = new HashMap<>();
    private static final HashMap<String, String> twoChTo3Ch = new HashMap<>();

    public static final int count;

    static {

        List<Locale> locales = Stream.of( Locale.getISOCountries() )
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
        if( ordinal == 0 ) return Strings.UNKNOWN;
        return iso3Countries[( ( int ) ordinal - 1 )];
    }
}
