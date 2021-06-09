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

package oap.time;

import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Time {
    private static final Map<String, DateTimeFormatter> formatters = new ConcurrentHashMap<>();

    public static String format( String format, ReadableInstant instant ) {
        return format( format, DateTimeZone.getDefault(), instant );
    }

    public static String format( String format, DateTimeZone zone, ReadableInstant instant ) {
        return formatter( format, zone ).print( instant );
    }

    public static String format( String format, DateTimeZone zone, ReadablePartial partial ) {
        return formatter( format, zone ).print( partial );
    }

    public static String format( String format, TemporalAccessor temporal ) {
        return format( format, ZoneOffset.systemDefault(), temporal );
    }

    public static String format( String format, ZoneId zone, TemporalAccessor temporal ) {
        return formatter( format, zone ).format( temporal );
    }

    /**
     * {@link org.joda.time.format.DateTimeFormatter} handles it's own caching
     */
    public static org.joda.time.format.DateTimeFormatter formatter( String format, DateTimeZone zone ) {
        return org.joda.time.format.DateTimeFormat.forPattern( format ).withZone( zone );
    }

    public static DateTimeFormatter formatter( String format, ZoneId zone ) {
        return formatters.computeIfAbsent( format + zone.toString(), k -> DateTimeFormatter.ofPattern( format ).withZone( zone ) );
    }
}
