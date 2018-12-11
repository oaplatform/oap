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

package oap.json;


import java.util.Objects;

public class Formatter {
    public static String format( String json ) {
        StringBuilder b = new StringBuilder();
        boolean string = false;
        boolean escape = false;
        StringBuilder tabs = new StringBuilder();
        for( char c : json.toCharArray() )
            if( c == '"' && !escape ) {
                b.append( "\"" );
                string = !string;
                escape = false;
            } else if( ( c == '{' || c == '[' ) && !string ) {
                tabs.append( "  " );
                b.append( c ).append( "\n" ).append( tabs );
                escape = false;
            } else if( ( c == '}' || c == ']' ) && !string ) {
                tabs.delete( tabs.length() - 2, tabs.length() );
                b.append( "\n" ).append( tabs ).append( c );
                escape = false;
            } else if( c == ',' && !string ) {
                b.append( c ).append( "\n" ).append( tabs );
                escape = false;
            } else if( c == '\\' && string ) {
                b.append( c );
                escape = !escape;
            } else if( c == ':' && !string ) {
                b.append( c ).append( " " );
                escape = false;
            } else if( ( c != '\n' && c != '\r' && c != '\t' && c != ' ' ) || string ) {
                b.append( c );
                escape = false;
            }
        b.append( "\n" );
        return b.toString();
    }

    public static boolean equals( String json1, String json2 ) {
        return Objects.equals(
            format( Objects.requireNonNull( json1 ) ),
            format( Objects.requireNonNull( json2 ) ) );
    }
}
