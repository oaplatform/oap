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

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class TimestampTest {

    @Test
    public void format() {
        assertThat( Timestamp.BPH_12.format( new DateTime( 2015, 12, 3, 11, 28, 30, UTC ) ) )
            .isEqualTo( "2015-12-03-11-05" );
        assertThat( Timestamp.BPH_1.format( new DateTime( 2015, 12, 3, 11, 28, 30, UTC ) ) )
            .isEqualTo( "2015-12-03-11-00" );
    }

    @Test
    public void directoryName() {
        assertThat( Timestamp.directoryName( "2015-12-03-11-05" ) ).isEqualTo( "2015-12/03" );
    }

//   @Test
//   public void directoryName2() throws Exception {
//      assertThat( Timestamp.directoryName2( "2015-12-03-11-05" ) ).isEqualTo( "2015-12/03" );
//   }

    @Test
    public void path() {
        DateTime date = new DateTime( 2015, 12, 3, 11, 28, 30, UTC );
        String timestamp = Timestamp.BPH_12.format( date );
        assertThat( Timestamp.BPH_12.path( "log", date, "dir/dir/file", "log.gz" ) )
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
        assertThat( Timestamp.BPH_12.timestampsBefore( new DateTime( 2016, 2, 1, 1, 1, 1, UTC ), 10 ) )
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
        assertThat( Timestamp.BPH_12.timestampsBefore( new DateTime( 2016, 2, 1, 1, 1, 1, UTC ), 10 ) )
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
        assertThat( Timestamp.BPH_12.timestampsAfter( new DateTime( 2016, 2, 1, 1, 1, 1, UTC ), 10 ) )
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

        DateTimeUtils.setCurrentMillisFixed( new DateTime( 2016, 4, 21, 1, 0, UTC ).getMillis() );

        assertThat( Timestamp.BPH_12.timestampsBeforeNow( new DateTime( 2016, 4, 21, 0, 1, UTC ) ) )
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
            new DateTime( 2016, 2, 1, 1, 0, UTC ),
            new DateTime( 2016, 2, 1, 1, 55, UTC ),
            new DateTime( 2016, 2, 1, 1, 5, UTC ),
            new DateTime( 2016, 2, 1, 1, 15, UTC )
        };
        for( DateTime time : times )
            assertThat( Timestamp.BPH_12.parse( Timestamp.BPH_12.format( time ) ) ).isEqualTo( time );
    }

    @Test
    public void parsePath() {
        assertThat( Timestamp.BPH_12.parse( Paths.get( "/tmp/test/2016-02/01/tes-t1-2016-02-01-01-00.tsv.gz" ) ) )
            .contains( new DateTime( 2016, 2, 1, 1, 0, UTC ) );
    }
}
