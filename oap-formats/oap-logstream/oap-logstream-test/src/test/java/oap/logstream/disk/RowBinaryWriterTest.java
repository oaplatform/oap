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
import oap.io.IoStreams;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LoggerException;
import oap.logstream.formats.RowBinaryAssertion;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.TemplateEngineFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Pair;
import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;
import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.formats.RowBinaryAssertion.assertRowBinaryFile;
import static oap.logstream.formats.RowBinaryAssertion.header;
import static oap.logstream.formats.RowBinaryAssertion.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joda.time.DateTimeZone.UTC;

public class RowBinaryWriterTest extends Fixtures {
    private static final String FILE_PATTERN = "{{ p }}-file-{{ INTERVAL }}-{{ LOG_VERSION }}.rb.gz";
    private final TestDirectoryFixture testDirectoryFixture;
    private final TemplateEngineFixture templateEngineFixture;

    public RowBinaryWriterTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        templateEngineFixture = fixture( new TemplateEngineFixture() );
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
        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, logs, FILE_PATTERN, logId, 1024, BPH_12, 20, "localhost", null ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, content1 );
            writer.write( CURRENT_PROTOCOL_VERSION, content2 );
        }

        Path path = logs.resolve( "1-file-02-4cd64dae0-1.rb.gz.rb.gz" );

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

    @Test
    public void testConcurrency() throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );

        String[] headers = new String[] { "COL1", "COL2", "COL3", "DATETIME" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id },
            new byte[] { Types.LONG.id },
            new byte[] { Types.LIST.id, Types.STRING.id },
            new byte[] { Types.DATETIME.id }
        };
        LogId logId = new LogId( "", "log", "log", Map.of( "p", "1" ), headers, types );
        Path logs = testDirectoryFixture.testPath( "logs" );

        int count = 10;

        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, logs, FILE_PATTERN, logId, 1024, BPH_12, 20, "localhost", null ) ) {
            try( ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor() ) {
                for( long i = 0; i < count; i++ ) {

                    byte[] content = Compression.gzip( RowBinaryUtils.lines( List.of(
                        List.of( "s11", i, List.of( "1" ), new DateTime( 2022, 3, 11, 15, 16, 12, UTC ) )
                    ) ) );

                    executorService.execute( () -> {
                        writer.write( CURRENT_PROTOCOL_VERSION, content );
                    } );
                }
            }
        }

        Path path = logs.resolve( "1-file-02-4cd64dae0-1.rb.gz.rb.gz" );

        RowBinaryAssertion.Row[] rows = new RowBinaryAssertion.Row[count];
        for( long i = 0; i < count; i++ ) {
            rows[( int ) i] = row( i );
        }

        assertRowBinaryFile( path, IoStreams.Encoding.GZIP )
            .containsExactlyInAnyOrderEntriesOf(
                header( "COL2" ),
                rows
            );
    }

    @Test
    public void testWriteToNewVersionWhenCompleted() throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );

        String[] headers = new String[] { "COL1" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id } };
        LogId logId = new LogId( "", "log", "log", Map.of( "p", "1" ), headers, types );
        Path logs = testDirectoryFixture.testPath( "logs" );

        byte[] content1 = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "row1" ) ) ) );
        byte[] content2 = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "row2" ) ) ) );

        Path v1 = logs.resolve( "1-file-02-47b82ddc0-1.rb.gz.rb.gz" );
        Path v2 = logs.resolve( "1-file-02-47b82ddc0-2.rb.gz.rb.gz" );

        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, logs, FILE_PATTERN, logId, 1024, BPH_12, 20, "localhost", null ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, content1 );

            writer.refresh();
            assertThat( v1 ).exists();
            assertThat( new LogFile( v1 ).isCompleted() ).isFalse();

            // refresh(true) forces closeOutput() — closes v1 and marks it completed via readyForUpload()
            writer.refresh( true );
            assertThat( v1 ).exists();
            assertThat( new LogFile( v1 ).isCompleted() ).isTrue();

            // v1 is completed — next write must go to v2
            writer.write( CURRENT_PROTOCOL_VERSION, content2 );
            assertThat( v2 ).exists();
            assertThat( new LogFile( v2 ).isCompleted() ).isFalse();
        }

        byte[] rb = Compression.ungzip( Files.readAllBytes( v2 ) );
        Pair<List<List<Object>>, List<String>> read = RowBinaryUtils.read( rb, 0, rb.length, null, null );
        assertThat( read._1 ).contains( List.of( "row2" ) );
    }

    @DataProvider
    public Object[][] compressions() {
        return Arrays.stream( LogStreamCompression.values() ).map( c -> new Object[] { c } ).toArray( Object[][]::new );
    }

    private static LogId logId() {
        return new LogId( "", "log", "log", Map.of( "p", "1" ), new String[] { "COL1", "COL2" },
            new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.LONG.id } } );
    }

    /**
     * the header and the first buffer are written with one append; the file is a valid stream of the codec
     */
    @Test( dataProvider = "compressions" )
    @SuppressWarnings( "deprecation" )
    public void writeAllCodecs( LogStreamCompression compression ) throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );

        byte[] content1 = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "a", 1L ), List.of( "b", 2L ) ) ) );
        byte[] content2 = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "c", 3L ) ) ) );

        Path logs = testDirectoryFixture.testPath( "logs" );
        String file;
        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, compression, logs, FILE_PATTERN, logId(), 1024, BPH_12, 20, "localhost", null ) ) {
            file = writer.write( CURRENT_PROTOCOL_VERSION, content1, 0, content1.length );
            assertThat( writer.write( CURRENT_PROTOCOL_VERSION, content2, 0, content2.length ) ).isEqualTo( file );
        }

        Path path = Path.of( file );
        assertThat( path.getFileName().toString() ).endsWith( "." + compression.logFormat.extension );

        LogFile logFile = new LogFile( path );
        assertThat( logFile.isCompleted() ).isTrue();
        assertThat( logFile.getTransactionFile() ).doesNotExist();
        assertThat( logFile.getLogMetadata().getString( "VERSION" ) ).endsWith( "-1" );

        byte[] rb = Frames.decompress( compression, Files.readAllBytes( path ) );
        Pair<List<List<Object>>, List<String>> read = RowBinaryUtils.read( rb, 0, rb.length, null, null );
        assertThat( read._2 ).containsExactly( "COL1", "COL2" );
        assertThat( read._1 ).containsExactly( List.of( "a", 1L ), List.of( "b", 2L ), List.of( "c", 3L ) );
    }

    /**
     * a failed append leaves the file at the last complete frame; the file is closed (completed) and the next buffer goes to the next version
     */
    @Test
    public void writeErrorGoesToNextVersion() throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );
        LogStreamCompression compression = LogStreamCompression.ZSTD;

        byte[] content1 = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "a", 1L ) ) ) );
        byte[] content2 = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "b", 2L ) ) ) );
        byte[] content3 = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "c", 3L ) ) ) );

        Path logs = testDirectoryFixture.testPath( "logs" );
        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, compression, logs, FILE_PATTERN, logId(), 1024, BPH_12, 20, "localhost", null ) ) {
            Path v1 = Path.of( writer.write( CURRENT_PROTOCOL_VERSION, content1, 0, content1.length ) );

            writer.logFile.out.close();
            assertThatThrownBy( () -> writer.write( CURRENT_PROTOCOL_VERSION, content2, 0, content2.length ) ).isInstanceOf( LoggerException.class );

            assertThat( new LogFile( v1 ).isCompleted() ).isTrue();
            byte[] rb = Frames.decompress( compression, Files.readAllBytes( v1 ) );
            assertThat( RowBinaryUtils.read( rb, 0, rb.length, null, null )._1 ).containsExactly( List.of( "a", 1L ) );

            Path v2 = Path.of( writer.write( CURRENT_PROTOCOL_VERSION, content3, 0, content3.length ) );
            assertThat( v2 ).isNotEqualTo( v1 );
            assertThat( new LogFile( v2 ).getLogMetadata().getString( "VERSION" ) ).endsWith( "-2" );
        }
    }

    @Test
    public void syncMode() throws IOException {
        Dates.setTimeFixed( 2022, 3, 8, 21, 11 );
        LogStreamCompression compression = LogStreamCompression.ZSTD;
        byte[] content = Frames.frame( compression, RowBinaryUtils.lines( List.of( List.of( "a", 1L ) ) ) );

        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, compression, SyncMode.PERIODIC,
            testDirectoryFixture.testPath( "periodic" ), FILE_PATTERN, logId(), 1024, BPH_12, 20, "localhost", null ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, content, 0, content.length );
            assertThat( writer.logFile.hasUnsyncedData() ).isTrue();

            writer.sync();
            assertThat( writer.logFile.hasUnsyncedData() ).isFalse();

            writer.write( CURRENT_PROTOCOL_VERSION, content, 0, content.length );
            assertThat( writer.logFile.hasUnsyncedData() ).isTrue();
        }

        try( RowBinaryWriter writer = new RowBinaryWriter( templateEngineFixture.templateEngine, compression, SyncMode.ALWAYS,
            testDirectoryFixture.testPath( "always" ), FILE_PATTERN, logId(), 1024, BPH_12, 20, "localhost", null ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, content, 0, content.length );
            assertThat( writer.logFile.hasUnsyncedData() ).isFalse();
        }
    }
}
