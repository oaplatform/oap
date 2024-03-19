package oap.storage.cloud;

public class CloudBlobNotFoundException extends CloudException {
    public CloudBlobNotFoundException( CloudURI blob ) {
        super( blob.toString() );
    }
}
