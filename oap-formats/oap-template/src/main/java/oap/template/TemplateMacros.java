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

package oap.template;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.SneakyThrows;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateMacros {
    private static final ConcurrentHashMap<String, DateTimeFormatter> dateTimeFormatters = new ConcurrentHashMap<>();

    static {
        dateTimeFormatters.put( "SIMPLE", Dates.FORMAT_SIMPLE );
        dateTimeFormatters.put( "MILLIS", Dates.FORMAT_MILLIS );
        dateTimeFormatters.put( "SIMPLE_CLEAN", Dates.FORMAT_SIMPLE_CLEAN );
        dateTimeFormatters.put( "DATE", Dates.FORMAT_DATE );
    }

    public static String urlencode( String src, long depth ) {
        if( src == null ) return null;
        String res = src;
        for( long curr = 0; curr < depth; curr++ ) {
            res = urlencode( res );
        }
        return res;
    }

    @SneakyThrows
    public static String urlencode( String src ) {
        if( src == null ) return null;
        return URLEncoder.encode( src, StandardCharsets.UTF_8.name() );
    }

    public static String urlencodePercent( String src ) {
        return urlencode( src ).replaceAll( "\\+", "%20" );
    }

    public static String urlencodePercent( String src, long depth ) {
        if( src == null ) return null;
        String res = src;
        for( long curr = 0; curr < depth; curr++ ) {
            res = urlencodePercent( res );
        }
        return res;
    }

    public static String toUpperCase( String src ) {
        return src != null ? src.toUpperCase() : null;
    }

    public static String toLowerCase( String src ) {
        return src != null ? src.toLowerCase() : null;
    }

    @JsonAlias( "default" )
    public static Object defaultFunction( String in, Object defaultValue ) {
        if( in == null ) return defaultValue;

        return in.isEmpty() ? defaultValue : in;
    }

    @JsonAlias( "default" )
    public static Object defaultFunction( Collection<?> in, Object defaultValue ) {
        if( in == null ) return defaultValue;

        return in.isEmpty() ? defaultValue : in;
    }

    @JsonAlias( "default" )
    public static Object defaultFunction( Map<?, ?> in, Object defaultValue ) {
        if( in == null ) return defaultValue;

        return in.isEmpty() ? defaultValue : in;
    }

    @JsonAlias( "default" )
    public static Object defaultFunction( Object in, Object defaultValue ) {
        return switch( in ) {
            case null -> defaultValue;
            case String inStr -> defaultFunction( inStr, defaultValue );
            case Collection<?> inCollection -> defaultFunction( inCollection, defaultValue );
            case Map<?, ?> inMap -> defaultFunction( inMap, defaultValue );
            default -> in;
        };

    }

    public static String format( DateTime dateTime, String pattern ) {
        if( dateTime == null ) return null;

        DateTimeFormatter dateTimeFormatter = dateTimeFormatters.computeIfAbsent( pattern, p -> DateTimeFormat
            .forPattern( p )
            .withZoneUTC()
        );

        return dateTimeFormatter.print( dateTime );
    }
}
