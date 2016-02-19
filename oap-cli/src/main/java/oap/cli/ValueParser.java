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
package oap.cli;

import oap.util.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public interface ValueParser<V> {
    Result<V, String> parse( String value );

    static final String[] TRUE_VALUES = { "true", "yes", "y", "t" };
    static final String[] FALSE_VALUES = { "false", "no", "n", "f" };

    public static final ValueParser<Boolean> BOOLEAN = value -> {
        if( value == null ) return Result.failure( "null value" );
        for( String v : TRUE_VALUES ) if( v.equals( value.toLowerCase() ) ) return Result.success( true );
        for( String v : FALSE_VALUES ) if( v.equals( value.toLowerCase() ) ) return Result.success( false );
        return Result.failure( "wrong value: " + value );
    };

    public static final ValueParser<String> STRING = Result::success;

    public static final ValueParser<Map<String, String>> MAP = value -> {
        if( value == null ) return Result.failure( "value should not be null" );
        else return Result.success(
                Maps.of(
                    Arrays.<String, Pair<String, String>>map(
                        Pair.class,
                        kv -> Strings.split( kv, "=" ),
                        value.split( "," ) ) ) );
    };

    public static ValueParser<Long> LONG( long min, long max ) {
        return value -> {
            try {
                long l = Long.parseLong( value );
                if( l < min || l > max ) return Result.failure( "_2 must be between " + min + " and " + max );
                return Result.success( l );
            } catch( NumberFormatException e ) {
                return Result.failure( e.getMessage() );
            }
        };
    }

    public static ValueParser<Integer> INT( int min, int max ) {
        return value -> {
            try {
                int l = Integer.parseInt( value );
                if( l < min || l > max ) return Result.failure( "_2 must be between " + min + " and " + max );
                return Result.success( l );
            } catch( NumberFormatException e ) {
                return Result.failure( e.getMessage() );
            }
        };
    }

    public static ValueParser<Date> DATE( String pattern ) {
        SimpleDateFormat format = new SimpleDateFormat( pattern );
        return value -> {
            if( value == null ) return Result.failure( "date should not be null" );
            try {
                return Result.success( format.parse( value ) );
            } catch( ParseException e ) {
                return Result.failure( e.getMessage() );
            }
        };
    }
}
