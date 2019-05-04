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

    public static long parseLongWithUnits( String value ) {
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
                if( Character.isDigit( c ) && stillNumber ) number.append( c );
                else {
                    stillNumber = false;
                    unit.append( c );
                }
            }
            var strNumber = number.toString();
            long res;
            switch( unit.toString().trim().toLowerCase() ) {
                case "kb":
                    res = Long.parseLong( strNumber ) * 1024;
                    break;
                case "mb":
                    res = Long.parseLong( strNumber ) * 1024 * 1024;
                    break;
                case "gb":
                    res = Long.parseLong( strNumber ) * 1024 * 1024 * 1024;
                    break;
                case "ms":
                case "":
                    res = Long.parseLong( strNumber );
                    break;
                case "s":
                case "second":
                case "seconds":
                    res = Long.parseLong( strNumber ) * 1000;
                    break;
                case "m":
                case "minute":
                case "minutes":
                    res = Long.parseLong( strNumber ) * 1000 * 60;
                    break;
                case "h":
                case "hour":
                case "hours":
                    res = Long.parseLong( strNumber ) * 1000 * 60 * 60;
                    break;
                case "d":
                case "day":
                case "days":
                    res = Long.parseLong( strNumber ) * 1000 * 60 * 60 * 24;
                    break;
                case "w":
                case "week":
                case "weeks":
                    res = Long.parseLong( strNumber ) * 1000 * 60 * 60 * 24 * 7;
                    break;
                default:
                    throw new NumberFormatException( value );
            }

            return negative ? -res : res;
        }
        throw new NumberFormatException( "value is null" );
    }
}
