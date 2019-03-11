/**
 * Copyright
 */
package oap.logstream;

public class NullLoggerBackend extends LoggerBackend {
    @Override
    public void log( String hostName, String fileName, String logType, int shard, int version, byte[] buffer, int offset, int length ) {

    }

    @Override
    public void close() {

    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
