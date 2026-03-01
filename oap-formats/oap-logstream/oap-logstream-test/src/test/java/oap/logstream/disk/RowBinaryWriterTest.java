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

import oap.compression.Compression;
import oap.logstream.LogId;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Pair;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;
import static oap.logstream.Timestamp.BPH_12;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class RowBinaryWriterTest extends Fixtures {
    private static final String FILE_PATTERN = "${p}-file-${INTERVAL}-${LOG_VERSION}.rb.gz";
    private final TestDirectoryFixture testDirectoryFixture;

    public RowBinaryWriterTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void testWrite() throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );

        byte[] content1 = Compression.gzip( RowBinaryUtils.lines( List.of(
            List.of( "s11", 21L, List.of( "1" ), new DateTime( 2022, 3, 11, 15, 16, 12, UTC ) ),
            List.of( "s12", 22L, List.of( "1", "2" ), new DateTime( 2022, 3, 11, 15, 16, 13, UTC ) )
        ) ) );

        byte[] content2 = Compression.gzip( RowBinaryUtils.lines( List.of(
            List.of( "s111", 121L, List.of( "rr" ), new DateTime( 2022, 3, 11, 15, 16, 14, UTC ) ),
            List.of( "s112", 122L, List.of( "zz", "66" ), new DateTime( 2022, 3, 11, 15, 16, 15, UTC ) )
        ) ) );


        String[] headers = new String[] { "COL1", "COL2", "COL3", "DATETIME" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id },
            new byte[] { Types.LONG.id },
            new byte[] { Types.LIST.id, Types.STRING.id },
            new byte[] { Types.DATETIME.id }
        };
        LogId logId = new LogId( "", "log", "log",
            Map.of( "p", "1" ), headers, types );
        Path logs = testDirectoryFixture.testPath( "logs" );
        try( RowBinaryWriter writer = new RowBinaryWriter( logs, FILE_PATTERN, logId, 1024, BPH_12, 20 ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, content1 );
            writer.write( CURRENT_PROTOCOL_VERSION, content2 );
        }

        Path path = logs.resolve( "1-file-02-4cd64dae-1.rb.gz.rb.gz" );

        byte[] rb = Compression.ungzip( Files.readAllBytes( path ) );

        Pair<List<List<Object>>, List<String>> read = RowBinaryUtils.read( rb, 0, rb.length, null, null );
        assertThat( read._2 ).isEqualTo( List.of( headers ) );
        assertThat( read._1 )
            .isEqualTo( List.of(
                List.of( "s11", 21L, List.of( "1" ), new DateTime( 2022, 3, 11, 15, 16, 12, UTC ) ),
                List.of( "s12", 22L, List.of( "1", "2" ), new DateTime( 2022, 3, 11, 15, 16, 13, UTC ) ),

                List.of( "s111", 121L, List.of( "rr" ), new DateTime( 2022, 3, 11, 15, 16, 14, UTC ) ),
                List.of( "s112", 122L, List.of( "zz", "66" ), new DateTime( 2022, 3, 11, 15, 16, 15, UTC ) )
            ) );
    }
}
