/**
 * Copyright
 */
package oap.logstream;

public class NullLoggingBackend implements LoggingBackend {
    @Override
    public void log( String hostName, String fileName, byte[] buffer, int offset, int length ) {

    }

    @Override
    public void close() {

    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
