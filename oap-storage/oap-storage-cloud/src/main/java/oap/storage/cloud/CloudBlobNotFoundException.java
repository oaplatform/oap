package oap.storage.cloud;

import java.io.Serial;

public class CloudBlobNotFoundException extends CloudException {
    @Serial
    private static final long serialVersionUID = 1602763875095843228L;

    public CloudBlobNotFoundException( CloudURI blob ) {
        super( blob.toString() );
    }
}
