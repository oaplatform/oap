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
import oap.logstream.Logger;
import oap.logstream.Timestamp;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.TemplateEngineFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Result;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.logstream.formats.RowBinaryAssertion.assertRowBinaryFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DiskLoggerBackendTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;
    private final TemplateEngineFixture templateEngineFixture;

    public DiskLoggerBackendTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        templateEngineFixture = fixture( new TemplateEngineFixture() );
    }

    @Test
    public void spaceAvailable() {
        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), Timestamp.BPH_12, 4000, "localhost" ) ) {
            backend.start();

            assertTrue( backend.isLoggingAvailable() );
            backend.requiredFreeSpace *= 1000;
            assertFalse( backend.isLoggingAvailable() );
            backend.requiredFreeSpace /= 1000;
            assertTrue( backend.isLoggingAvailable() );
        }
    }

    @Test
    public void testPatternByType() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 16 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );

        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), Timestamp.BPH_12, 4000, "localhost" ) ) {
            backend.filePattern = "{{ LOG_TYPE }}_{{ LOG_VERSION }}_{{ INTERVAL }}.tsv.gz";
            backend.filePatternByType.put( "LOG_TYPE_WITH_DIFFERENT_FILE_PATTERN",
                new DiskLoggerBackend.FilePatternConfiguration( "{{ LOG_TYPE }}_{{ LOG_VERSION }}_{{ MINUTE }}.parquet" ) );
            backend.start();

            Logger logger = new Logger( backend );
            //log a line to lfn1
            logger.log( "lfn1", Map.of(), "log_type_with_default_file_pattern", headers, types, lines );
            logger.log( "lfn1", Map.of(), "log_type_with_different_file_pattern", headers, types, lines );

            backend.refresh( true );

            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/log_type_with_default_file_pattern_59193f7e0-1_03.tsv.gz.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/log_type_with_different_file_pattern_59193f7e0-1_16.parquet.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
        }
    }

    @Test
    public void testWriteSync() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );
        //init new logger
        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            backend.start();

            Logger logger = new Logger( backend );
            //log a line to lfn1
            Result<String, LogstreamException> result = logger.log( "lfn1", Map.of(), "log", headers, types, lines );

            assertThat( result.isSuccess() ).isTrue();
            assertThat( result.successValue )
                .isEqualTo( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz.rb.gz" ).toString() );

            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v59193f7e0-1-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf(
                    List.of( "12345678", "rrrr5678" ),
                    List.of( "1", "2" )
                );
        }
    }

    /**
     * the state after a crash: the file is written (and synced), .metadata.completed is missing, a torn buffer at the end
     *
     * @return the data file and its size before the torn buffer
     */
    private Path crashedFile( byte[] lines, byte[] torn, String[] headers, byte[][] types ) throws IOException {
        Path file;
        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            Logger logger = new Logger( backend );
            file = Path.of( logger.log( "lfn1", Map.of(), "log", headers, types, lines ).successValue );
        }

        Files.delete( new LogFile( file ).getCompletedFile() );
        Files.write( file, torn, StandardOpenOption.APPEND );
        return file;
    }

    @Test
    public void recoveryOnStart() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );

        Path file = crashedFile( lines, java.util.Arrays.copyOf( lines, lines.length / 2 ), headers, types );
        long sizeBeforeTorn = Files.size( file ) - lines.length / 2;

        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            CopyOnWriteArrayList<Path> closed = new CopyOnWriteArrayList<>();
            backend.addNotificationListener( closed::add );
            backend.start();

            assertThat( closed ).containsExactly( file );
            assertThat( Files.size( file ) ).isEqualTo( sizeBeforeTorn );
            assertThat( new LogFile( file ).isCompleted() ).isTrue();
            assertRowBinaryFile( file, GZIP ).containsExactlyInAnyOrderEntriesOf(
                List.of( "12345678", "rrrr5678" ),
                List.of( "1", "2" )
            );

            Logger logger = new Logger( backend );
            Path next = Path.of( logger.log( "lfn1", Map.of(), "log", headers, types, lines ).successValue );
            assertThat( next ).isNotEqualTo( file );
            assertThat( next.getFileName().toString() ).contains( "-2-2015" );
        }
    }

    /**
     * a file of the previous version (with .metadata.transaction) left by a crash: truncated to the committed offset,
     * completed; the new data goes to the next version
     */
    @Test
    @SuppressWarnings( "deprecation" )
    public void recoveryOfLegacyFileOnStart() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "12345678", "rrrr5678" ), List.of( "1", "2" ) ) ) );
        byte[] uncommitted = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "not", "committed" ) ) ) );

        // a complete frame after the committed offset: written, but not committed
        Path file = crashedFile( lines, uncommitted, headers, types );
        long committed = Files.size( file ) - uncommitted.length;
        LogFile logFile = new LogFile( file );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( committed ) );

        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            backend.start();

            assertThat( Files.size( file ) ).isEqualTo( committed );
            assertThat( logFile.isCompleted() ).isTrue();
            assertThat( logFile.getTransactionFile() ).exists();
            assertRowBinaryFile( file, GZIP ).containsExactlyInAnyOrderEntriesOf(
                List.of( "12345678", "rrrr5678" ),
                List.of( "1", "2" )
            );

            Logger logger = new Logger( backend );
            Path next = Path.of( logger.log( "lfn1", Map.of(), "log", headers, types, lines ).successValue );
            assertThat( next ).isNotEqualTo( file );
            assertThat( Files.size( file ) ).isEqualTo( committed );
        }
    }

    @Test
    public void periodicSync() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );
        String[] headers = new String[] { "REQUEST_ID" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id } };
        byte[] lines = Compression.gzip( RowBinaryUtils.lines( List.of( List.of( "1" ) ) ) );

        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            backend.syncMode = SyncMode.PERIODIC;
            backend.syncInterval = Dates.h( 1 );
            backend.start();

            new Logger( backend ).log( "lfn1", Map.of(), "log", headers, types, lines );

            AbstractWriter writer = backend.writers.asMap().values().iterator().next();
            assertThat( writer.logFile.hasUnsyncedData() ).isTrue();

            backend.sync();
            assertThat( writer.logFile.hasUnsyncedData() ).isFalse();
        }
    }
}
