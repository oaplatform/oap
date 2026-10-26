package oap.logstream.disk;

import oap.logstream.LogStreamProtocol;

public class LogstreamException extends RuntimeException {
    public final LogStreamProtocol.ErrorStatus status;

    public LogstreamException( Throwable cause, LogStreamProtocol.ErrorStatus status ) {
        super( cause );
        this.status = status;
    }
}
