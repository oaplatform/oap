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

import java.util.LinkedList;
import java.util.List;

public class Tokenizer {
    public static List<String> parse( String line, char delimiter ) {
        return parse( line, delimiter, Integer.MIN_VALUE, false );
    }

    public static List<String> parse( String line, char delimiter, boolean quoted ) {
        return parse( line, delimiter, Integer.MIN_VALUE, quoted );
    }

    public static List<String> parse( String line, char delimiter, int limit, boolean quoted ) {
        List<String> tokens = new LinkedList<>();
        int beginIndex = 0;
        boolean inQuote = false;
        for( int i = 0; i < line.length(); i++ ) {
            char c = line.charAt( i );
            if( c == delimiter && !inQuote ) {
                if( quoted && line.charAt( beginIndex ) == '"' && line.charAt( i - 1 ) == '"' )
                    tokens.add( line.substring( beginIndex + 1, i - 1 ).replaceAll( "\"\"", "\"" ) );
                else tokens.add( line.substring( beginIndex, i ) );
                beginIndex = i + 1;
            }
            if( quoted && c == '"' ) inQuote = !inQuote;

            if( tokens.size() == limit ) return tokens;
        }
        if( quoted && line.charAt( beginIndex ) == '"' && line.charAt( line.length() - 1 ) == '"' )
            tokens.add( line.substring( beginIndex + 1, line.length() - 1 ).replaceAll( "\"\"", "\"" ) );
        else tokens.add( line.substring( beginIndex ) );

        return tokens;
    }
}
