package oap.logstream;

import oap.logstream.disk.LogstreamException;
import oap.template.Types;
import oap.util.Result;
import org.testng.annotations.Test;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

public class NullLoggerBackendTest {
    @Test
    public void logReturnsLogId() {
        String[] headers = new String[] { "h1" };
        byte[][] types = new byte[][] { new byte[] { Types.STRING.id } };

        try( NullLoggerBackend backend = new NullLoggerBackend() ) {
            Result<String, LogstreamException> result = backend.log( CURRENT_PROTOCOL_VERSION, "test1", "file1", Map.of( "p1", "v1" ), "type1",
                headers, types, "line1\n".getBytes( UTF_8 ) );

            assertThat( result.isSuccess() ).isTrue();
            assertThat( result.successValue ).isEqualTo( new LogId( "file1", "type1", "test1", Map.of( "p1", "v1" ), headers, types ).toString() );
        }
    }
}
