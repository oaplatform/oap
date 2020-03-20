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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public class Url {
    public static final Escaper ENCODER = UrlEscapers.urlFormParameterEscaper();

    public static String decode( String value ) {
        return URLDecoder.decode( value, UTF_8 );
    }

    public static void parseQuery( String params, ListMultimap<String, String> map ) {
        if( StringUtils.isEmpty( params ) ) return;

        var pairs = StringUtils.split( params, '&' );
        for( var pair : pairs ) {
            var idx = pair.indexOf( "=" );
            var key = idx > 0 ? pair.substring( 0, idx ) : pair;
            var value = idx > 0 && pair.length() > idx + 1 ? decode( pair.substring( idx + 1 ) ) : "";
            map.put( key, value );
        }
    }

    public static void parseQuery( String params, Map<String, String> map ) {
        if( StringUtils.isEmpty( params ) ) return;

        var pairs = StringUtils.split( params, '&' );
        for( var pair : pairs ) {
            var idx = pair.indexOf( "=" );
            var key = idx > 0 ? pair.substring( 0, idx ) : pair;
            var value = idx > 0 && pair.length() > idx + 1 ? decode( pair.substring( idx + 1 ) ) : "";
            map.put( key, value );
        }
    }

    public static ListMultimap<String, String> parseQuery( String params ) {
        var map = ArrayListMultimap.<String, String>create();
        parseQuery( params, map );
        return map;
    }

    public static String encode( String value ) {
        return ENCODER.escape( value );
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
