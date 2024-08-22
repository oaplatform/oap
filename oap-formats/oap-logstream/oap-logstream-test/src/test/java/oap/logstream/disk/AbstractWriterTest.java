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

package oap.logstream.disk;

import oap.logstream.LogId;
import oap.logstream.Timestamp;
import oap.logstream.disk.DiskLoggerBackend.ValidationWriter;
import oap.template.Types;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractWriterTest {
    @Test
    public void testFileName() {
        var h1Headers = new String[] { "h1" };
        var strTypes = new byte[][] { new byte[] { Types.STRING.id } };

        var lid1 = new LogId( "ln", "lt", "chn", Map.of(), h1Headers, strTypes );

        Dates.setTimeFixed( 2023, 1, 23, 21, 6, 0 );

        assertThat( ValidationWriter.INSTANCE.currentPattern( "<INTERVAL>  -<LOG_VERSION>-<if(ORGANIZATION)><ORGANIZATION><else>UNKNOWN<endif><EXT>", lid1, Timestamp.BPH_12, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/01-85594397-1-UNKNOWN.validation.gz" );

        assertThat( ValidationWriter.INSTANCE.currentPattern( "<INTERVAL>-<LOG_VERSION>-<ORGANIZATION><EXT>", lid1, Timestamp.BPH_12, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/01-85594397-1-.validation.gz" );
        assertThat( ValidationWriter.INSTANCE.currentPattern( "${INTERVAL}-${LOG_VERSION}-${ORGANIZATION}<EXT>", lid1, Timestamp.BPH_12, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/01-85594397-1-.validation.gz" );

        assertThat( ValidationWriter.INSTANCE.currentPattern( "${LOG_TIME_INTERVAL}.log.gz", lid1, Timestamp.BPH_6, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/10.log.gz" );
    }

    @Test
    public void testFileNameConditional() {
        var h1Headers = new String[] { "h1" };
        var strTypes = new byte[][] { new byte[] { Types.STRING.id } };

        Dates.setTimeFixed( 2023, 1, 23, 21, 6, 0 );

        var lid1 = new LogId( "ln", "lt", "chn", Map.of(), h1Headers, strTypes );
        assertThat( ValidationWriter.INSTANCE.currentPattern( "<if((ORGANIZATION)&&(ACCOUNT))><ORGANIZATION>/<ACCOUNT>/<endif><INTERVAL>-<LOG_VERSION><EXT>", lid1, Timestamp.BPH_12, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/01-85594397-1.validation.gz" );

        lid1 = new LogId( "ln", "lt", "chn", Map.of( "ORGANIZATION", "org1", "ACCOUNT", "acc1" ), h1Headers, strTypes );
        assertThat( ValidationWriter.INSTANCE.currentPattern( "<if((ORGANIZATION)&&(ACCOUNT))><ORGANIZATION>/<ACCOUNT>/<endif><INTERVAL>-<LOG_VERSION><EXT>", lid1, Timestamp.BPH_12, 1, Dates.nowUtc() ) )
            .isEqualTo( "ln/org1/acc1/01-85594397-1.validation.gz" );
    }
}
