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

public final class Numbers {
    private Numbers() {
    }

    public static long parseLongWithUnits( String value ) throws NumberFormatException {
        try {
            return Dates.stringToDuration( value );
        } catch( IllegalArgumentException e ) {
            if( value != null ) {
                var v = value.trim();
                var unit = new StringBuilder();
                var number = new StringBuilder();
                var negative = false;
                var stillNumber = true;
                for( var i = 0; i < v.length(); i++ ) {
                    var c = value.charAt( i );
                    if( c == '-' && i == 0 ) {
                        negative = true;
                        continue;
                    }
                    if( c == '_' ) continue;
                    if( Character.isDigit( c ) && stillNumber ) number.append( c );
                    else {
                        stillNumber = false;
                        unit.append( c );
                    }
                }
                var strNumber = number.toString();
                long res = switch( unit.toString().trim().toLowerCase() ) {
                    case "kb" -> Long.parseLong( strNumber ) * 1024;
                    case "mb" -> Long.parseLong( strNumber ) * 1024 * 1024;
                    case "gb" -> Long.parseLong( strNumber ) * 1024 * 1024 * 1024;
                    case "ms", "" -> Long.parseLong( strNumber );
                    case "s", "second", "seconds" -> Long.parseLong( strNumber ) * 1000;
                    case "m", "minute", "minutes" -> Long.parseLong( strNumber ) * 1000 * 60;
                    case "h", "hour", "hours" -> Long.parseLong( strNumber ) * 1000 * 60 * 60;
                    case "d", "day", "days" -> Long.parseLong( strNumber ) * 1000 * 60 * 60 * 24;
                    case "w", "week", "weeks" -> Long.parseLong( strNumber ) * 1000 * 60 * 60 * 24 * 7;
                    default -> throw new NumberFormatException( value );
                };

                return negative ? -res : res;
            }
        }

        throw new NumberFormatException( "value is null" );
    }

    public static double parseDoubleWithUnits( String value ) throws NumberFormatException {
        if( value != null ) {
            var v = value.trim();

            if( v.equals( "∞" ) ) return Double.POSITIVE_INFINITY;
            if( v.endsWith( "%" ) ) {
                return Double.parseDouble( v.substring( 0, v.length() - 1 ) ) / 100.0;
            }

            return Double.parseDouble( v );
        }
        throw new NumberFormatException( "value is null" );
    }
}
