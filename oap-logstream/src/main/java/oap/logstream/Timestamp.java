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
package oap.logstream;

import oap.util.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class Timestamp {
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern( "yyyy-MM-dd-HH" ).withZoneUTC();
    private static final DateTimeFormatter directoryFormatter = DateTimeFormat.forPattern( "yyyy-MM/dd" ).withZoneUTC();

    public static String formatDate( DateTime date, int bucketsPerHour ) {
        int bucket = currentBucket( date, bucketsPerHour );
        return formatter.print( date ) + "-" + ( bucket > 9 ? bucket : "0" + bucket );
    }

    private static int currentBucket( DateTime date, int bucketsPerHour ) {
        return ( int ) Math.floor( date.getMinuteOfHour() / ( 60d / bucketsPerHour ) );
    }

    public static long currentBucketStartMillis( int bucketsPerHour ) {
        DateTime date = DateTime.now();
        return date.withMinuteOfHour( 60 / bucketsPerHour * currentBucket( date, bucketsPerHour ) )
            .withSecondOfMinute( 0 )
            .withMillisOfSecond( 0 )
            .getMillis();
    }

    public static String directoryName( String timestamp ) {
        return directoryFormatter.print( formatter.parseDateTime( timestamp.substring( 0, 12 ) ) );
    }

    public static Stream<String> timestamps( int back, int bucketsPerHour ) {
        DateTime now = DateTime.now();
        return Stream.of( back, b -> b >= 0, b -> b - 1 )
            .map( b -> formatDate( now.minusMinutes( b * 60 / bucketsPerHour ), bucketsPerHour ) );
    }
}
