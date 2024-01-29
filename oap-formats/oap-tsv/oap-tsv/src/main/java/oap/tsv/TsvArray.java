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

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TsvArray {
    public static String print( List<Object> list, DateTimeFormatter dateTimeFormatter ) {
        StringJoiner sj = new StringJoiner( ",", "[", "]" );

        for( var item : list ) {
            if( item instanceof String items ) {
                sj.add( "'" + escape( items ) + "'" );
            } else if( item instanceof DateTime itemd ) {
                sj.add( "'" + dateTimeFormatter.print( itemd ) + "'" );
            } else {
                sj.add( String.valueOf( item ) );
            }
        }

        return sj.toString();
    }

    private static String escape( String item ) {
        return StringUtils.replace( item, "'", "\\'" );
    }

    public static List<String> parse( String item ) {
        var array = item.substring( 1, item.length() - 1 );
        List<String> ret = new ArrayList<>();

        if( array.isEmpty() ) return ret;

        StringBuilder sb = new StringBuilder();

        boolean strBegin = false;
        boolean escape = false;

        for( var i = 0; i < array.length(); i++ ) {
            var ch = array.charAt( i );
            switch( ch ) {
                case '\'':
                    if( strBegin && escape ) {
                        sb.append( '\'' );
                        escape = false;
                    }
                    strBegin = !strBegin;
                    break;
                case ',':
                    ret.add( sb.toString() );
                    sb.delete( 0, sb.length() );
                    break;
                case '\\':
                    if( escape ) {
                        sb.append( "\\" );
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                default:
                    if( escape ) {
                        escape = false;
                        sb.append( "\\" ).append( ch );
                    } else {
                        sb.append( ch );
                    }
            }
        }

        ret.add( sb.toString() );

        return ret;
    }
}
