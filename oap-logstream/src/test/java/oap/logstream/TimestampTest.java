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

import oap.testng.AbstractTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TimestampTest extends AbstractTest {

   @Test
   public void format() throws Exception {
      assertThat( Timestamp.format( new DateTime( 2015, 12, 3, 11, 28, 30 ), 12 ) ).isEqualTo( "2015-12-03-11-05" );
   }

   @Test
   public void directoryName() throws Exception {
      assertThat( Timestamp.directoryName( "2015-12-03-11-05" ) ).isEqualTo( "2015-12/03" );
   }

//   @Test
//   public void directoryName2() throws Exception {
//      assertThat( Timestamp.directoryName2( "2015-12-03-11-05" ) ).isEqualTo( "2015-12/03" );
//   }

   @Test
   public void path() {
      DateTime date = new DateTime( 2015, 12, 3, 11, 28, 30 );
      String timestamp = Timestamp.format( date, 12 );
      assertThat( Timestamp.path( "log", date, "dir/dir/file", "log.gz", 12 ) )
         .isEqualTo( "log/dir/dir/2015-12/03/file-2015-12-03-11-05.log.gz" );
      assertThat( Timestamp.path( "log", timestamp, "dir/dir/file", "log.gz" ) )
         .isEqualTo( "log/dir/dir/2015-12/03/file-2015-12-03-11-05.log.gz" );
      assertThat( Timestamp.path( "log", timestamp, "file", "log.gz" ) )
         .isEqualTo( "log/2015-12/03/file-2015-12-03-11-05.log.gz" );
      assertThat( Timestamp.path( "*", timestamp, "dir/dir/file", "log.gz" ) )
         .isEqualTo( "*/dir/dir/2015-12/03/file-2015-12-03-11-05.log.gz" );
   }

   @Test
   public void timestamps() {
      assertThat( Timestamp.timestampsBefore( new DateTime( 2016, 2, 1, 1, 1, 1 ), 10, 12 ) )
         .containsExactly(
            "2016-02-01-00-03",
            "2016-02-01-00-04",
            "2016-02-01-00-05",
            "2016-02-01-00-06",
            "2016-02-01-00-07",
            "2016-02-01-00-08",
            "2016-02-01-00-09",
            "2016-02-01-00-10",
            "2016-02-01-00-11",
            "2016-02-01-01-00"
         );
      assertThat( Timestamp.timestampsBefore( new DateTime( 2016, 2, 1, 1, 1, 1 ), 10, 12 ) )
         .containsExactly(
            "2016-02-01-00-03",
            "2016-02-01-00-04",
            "2016-02-01-00-05",
            "2016-02-01-00-06",
            "2016-02-01-00-07",
            "2016-02-01-00-08",
            "2016-02-01-00-09",
            "2016-02-01-00-10",
            "2016-02-01-00-11",
            "2016-02-01-01-00"
         );
      assertThat( Timestamp.timestampsAfter( new DateTime( 2016, 2, 1, 1, 1, 1 ), 10, 12 ) )
         .containsExactly(
            "2016-02-01-01-00",
            "2016-02-01-01-01",
            "2016-02-01-01-02",
            "2016-02-01-01-03",
            "2016-02-01-01-04",
            "2016-02-01-01-05",
            "2016-02-01-01-06",
            "2016-02-01-01-07",
            "2016-02-01-01-08",
            "2016-02-01-01-09"
         );

      DateTimeUtils.setCurrentMillisFixed( new DateTime( 2016, 4, 21, 1, 0 ).getMillis() );

      assertThat( Timestamp.timestampsBeforeNow( new DateTime( 2016, 4, 21, 0, 1 ), 12 ) )
         .containsExactly(
            "2016-04-21-00-00",
            "2016-04-21-00-01",
            "2016-04-21-00-02",
            "2016-04-21-00-03",
            "2016-04-21-00-04",
            "2016-04-21-00-05",
            "2016-04-21-00-06",
            "2016-04-21-00-07",
            "2016-04-21-00-08",
            "2016-04-21-00-09",
            "2016-04-21-00-10",
            "2016-04-21-00-11"
         );
   }

   @Test
   public void parse() {
      DateTime[] times = {
         new DateTime( 2016, 2, 1, 1, 0 ),
         new DateTime( 2016, 2, 1, 1, 55 ),
         new DateTime( 2016, 2, 1, 1, 5 ),
         new DateTime( 2016, 2, 1, 1, 15 )
      };
      for( DateTime time : times )
         assertThat( Timestamp.parse( Timestamp.format( time, 12 ), 12 ) ).isEqualTo( time );
   }

   @Test
   public void testParseFileNameWithTimestamp() {
      final Optional<DateTime> dateTime = Timestamp.parseFileNameWithTimestamp( "/tmp/test/2016-02/01/tes-t1-2016-02-01-01-00.tsv.gz", 12 );

      assertThat( dateTime ).contains( new DateTime( 2016, 2, 1, 1, 0 ) );
   }
}
