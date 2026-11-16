/**
 * Copyright
 */
package oap.logstream;

import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.logstream.disk.LogstreamException;
import oap.util.Result;

import java.util.Map;

public class NullLoggerBackend extends AbstractLoggerBackend {
    @Override
    public Result<LogId, LogstreamException> log( ProtocolVersion protocolVersion, Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] row, int offset, int length ) {
        LogId logId = new LogId( logType, properties, headers, types );
        return Result.success( logId );
    }

    @Override
    public void close() {

    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
