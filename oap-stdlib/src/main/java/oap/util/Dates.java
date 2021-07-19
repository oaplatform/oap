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

import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class Dates {
    public static final DateTimeFormatter FORMAT_MILLIS = DateTimeFormat
        .forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS" )
        .withZoneUTC();
    public static final DateTimeFormatter FORMAT_SIMPLE = DateTimeFormat
        .forPattern( "yyyy-MM-dd'T'HH:mm:ss" )
        .withZoneUTC();
    public static final DateTimeFormatter FORMAT_SIMPLE_CLEAN = DateTimeFormat
        .forPattern( "yyyy-MM-dd HH:mm:ss" )
        .withZoneUTC();
    public static final DateTimeFormatter FORMAT_DATE = DateTimeFormat
        .forPattern( "yyyy-MM-dd" )
        .withZoneUTC();
    public static final PeriodFormatter DURATION_FORMATTER = new PeriodFormatterBuilder()
        .appendWeeks().appendSuffix( new String[] { "ˆ1$", "ˆ1$", ".*" }, new String[] { "week", "weeks", "w" } ).appendSeparator( " " )
        .appendDays().appendSuffix( new String[] { "ˆ1$", "ˆ1$", ".*" }, new String[] { "day", "days", "d" } ).appendSeparator( " " )
        .appendHours().appendSuffix( new String[] { "ˆ1$", "ˆ1$", ".*" }, new String[] { "hour", "hours", "h" } ).appendSeparator( " " )
        .appendMinutes().appendSuffix( new String[] { "ˆ1$", "ˆ1$", ".*" }, new String[] { "minute", "minutes", "m" } ).appendSeparator( " " )
        .appendSeconds().appendSuffix( new String[] { "ˆ1$", "ˆ1$", ".*" }, new String[] { "second", "seconds", "s" } ).appendSeparator( " " )
        .appendMillis().appendSuffix( "ms" )
        .toFormatter();
    private static final DateTimeParser TIMEZONE_PARSER = DateTimeFormat.forPattern( "Z" ).getParser();
    private static final DateTimeParser FRACTION_PARSER =
        new DateTimeFormatterBuilder()
            .appendLiteral( '.' )
            .appendFractionOfSecond( 3, 9 )
            .appendOptional( TIMEZONE_PARSER )
            .toParser();
    public static final DateTimeFormatter PARSER_FULL = new DateTimeFormatterBuilder()
        .append( ISODateTimeFormat.date() )
        .appendLiteral( "T" )
        .append( ISODateTimeFormat.hourMinuteSecond() )
        .appendOptional( FRACTION_PARSER )
        .toFormatter()
        .withZoneUTC();

    public static Result<DateTime, Exception> parseDateWithMillis( String date ) {
        return parse( date, FORMAT_MILLIS );
    }

    public static Result<DateTime, Exception> parseDate( String date ) {
        return parse( date, FORMAT_SIMPLE );
    }

    public static Result<DateTime, Exception> parseDateWithTimeZone( String date ) {
        return parse( date, PARSER_FULL );
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

    public static DateTime nowUtcDate() {
        return nowUtc().withTime( 0, 0, 0, 0 );
    }

    @Deprecated
    public static DateTime nowUtcClean() {
        return nowUtcDate();
    }

    public static long currentTimeHour() {
        return DateTimeUtils.currentTimeMillis() / 1000 / 60 / 60;
    }

    public static long currentTimeDay() {
        return DateTimeUtils.currentTimeMillis() / 1000 / 60 / 60 / 24;
    }

    public static String formatDateWithMillis( DateTime date ) {
        return FORMAT_MILLIS.print( date );
    }

    public static String formatDateWithMillis( long millis ) {
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
            millisOfSecond, DateTimeZone.UTC ).getMillis() );
    }

    public static void setTimeFixed( long millis ) {
        DateTimeUtils.setCurrentMillisFixed( millis );
    }

    public static void incFixed( long offsetMillis ) {
        DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() + offsetMillis );
    }

    public static long s( int value ) {
        return value * 1000L;
    }

    public static long m( int value ) {
        return s( value ) * 60;
    }

    public static long h( int value ) {
        return m( value ) * 60;
    }

    public static long d( int value ) {
        return h( value ) * 24;
    }

    public static long w( int value ) {
        return d( value ) * 7;
    }

    public static String durationToString( long duration ) {
        if( duration == Long.MAX_VALUE ) return "infinity";

        var d = Duration.standardSeconds( duration / 1000 ).plus( duration % 1000 );
        return DURATION_FORMATTER.print( d.toPeriod().normalizedStandard() );
    }

    public static long stringToDuration( String periodStr ) throws IllegalArgumentException {
        if( NumberUtils.isDigits( periodStr ) ) return Long.parseLong( periodStr );

        var period = DURATION_FORMATTER.parsePeriod( periodStr );
        return period.toStandardDuration().getMillis();
    }

    public static double nanosToSeconds( long ns ) {
        return ns / 1E9;
    }
}
