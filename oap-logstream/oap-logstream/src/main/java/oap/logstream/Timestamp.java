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
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.base.AbstractInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.joda.time.DateTimeZone.UTC;

public class Timestamp implements Serializable {
    @Serial
    private static final long serialVersionUID = 2253058730098056001L;

    public static final Pattern FILE_NAME_WITH_TIMESTAMP = Pattern.compile( ".+-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2})\\..+" );
    public static final DateTimeFormatter FILE_FORMATTER = DateTimeFormat
        .forPattern( "yyyy-MM-dd-HH" )
        .withZoneUTC();
    public static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormat
        .forPattern( "yyyy-MM/dd" )
        .withZoneUTC();
    public static final char SEPARATOR_CHAR = '/';
    public static final Timestamp BPH_12 = new Timestamp( 60 / 5 );
    public static final Timestamp BPH_6 = new Timestamp( 60 / 10 );
    public static final Timestamp BPH_4 = new Timestamp( 60 / 15 );
    public static final Timestamp BPH_3 = new Timestamp( 60 / 20 );
    public static final Timestamp BPH_2 = new Timestamp( 60 / 30 );
    public static final Timestamp BPH_1 = new Timestamp( 60 / 60 );

    public final int bucketsPerHour;

    public Timestamp( int bucketsPerHour ) {
        this.bucketsPerHour = bucketsPerHour;
    }

    @Deprecated
    public static String parseTimestamp( String fileName ) {
        final Matcher matcher = FILE_NAME_WITH_TIMESTAMP.matcher( fileName );
        if( matcher.find() ) return matcher.group( 1 );
        else throw new RuntimeException( "cannot parse template's timestamp from: " + fileName );
    }

    public static String directoryName( String timestamp ) {
        final String yyyy = timestamp.substring( 0, 4 );
        final String MM = timestamp.substring( 5, 7 );
        final String dd = timestamp.substring( 8, 10 );

        return yyyy + "-" + MM + "/" + dd;
    }

    public static String path( String directory, String timestamp, String filename, String ext ) {
        String parent = FilenameUtils.getFullPathNoEndSeparator( filename );
        return ( parent.length() > 0 ? directory
            + SEPARATOR_CHAR + parent : directory )
            + SEPARATOR_CHAR + Timestamp.directoryName( timestamp )
            + SEPARATOR_CHAR + FilenameUtils.getName( filename ) + "-" + timestamp + ( ext.startsWith( "." )
            ? ext : "." + ext );
    }

    public String path( String directory, DateTime date, String filename, String ext ) {
        return path( directory, format( date ), filename, ext );
    }

    public DateTime parse( String timestamp ) {
        return FILE_FORMATTER.parseDateTime( timestamp.substring( 0, 13 ) )
            .plusMinutes( Integer.parseInt( timestamp.substring( 14, 16 ) ) * 60 / bucketsPerHour );
    }

    public Optional<DateTime> parse( Path path ) {
        final Matcher matcher = FILE_NAME_WITH_TIMESTAMP.matcher( path.getFileName().toString() );
        if( matcher.find() ) {
            final String timestamp = matcher.group( 1 );
            return Optional.of( parse( timestamp ) );
        } else return Optional.empty();
    }

    public String format( DateTime date ) {
        int bucket = currentBucket( date );
        return FILE_FORMATTER.print( date ) + "-" + ( bucket > 9 ? bucket : "0" + bucket );
    }

    public int currentBucket( DateTime date ) {
        return ( int ) Math.floor( date.getMinuteOfHour() / ( 60d / bucketsPerHour ) );
    }

    public long currentBucketStartMillis() {
        DateTime date = DateTime.now();
        return date.withMinuteOfHour( 60 / bucketsPerHour * currentBucket( date ) )
            .withSecondOfMinute( 0 )
            .withMillisOfSecond( 0 )
            .getMillis();
    }

    public Stream<String> timestampsBeforeNow( int back ) {
        return timestampsBefore( DateTime.now(), back );
    }

    public Stream<String> timestampsBeforeNow( DateTime since ) {
        return Stream.of( since, AbstractInstant::isBeforeNow, t -> t.plusMinutes( 60 / bucketsPerHour ) )
            .map( this::format );
    }

    public Stream<String> timestampsBefore( DateTime since, int back ) {
        return Stream.of( IntStream.rangeClosed( 1, back )
            .mapToObj( b -> format( since.minusMinutes( ( back - b ) * 60 / bucketsPerHour ) ) )
        );
    }

    public Stream<String> timestampsAfter( DateTime since, int fore ) {
        return Stream.of( IntStream.range( 0, fore )
            .mapToObj( b -> format( since.plusMinutes( b * 60 / bucketsPerHour ) ) )
        );
    }

    public DateTime toStartOfBucket( DateTime dateTime ) {
        long ms = 60L * 1000 / bucketsPerHour;

        return new DateTime( dateTime.getMillis() / ms * ms, UTC );
    }
}
