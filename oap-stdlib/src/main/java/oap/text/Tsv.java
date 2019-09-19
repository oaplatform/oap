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

package oap.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor.petrenko on 09/19/2019.
 */
public class Tsv {
    private static final char TAB = '\t';
    private static final char ESCAPE = '\\';

    public static void split( String line, ArrayList<String> list ) {
        assert line != null;

        var len = line.length();

        int start = 0, i = 0;
        boolean escape = false;
        while( i < len ) {
            var ch = line.charAt( i );
            switch( ch ) {
                case ESCAPE -> escape = !escape;
                case TAB -> {
                    if( !escape ) list.add( line.substring( start, i ) );
                    start = i + 1;
                    escape = false;
                }
                default -> escape = false;
            }
            i++;
        }
        list.add( line.substring( start, i ) );
    }

    public static String escape( String text ) {
        if( text == null || text.length() == 0 ) return "";

        var sb = new StringBuilder();
        
        for(var i = 0; i < text.length(); i++) {
            var ch = text.charAt( i );
            switch( ch ) {
                case '\n' -> sb.append( "\\\n" );
                case '\r' -> sb.append( "\\\r" );
                case '\t' -> sb.append( "\\\t" );
                case '\\' -> sb.append( "\\\\" );
                default -> sb.append( ch );
            }
        }
        
        return sb.toString();
    }
}
