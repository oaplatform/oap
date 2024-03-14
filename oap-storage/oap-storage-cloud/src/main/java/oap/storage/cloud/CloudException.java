package oap.storage.cloud;

import java.io.Serial;

public class CloudException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -9090506632286848059L;

    public CloudException() {
    }

    public CloudException( String message ) {
        super( message );
    }

    public CloudException( String message, Throwable cause ) {
        super( message, cause );
    }

    public CloudException( Throwable cause ) {
        super( cause );
    }

    public CloudException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
}
