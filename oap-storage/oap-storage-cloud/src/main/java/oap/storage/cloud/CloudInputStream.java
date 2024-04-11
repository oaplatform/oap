package oap.storage.cloud;

import oap.io.Closeables;
import org.jclouds.blobstore.BlobStoreContext;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CloudInputStream extends FilterInputStream {
    public final Map<String, String> userMetadata;
    private final BlobStoreContext blobStoreContext;

    public CloudInputStream( InputStream inputStream, Map<String, String> userMetadata, BlobStoreContext blobStoreContext ) {
        super( inputStream );
        this.userMetadata = userMetadata;
        this.blobStoreContext = blobStoreContext;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            Closeables.close( blobStoreContext );
        }
    }
}
