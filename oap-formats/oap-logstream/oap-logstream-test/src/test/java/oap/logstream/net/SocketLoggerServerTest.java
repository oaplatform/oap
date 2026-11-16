package oap.logstream.net;

import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LogStreamProtocol;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.disk.LogstreamException;
import oap.logstream.net.server.SocketLoggerServer;
import oap.message.MessageProtocol;
import oap.template.Types;
import oap.util.FastByteArrayOutputStream;
import oap.util.Result;
import org.testng.annotations.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V3;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V4;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SocketLoggerServerTest {
    private static final String[] HEADERS = new String[] { "h1" };
    private static final byte[][] TYPES = new byte[][] { new byte[] { Types.STRING.id } };
    private static final byte[] ROW = "line1\n".getBytes( UTF_8 );
    private static final LogId LOG_ID = new LogId( "type", Map.of( "p1", "v1" ), HEADERS, TYPES );

    @Test
    public void success() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.LZ4 );
        assertThat( server.run( ROW_BINARY_V5.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_OK );

        assertThat( backend.calls ).isEqualTo( 1 );
        assertThat( backend.version ).isEqualTo( ROW_BINARY_V5 );
        assertThat( backend.compression ).isEqualTo( LogStreamCompression.LZ4 );
        assertThat( backend.logType ).isEqualTo( "type" );
        assertThat( backend.properties ).containsExactlyEntriesOf( Map.of( "p1", "v1" ) );
        assertThat( backend.headers ).containsExactly( HEADERS );
        assertThat( backend.types ).isDeepEqualTo( TYPES );
        assertThat( backend.buffer ).isEqualTo( ROW );
    }

    @Test
    public void failureRetry() {
        TestBackend backend = new TestBackend( Result.failure( new LogstreamException( new IOException( "retry" ), LogStreamProtocol.ErrorStatus.ERROR_RETRY ) ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.GZIP );
        assertThat( server.run( ROW_BINARY_V5.version, "host", data.length, data, "md5" ) ).isEqualTo( MessageProtocol.STATUS_UNKNOWN_ERROR );
        assertThat( backend.calls ).isEqualTo( 1 );
    }

    @Test
    public void failureNoRetry() {
        IOException cause = new IOException( "no retry" );
        TestBackend backend = new TestBackend( Result.failure( new LogstreamException( cause, LogStreamProtocol.ErrorStatus.ERROR_NO_RETRY ) ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.GZIP );
        assertThatThrownBy( () -> server.run( ROW_BINARY_V5.version, "host", data.length, data, "md5" ) )
            .isInstanceOf( LoggerException.class )
            .hasCause( cause );
        assertThat( backend.calls ).isEqualTo( 1 );
    }

    @Test
    public void backendNotAvailable() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        backend.state = AvailabilityReport.State.FAILED;
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.GZIP );
        assertThat( server.run( ROW_BINARY_V5.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_BACKEND_LOGGER_NOT_AVAILABLE );
        assertThat( backend.calls ).isZero();
    }

    @Test
    public void invalidVersion() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.GZIP );
        assertThat( server.run( 99, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.INVALID_VERSION );
        assertThat( backend.calls ).isZero();
    }

    @Test
    public void v3IsGzip() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payloadV3();
        assertThat( server.run( ROW_BINARY_V3.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_OK );

        assertThat( backend.calls ).isEqualTo( 1 );
        assertThat( backend.version ).isEqualTo( ROW_BINARY_V3 );
        assertThat( backend.compression ).isEqualTo( LogStreamCompression.GZIP );
        assertThat( backend.logType ).isEqualTo( "type" );
        assertThat( backend.properties ).containsExactlyEntriesOf( Map.of( "p1", "v1" ) );
        assertThat( backend.headers ).containsExactly( HEADERS );
        assertThat( backend.buffer ).isEqualTo( ROW );
    }

    /**
     * filePrefixPattern of the old clients is read and ignored
     */
    @Test
    public void v4PrefixIgnored() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payloadV4( LogStreamCompression.LZ4 );
        assertThat( server.run( ROW_BINARY_V4.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_OK );

        assertThat( backend.calls ).isEqualTo( 1 );
        assertThat( backend.version ).isEqualTo( ROW_BINARY_V4 );
        assertThat( backend.compression ).isEqualTo( LogStreamCompression.LZ4 );
        assertThat( backend.logType ).isEqualTo( "type" );
        assertThat( backend.properties ).containsExactlyEntriesOf( Map.of( "p1", "v1" ) );
        assertThat( backend.headers ).containsExactly( HEADERS );
        assertThat( backend.types ).isDeepEqualTo( TYPES );
        assertThat( backend.buffer ).isEqualTo( ROW );
    }

    @Test
    public void unknownCompression() {
        TestBackend backend = new TestBackend( Result.success( LOG_ID ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload( LogStreamCompression.GZIP );
        data[Buffer.COMPRESSION_POSITION] = 99;
        assertThatThrownBy( () -> server.run( ROW_BINARY_V5.version, "host", data.length, data, "md5" ) )
            .isInstanceOf( LoggerException.class )
            .hasRootCauseMessage( "unknown compression id 99" );
        assertThat( backend.calls ).isZero();
    }

    private static byte[] payload( LogStreamCompression compression ) {
        LogId id = new LogId( "type", Map.of( "p1", "v1" ), HEADERS, TYPES );
        Buffer buffer = new Buffer( 1024, id, ROW_BINARY_V5, Buffer.encodeHeader( id ) );
        assertThat( buffer.put( ROW ) ).isTrue();
        buffer.close( 1L );

        byte[] data = Arrays.copyOf( buffer.data(), buffer.length() );
        data[Buffer.COMPRESSION_POSITION] = compression.id;
        return data;
    }

    /**
     * v5 header + filePrefixPattern after the compression byte and clientHostname after logType
     */
    private static byte[] payloadV4( LogStreamCompression compression ) {
        byte[] v5 = payload( compression );
        int fixed = Buffer.COMPRESSION_POSITION + 1;
        // logType: writeUTF
        int logTypeLength = 2 + ( ( ( v5[fixed] & 0xFF ) << 8 ) | ( v5[fixed + 1] & 0xFF ) );

        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        try( DataOutputStream out = new DataOutputStream( baos ) ) {
            out.write( v5, 0, fixed );
            out.writeUTF( "prefix" );
            out.write( v5, fixed, logTypeLength );
            out.writeUTF( "client-host" );
            out.write( v5, fixed + logTypeLength, v5.length - fixed - logTypeLength );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return baos.toByteArray();
    }

    /**
     * v4 header without the compression byte
     */
    private static byte[] payloadV3() {
        byte[] v4 = payloadV4( LogStreamCompression.GZIP );
        byte[] v3 = new byte[v4.length - 1];
        System.arraycopy( v4, 0, v3, 0, Buffer.COMPRESSION_POSITION );
        System.arraycopy( v4, Buffer.COMPRESSION_POSITION + 1, v3, Buffer.COMPRESSION_POSITION, v4.length - Buffer.COMPRESSION_POSITION - 1 );
        return v3;
    }

    private static class TestBackend extends AbstractLoggerBackend {
        private final Result<LogId, LogstreamException> result;
        private AvailabilityReport.State state = AvailabilityReport.State.OPERATIONAL;
        private int calls;
        private ProtocolVersion version;
        private LogStreamCompression compression;
        private Map<String, String> properties;
        private String logType;
        private String[] headers;
        private byte[][] types;
        private byte[] buffer;

        private TestBackend( Result<LogId, LogstreamException> result ) {
            this.result = result;
        }

        @Override
        public Result<LogId, LogstreamException> log( ProtocolVersion version, LogStreamCompression compression,
                                                       Map<String, String> properties, String logType,
                                                       String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
            this.compression = compression;
            return log( version, properties, logType, headers, types, buffer, offset, length );
        }

        @Override
        public Result<LogId, LogstreamException> log( ProtocolVersion version, Map<String, String> properties, String logType,
                                                       String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
            this.calls++;
            this.version = version;
            this.properties = properties;
            this.logType = logType;
            this.headers = headers;
            this.types = types;
            this.buffer = Arrays.copyOfRange( buffer, offset, offset + length );

            return result;
        }

        @Override
        public void close() {
        }

        @Override
        public AvailabilityReport availabilityReport() {
            return new AvailabilityReport( state );
        }
    }
}
