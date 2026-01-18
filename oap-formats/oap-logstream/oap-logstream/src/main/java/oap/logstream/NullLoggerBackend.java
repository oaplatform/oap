/**
 * Copyright
 */
package oap.logstream;

import oap.logstream.LogStreamProtocol.ProtocolVersion;

import java.util.Map;

public class NullLoggerBackend extends AbstractLoggerBackend {
    @Override
    public String log( ProtocolVersion protocolVersion, String hostName, String filePreffix, Map<String, String> properties, String logType,
                       String[] headers, byte[][] types, byte[] row, int offset, int length ) {
        LogId logId = new LogId( filePreffix, logType, hostName, properties, headers, types );
        return logId.toString();
    }

    @Override
    public void close() {

    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
