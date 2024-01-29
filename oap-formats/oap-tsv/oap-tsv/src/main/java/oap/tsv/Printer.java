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

package oap.tsv;

import com.google.common.base.Strings;
import oap.util.Stream;

import java.util.List;
import java.util.stream.Collectors;

public class Printer {

    public static <E> String print( Stream<List<E>> stream, char delimiter ) {
        return stream.map( l -> print( l, delimiter ) ).collect( Collectors.joining() );
    }

    public static String print( List<?> list, char delimiter ) {
        return print( list, delimiter, false );
    }

    public static String print( List<?> list, char delimiter, boolean quoted ) {
        return Stream.of( list )
            .map( e -> escape( String.valueOf( e ), quoted ) )
            .collect( Collectors.joining( String.valueOf( delimiter ) ) ) + "\n";
    }

    public static String escape( String text, boolean quoted ) {
        if( Strings.isNullOrEmpty( text ) ) return "";

        var sb = new StringBuilder();
        if( quoted ) sb.append( '"' );
        for( var i = 0; i < text.length(); i++ ) {
            char c = text.charAt( i );
            sb.append( switch( c ) {
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                case '\\' -> "\\\\";
                case '"' -> quoted ? "\"\"" : "\"";
                default -> String.valueOf( c );
            } );
        }
        if( quoted ) sb.append( '"' );

        return sb.toString();
    }

}
