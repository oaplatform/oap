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

package oap.http;

import com.google.api.client.util.escape.CharEscapers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static oap.util.Maps.Collectors.toListMultimap;
import static oap.util.Pair.__;

public class Url {
    public static String decode( String s ) {
        return CharEscapers.decodeUri( s );
    }

    public static ListMultimap<String, String> parseQuery( String params ) {
        return Strings.isEmpty( params )
            ? ArrayListMultimap.create()
            : Stream.of( StringUtils.split( params, '&' ) )
                .map( s -> Strings.split( s, "=" ) )
                .map( pair -> __( pair._1, decode( pair._2 ) ) )
                .collect( toListMultimap() );
    }

    public static String encode( String value ) {
        return CharEscapers.escapeUri( value );
    }

    public static List<String> subdomains( String domain ) {
        if( domain == null ) return emptyList();

        ArrayList<String> strings = new ArrayList<>();

        int end;
        int length = domain.length();

        for( int i = domain.lastIndexOf( '.' ); i >= 0; end = i, i = domain.lastIndexOf( '.', end - 1 ) )
            strings.add( domain.substring( i + 1, length ) );

        strings.add( domain );

        return strings;
    }

    public static String domainOf( String url ) {
        if( url == null ) return null;
        if( !url.startsWith( "http" ) ) return url;

        int slashPosition = url.indexOf( '/' );
        if( slashPosition < 0 ) return url;
        int start = slashPosition + 2;
        int end = Strings.indexOfAny( url, "/?#", start );

        if( end > 0 ) return url.substring( start, end );

        return url.substring( start ).toLowerCase();
    }
}
