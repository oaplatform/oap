package oap.storage.cloud;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;

import java.util.Map;

public interface Tags {
    void putBlob( BlobStore blobStore, Blob blob, CloudURI blobURI, Map<String, String> tags ) throws CloudException;
}
