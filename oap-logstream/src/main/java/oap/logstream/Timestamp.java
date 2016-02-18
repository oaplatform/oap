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

import oap.io.Files;
import oap.util.Stream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.file.Path;

public class Timestamp {
    public static final DateTimeFormatter FILE_FORMATTER = DateTimeFormat.forPattern( "yyyy-MM-dd-HH" ).withZoneUTC();
    public static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormat.forPattern( "yyyy-MM/dd" ).withZoneUTC();

    public static String format( DateTime date, int bucketsPerHour ) {
        int bucket = currentBucket( date, bucketsPerHour );
        return FILE_FORMATTER.print( date ) + "-" + ( bucket > 9 ? bucket : "0" + bucket );
    }

    public static DateTime parse( String timestamp, int bucketsPerHour ) {
        return FILE_FORMATTER.parseDateTime( timestamp.substring( 0, 13 ) )
            .plusMinutes( Integer.parseInt( timestamp.substring( 14, 16 ) ) * 60 / bucketsPerHour );
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
        return DIRECTORY_FORMATTER.print( FILE_FORMATTER.parseDateTime( timestamp.substring( 0, 13 ) ) );
    }

    public static Stream<String> timestamps( int back, int bucketsPerHour ) {
        return timestamps( DateTime.now(), back, bucketsPerHour );
    }

    public static Stream<String> timestamps( DateTime since, int back, int bucketsPerHour ) {
        return Stream.of( back, b -> b >= 0, b -> b - 1 )
            .map( b -> format( since.minusMinutes( b * 60 / bucketsPerHour ), bucketsPerHour ) );
    }

    public static Path path( Path directory, String pattern, String filename, String ext ) {
        Path path = Files.path( filename );
        return ( path.getParent() != null ? directory.resolve( path.getParent() ) : directory )
            .resolve( Timestamp.directoryName( pattern ) )
            .resolve( path.getFileName() + "-" + pattern + "." + ext );
    }
}
