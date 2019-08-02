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

package oap.ws;

import oap.util.Stream;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WsServices {
    private static Pattern rxParamPattern = Pattern.compile( "\\{([^:]+):([^\\)]+\\))\\}" );
    private static Pattern namedParamPattern = Pattern.compile( "(?<=[/=])\\{([^\\}]+)\\}" );

    public static Pattern compile( String mapping ) {
        var pattern = namedParamPattern.matcher( rxParamPattern.matcher( mapping ).replaceAll( "$2" ) )
            .replaceAll( "([^/]+)" );
        return Pattern.compile( '^' + ( pattern.equals( "/" ) ? "/?" : pattern ).replace( "=", "\\=" ) + '$' );
    }

    private static String filter( String mapping ) {
        return rxParamPattern.matcher( mapping ).replaceAll( "{$1}" );
    }

    public static Optional<String> pathParam( String mapping, String path, String name ) {
        Matcher matcher = namedParamPattern.matcher( filter( mapping ) );
        return Stream.of( matcher::find, matcher::group )
            .zipWithIndex()
            .filter( p -> p._1.equals( "{" + name + "}" ) )
            .map( p -> p._2 )
            .findFirst()
            .flatMap( group -> {
                Matcher matcher1 = compile( mapping ).matcher( path );
                return matcher1.matches() && group <= matcher1.groupCount()
                    ? Optional.of( matcher1.group( group + 1 ) )
                    : Optional.<String>empty();
            } );
    }
}
