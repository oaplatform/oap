package oap.storage.cloud;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

public interface FileSystemCloudApi extends Closeable {
    boolean blobExists( CloudURI path ) throws CloudException;

    boolean containerExists( CloudURI path ) throws CloudException;

    void deleteBlob( CloudURI path ) throws CloudException;

    void deleteContainer( CloudURI path ) throws CloudException;

    boolean createContainer( CloudURI path ) throws CloudException;

    boolean deleteContainerIfEmpty( CloudURI path ) throws CloudException;

    FileSystem.StorageItem getMetadata( CloudURI path ) throws CloudException;

    void downloadFile( CloudURI source, Path destination ) throws CloudException;

    void copy( CloudURI source, CloudURI destination ) throws CloudException;

    InputStream getInputStream( CloudURI path ) throws CloudException;

    OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags );

    void upload( CloudURI destination, BlobData blobData );

    PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions );
}
