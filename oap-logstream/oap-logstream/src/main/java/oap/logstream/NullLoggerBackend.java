/**
 * Copyright
 */
package oap.logstream;

import oap.logstream.LogStreamProtocol.ProtocolVersion;

import java.util.Map;

public class NullLoggerBackend extends AbstractLoggerBackend {
    @Override
    public void log( ProtocolVersion protocolVersion, String hostName, String filePreffix, Map<String, String> properties, String logType,
                     String[] headers, byte[][] types, byte[] row, int offset, int length ) {
    }

    @Override
    public void close() {

    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
