package oap.logstream.net;

import oap.logstream.AbstractLoggerBackend;
import oap.logstream.AvailabilityReport;
import oap.logstream.LogId;
import oap.logstream.LogStreamProtocol;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.LoggerException;
import oap.logstream.disk.LogstreamException;
import oap.logstream.net.server.SocketLoggerServer;
import oap.message.MessageProtocol;
import oap.template.Types;
import oap.util.Result;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SocketLoggerServerTest {
    private static final String[] HEADERS = new String[] { "h1" };
    private static final byte[][] TYPES = new byte[][] { new byte[] { Types.STRING.id } };
    private static final byte[] ROW = "line1\n".getBytes( UTF_8 );

    @Test
    public void success() {
        TestBackend backend = new TestBackend( Result.success( "log-id" ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload();
        assertThat( server.run( ROW_BINARY_V3.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_OK );

        assertThat( backend.calls ).isEqualTo( 1 );
        assertThat( backend.version ).isEqualTo( ROW_BINARY_V3 );
        assertThat( backend.hostName ).isEqualTo( "client-host" );
        assertThat( backend.filePreffix ).isEqualTo( "prefix" );
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

        byte[] data = payload();
        assertThat( server.run( ROW_BINARY_V3.version, "host", data.length, data, "md5" ) ).isEqualTo( MessageProtocol.STATUS_UNKNOWN_ERROR );
        assertThat( backend.calls ).isEqualTo( 1 );
    }

    @Test
    public void failureNoRetry() {
        IOException cause = new IOException( "no retry" );
        TestBackend backend = new TestBackend( Result.failure( new LogstreamException( cause, LogStreamProtocol.ErrorStatus.ERROR_NO_RETRY ) ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload();
        assertThatThrownBy( () -> server.run( ROW_BINARY_V3.version, "host", data.length, data, "md5" ) )
            .isInstanceOf( LoggerException.class )
            .hasCause( cause );
        assertThat( backend.calls ).isEqualTo( 1 );
    }

    @Test
    public void backendNotAvailable() {
        TestBackend backend = new TestBackend( Result.success( "log-id" ) );
        backend.state = AvailabilityReport.State.FAILED;
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload();
        assertThat( server.run( ROW_BINARY_V3.version, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.STATUS_BACKEND_LOGGER_NOT_AVAILABLE );
        assertThat( backend.calls ).isZero();
    }

    @Test
    public void invalidVersion() {
        TestBackend backend = new TestBackend( Result.success( "log-id" ) );
        SocketLoggerServer server = new SocketLoggerServer( backend );

        byte[] data = payload();
        assertThat( server.run( 99, "host", data.length, data, "md5" ) ).isEqualTo( LogStreamProtocol.INVALID_VERSION );
        assertThat( backend.calls ).isZero();
    }

    private static byte[] payload() {
        Buffer buffer = new Buffer( 1024, new LogId( "prefix", "type", "client-host", Map.of( "p1", "v1" ), HEADERS, TYPES ), ROW_BINARY_V3 );
        assertThat( buffer.put( ROW ) ).isTrue();
        buffer.close( 1L );

        return Arrays.copyOf( buffer.data(), buffer.length() );
    }

    private static class TestBackend extends AbstractLoggerBackend {
        private final Result<String, LogstreamException> result;
        private AvailabilityReport.State state = AvailabilityReport.State.OPERATIONAL;
        private int calls;
        private ProtocolVersion version;
        private String hostName;
        private String filePreffix;
        private Map<String, String> properties;
        private String logType;
        private String[] headers;
        private byte[][] types;
        private byte[] buffer;

        private TestBackend( Result<String, LogstreamException> result ) {
            this.result = result;
        }

        @Override
        public Result<String, LogstreamException> log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                                                       String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
            this.calls++;
            this.version = version;
            this.hostName = hostName;
            this.filePreffix = filePreffix;
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
