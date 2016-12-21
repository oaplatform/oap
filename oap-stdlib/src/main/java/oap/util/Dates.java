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

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Dates {
    public static final DateTimeFormatter FORMAT_MILLIS = DateTimeFormat
        .forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS" )
        .withZone( DateTimeZone.getDefault() );

    public static final DateTimeFormatter FORMAT_SIMPLE = DateTimeFormat
        .forPattern( "yyyy-MM-dd'T'HH:mm:ss" )
        .withZone( DateTimeZone.getDefault() );

    public static Result<DateTime, Exception> parseDateWithMillis( String date ) {
        return parse( date, FORMAT_MILLIS );
    }

    public static Result<DateTime, Exception> parseDate( String date ) {
        return parse( date, FORMAT_SIMPLE );
    }

    private static Result<DateTime, Exception> parse( String date, DateTimeFormatter formatter ) {
        try {
            return Result.success( formatter.parseDateTime( date ) );
        } catch( Exception e ) {
            return Result.failure( e );
        }
    }

    public static DateTime nowUtc() {
        return DateTime.now( DateTimeZone.UTC );
    }

    public static long currentTimeHour() {
        return DateTimeUtils.currentTimeMillis() / 1000 / 60 / 60;
    }

    public static long currentTimeDay() {
        return DateTimeUtils.currentTimeMillis() / 1000 / 60 / 60 / 24;
    }

    public static String formatDateWihMillis( DateTime date ) {
        return FORMAT_MILLIS.print( date );
    }

    public static String formatDateWihMillis( long millis ) {
        return FORMAT_MILLIS.print( millis );
    }

    public static void setTimeFixed( int year, int monthOfYear, int dayOfMonth, int hourOfDay ) {
        setTimeFixed( year, monthOfYear, dayOfMonth, hourOfDay, 0, 0, 0 );
    }

    public static void setTimeFixed( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour ) {
        setTimeFixed( year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0 );
    }

    public static void setTimeFixed( int year, int monthOfYear, int dayOfMonth, int hourOfDay,
                                     int minuteOfHour, int secondOfMinute ) {
        setTimeFixed( year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, 0 );
    }

    public static void setTimeFixed( int year, int monthOfYear, int dayOfMonth, int hourOfDay,
                                     int minuteOfHour, int secondOfMinute, int millisOfSecond ) {
        setTimeFixed( new DateTime( year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            millisOfSecond ).getMillis() );
    }

    public static void setTimeFixed( long millis ) {
        DateTimeUtils.setCurrentMillisFixed( millis );
    }
}
