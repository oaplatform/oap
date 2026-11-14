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

import lombok.extern.slf4j.Slf4j;
import oap.compression.Compression;
import oap.http.server.nio.NioHttpServer;
import oap.logstream.disk.DiskLoggerBackend;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.logstream.net.BufferConfigurationMap;
import oap.logstream.net.client.SocketLoggerBackend;
import oap.logstream.net.server.SocketLoggerServer;
import oap.message.client.MessageSender;
import oap.message.server.MessageHttpHandler;
import oap.template.TemplateEngineFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.ZSTD;
import static oap.logstream.AvailabilityReport.State.FAILED;
import static oap.logstream.Timestamp.BPH_12;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_FREE_SPACE_REQUIRED;
import static oap.logstream.formats.RowBinaryAssertion.assertRowBinaryFile;
import static oap.net.Inet.HOSTNAME;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertFile;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Slf4j
public class LoggerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;
    private final TemplateEngineFixture templateEngineFixture;

    public LoggerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        templateEngineFixture = fixture( new TemplateEngineFixture() );
    }

    @Test
    public void disk() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1 );

        List<Object> lineData1 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678", "12345678" );
        byte[] line1 = Compression.gzip( RowBinaryUtils.line( lineData1 ) );
        String[] headers1 = new String[] { "TIMESTAMP", "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types1 = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        List<Object> lineData2 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678" );
        byte[] line2 = Compression.gzip( RowBinaryUtils.line( lineData2 ) );
        String[] headers2 = new String[] { "TIMESTAMP", "REQUEST_ID2" };
        byte[][] types2 = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id } };
        try( DiskLoggerBackend backend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" ) ) {
            Logger logger = new Logger( backend );
            logger.log( "lfn1", Map.of(), "log", headers1, types1, line1 );
            logger.log( "lfn2", Map.of(), "log", headers1, types1, line1 );
            logger.log( "lfn1", Map.of(), "log", headers1, types1, line1 );
            logger.log( "lfn1", Map.of(), "log2", headers2, types2, line2 );

            logger.log( "lfn1", Map.of(), "log", headers2, types2, line2 );
        }

        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
            .containsExactlyInAnyOrderEntriesOf( lineData1, lineData1 );
        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn2/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
            .containsExactlyInAnyOrderEntriesOf( lineData1 );
        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log2_v8a769cda0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
            .containsExactlyInAnyOrderEntriesOf( lineData2 );
        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v8a769cda0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.gz" ), GZIP )
            .containsExactlyInAnyOrderEntriesOf( lineData2 );
    }

    @Test
    public void net() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        List<Object> lineData1 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678", "12345678" );
        byte[] line1 = RowBinaryUtils.line( lineData1 );
        String[] headers1 = new String[] { "TIMESTAMP", "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types1 = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };
        List<Object> lineData2 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678" );
        byte[] line2 = RowBinaryUtils.line( lineData2 );
        String[] headers2 = new String[] { "TIMESTAMP", "REQUEST_ID2" };
        byte[][] types2 = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id } };

        try( DiskLoggerBackend serverBackend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" );
             SocketLoggerServer server = new SocketLoggerServer( serverBackend );
             NioHttpServer mServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( mServer, "/messages", controlStatePath, List.of( server ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 );
             SocketLoggerBackend clientBackend = new SocketLoggerBackend( client, 256, -1 ) ) {

            mServer.start();
            messageHttpHandler.preStart();
            client.start();

            serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED * 10000L;
            assertFalse( serverBackend.isLoggingAvailable() );
            Logger logger = new Logger( clientBackend );
            logger.log( "lfn1", Map.of(), "log", headers1, types1, line1 );
            logger.log( "lfn2", Map.of(), "log", headers1, types1, line1 );
            clientBackend.sendAsync();
            client.syncMemory();
            assertEventually( 50, 100, () -> assertFalse( logger.isLoggingAvailable() ) );

            assertFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.zst" ) )
                .doesNotExist();

            serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;

            log.debug( "add disk space" );

            Dates.incFixed( 2000 );

            assertEventually( 50, 100, () -> {
                client.syncMemory();
                assertTrue( logger.isLoggingAvailable() );
            } );
            logger.log( "lfn1", Map.of(), "log", headers1, types1, line1 );
            clientBackend.sendAsync();
            client.syncMemory();

            assertTrue( logger.isLoggingAvailable() );
            logger.log( "lfn1", Map.of(), "log2", headers2, types2, line2 );
            clientBackend.sendAsync();
            client.syncMemory();
        }

        assertEventually( 10, 1000, () ->
            assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.zst" ), ZSTD )
                .containsExactlyInAnyOrderEntriesOf( lineData1, lineData1 ) );
        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn2/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.zst" ), ZSTD )
            .containsExactlyInAnyOrderEntriesOf( lineData1 );
        assertRowBinaryFile( testDirectoryFixture.testPath( "logs/localhost/lfn1/2015-10/10/log2_v8a769cda0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz.rb.zst" ), ZSTD )
            .containsExactlyInAnyOrderEntriesOf( lineData2 );
    }

    @Test
    public void netCompressionRuntimeSwitch() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        List<Object> lineData1 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678", "12345678" );
        byte[] line1 = RowBinaryUtils.line( lineData1 );
        List<Object> lineData2 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "abc", "def" );
        byte[] line2 = RowBinaryUtils.line( lineData2 );
        String[] headers = new String[] { "TIMESTAMP", "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };

        try( DiskLoggerBackend serverBackend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" );
             SocketLoggerServer server = new SocketLoggerServer( serverBackend );
             NioHttpServer mServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( mServer, "/messages", controlStatePath, List.of( server ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 );
             SocketLoggerBackend clientBackend = new SocketLoggerBackend( client, 256, -1 ) ) {

            mServer.start();
            messageHttpHandler.preStart();
            client.start();

            Logger logger = new Logger( clientBackend );

            assertThat( clientBackend.getCompression() ).isEqualTo( LogStreamCompression.ZSTD );
            assertThat( clientBackend.getZstdLevel() ).isEqualTo( 1 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line1 );
            clientBackend.sendAsync();
            client.syncMemory();

            clientBackend.setCompression( LogStreamCompression.LZ4 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line1 );
            clientBackend.sendAsync();
            client.syncMemory();
            clientBackend.setLz4Level( 9 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line2 );
            clientBackend.sendAsync();
            client.syncMemory();

            clientBackend.setCompression( LogStreamCompression.ZSTD );
            clientBackend.setZstdLevel( 3 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line2 );
            clientBackend.sendAsync();
            client.syncMemory();
            clientBackend.setZstdLevel( -5 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line1 );
            clientBackend.sendAsync();
            client.syncMemory();

            clientBackend.setCompression( LogStreamCompression.GZIP );
            clientBackend.setGzipLevel( 9 );
            logger.log( "lfn1", Map.of(), "log", headers, types, line2 );
            clientBackend.sendAsync();
            client.syncMemory();

            assertThatThrownBy( () -> clientBackend.setGzipLevel( 10 ) ).isInstanceOf( IllegalArgumentException.class );
            assertThatThrownBy( () -> clientBackend.setLz4Level( -1 ) ).isInstanceOf( IllegalArgumentException.class );
            assertThatThrownBy( () -> clientBackend.setZstdLevel( 23 ) ).isInstanceOf( IllegalArgumentException.class );
            assertThat( clientBackend.getGzipLevel() ).isEqualTo( 9 );
        }

        String prefix = "logs/localhost/lfn1/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz";
        assertEventually( 10, 1000, () ->
            assertRowBinaryFile( testDirectoryFixture.testPath( prefix + ".rb.gz" ), GZIP )
                .containsExactlyInAnyOrderEntriesOf( lineData2 ) );
        assertRowBinaryFile( testDirectoryFixture.testPath( prefix + ".rb.lz4" ), LZ4 )
            .containsExactlyInAnyOrderEntriesOf( lineData1, lineData2 );
        assertRowBinaryFile( testDirectoryFixture.testPath( prefix + ".rb.zst" ), ZSTD )
            .containsExactlyInAnyOrderEntriesOf( lineData1, lineData2, lineData1 );
    }

    @Test
    public void netRuntimeParameters() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        List<Object> lineData1 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "12345678", "12345678" );
        byte[] line1 = RowBinaryUtils.line( lineData1 );
        List<Object> lineData2 = List.of( new DateTime( 2015, 10, 10, 1, 0, UTC ), "abc", "def" );
        byte[] line2 = RowBinaryUtils.line( lineData2 );
        String[] headers = new String[] { "TIMESTAMP", "REQUEST_ID", "REQUEST_ID2" };
        byte[][] types = new byte[][] { new byte[] { Types.DATETIME.id }, new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };

        BufferConfigurationMap configurations = BufferConfigurationMap.custom(
            __( "LOG", new BufferConfigurationMap.BufferConfiguration( 256, Pattern.compile( "^log$" ) ) ),
            __( "OTHER", new BufferConfigurationMap.BufferConfiguration( 512, Pattern.compile( ".*" ) ) ) );

        try( DiskLoggerBackend serverBackend = new DiskLoggerBackend( templateEngineFixture.templateEngine, testDirectoryFixture.testPath( "logs" ), BPH_12, DEFAULT_BUFFER, "localhost" );
             SocketLoggerServer server = new SocketLoggerServer( serverBackend );
             NioHttpServer mServer = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( mServer, "/messages", controlStatePath, List.of( server ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 );
             SocketLoggerBackend clientBackend = new SocketLoggerBackend( client, configurations, -1 ) ) {

            mServer.start();
            messageHttpHandler.preStart();
            client.start();

            Logger logger = new Logger( clientBackend );

            assertThat( clientBackend.getFlushInterval() ).isEqualTo( -1L );
            assertThat( clientBackend.getMaxBuffers() ).isEqualTo( 5000 );
            assertThat( clientBackend.getBufferSizes() ).containsOnly( Map.entry( "LOG", 256 ), Map.entry( "OTHER", 512 ) );

            // bufferSize: the current buffer (256) is sent, the next one is 1024
            logger.log( "lfn1", Map.of(), "log", headers, types, line1 );
            clientBackend.setBufferSize( "LOG", 1024 );
            assertThat( clientBackend.getBufferSizes() ).containsOnly( Map.entry( "LOG", 1024 ), Map.entry( "OTHER", 512 ) );
            for( int i = 0; i < 20; i++ ) logger.log( "lfn1", Map.of(), "log", headers, types, line2 );
            clientBackend.sendAsync();
            client.syncMemory();

            // maxBuffers: 100 lines do not fit into one 1024-byte buffer - full buffers are ready (not sent yet)
            for( int i = 0; i < 100; i++ ) logger.log( "lfn1", Map.of(), "log", headers, types, line1 );
            clientBackend.setMaxBuffers( 1 );
            AvailabilityReport report = clientBackend.availabilityReport();
            assertThat( report.state ).isEqualTo( FAILED );
            assertThat( report.subsystemStates ).containsEntry( SocketLoggerBackend.FAILURE_BUFFERS_STATE, FAILED );
            clientBackend.setMaxBuffers( 5000 );
            assertThat( clientBackend.availabilityReport().subsystemStates ).doesNotContainEntry( SocketLoggerBackend.FAILURE_BUFFERS_STATE, FAILED );

            // flushInterval: sent without sendAsync()
            clientBackend.setFlushInterval( 100 );
            assertThat( clientBackend.getFlushInterval() ).isEqualTo( 100L );

            String prefix = "logs/localhost/lfn1/2015-10/10/log_v356dae4c0-1_" + HOSTNAME + "-2015-10-10-01-00.tsv.gz";
            List<Object>[] expected = expectedLines( lineData1, lineData2 );
            assertEventually( 100, 100, () -> {
                client.syncMemory();
                assertRowBinaryFile( testDirectoryFixture.testPath( prefix + ".rb.zst" ), ZSTD )
                    .containsExactlyInAnyOrderEntriesOf( expected );
            } );

            clientBackend.setFlushInterval( -1 );
            assertThat( clientBackend.getFlushInterval() ).isEqualTo( -1L );
        }
    }

    /**
     * 1 x line1, 20 x line2, 100 x line1
     */
    @SuppressWarnings( "unchecked" )
    private static List<Object>[] expectedLines( List<Object> lineData1, List<Object> lineData2 ) {
        List<List<Object>> lines = new java.util.ArrayList<>();
        lines.add( lineData1 );
        for( int i = 0; i < 20; i++ ) lines.add( lineData2 );
        for( int i = 0; i < 100; i++ ) lines.add( lineData1 );
        return lines.toArray( new List[0] );
    }

    @Test
    public void runtimeParametersValidation() {
        SocketLoggerBackend backend = new SocketLoggerBackend( null, 256, -1 );

        assertThatThrownBy( () -> backend.setMaxBuffers( 0 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> backend.setBufferSize( 0 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> backend.setBufferSize( "unknown", 1024 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThat( backend.getMaxBuffers() ).isEqualTo( 5000 );
        assertThat( backend.getBufferSizes() ).containsOnly( Map.entry( "DEFAULT", 256 ) );

        backend.setFlushInterval( 60_000 );
        assertThat( backend.getFlushInterval() ).isEqualTo( 60_000L );

        backend.close();
        assertThatThrownBy( () -> backend.setFlushInterval( 100 ) ).isInstanceOf( IllegalStateException.class );
    }
}
