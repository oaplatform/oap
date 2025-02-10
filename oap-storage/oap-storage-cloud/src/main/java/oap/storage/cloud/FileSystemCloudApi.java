package oap.storage.cloud;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public interface FileSystemCloudApi extends AutoCloseable {
    CompletableFuture<Boolean> blobExistsAsync( CloudURI path );

    default boolean blobExists( CloudURI path ) throws CloudException {
        try {
            return blobExistsAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Boolean> containerExistsAsync( CloudURI path );

    default boolean containerExists( CloudURI path ) throws CloudException {
        try {
            return containerExistsAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Void> deleteBlobAsync( CloudURI path );

    default void deleteBlob( CloudURI path ) throws CloudException {
        try {
            deleteBlobAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Void> deleteContainerAsync( CloudURI path ) throws CloudException;

    default void deleteContainer( CloudURI path ) throws CloudException {
        try {
            deleteContainerAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Boolean> createContainerAsync( CloudURI path );

    default boolean createContainer( CloudURI path ) throws CloudException {
        try {
            return createContainerAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Boolean> deleteContainerIfEmptyAsync( CloudURI path );

    default boolean deleteContainerIfEmpty( CloudURI path ) throws CloudException {
        try {
            return deleteContainerIfEmptyAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<? extends FileSystem.StorageItem> getMetadataAsync( CloudURI path );

    default FileSystem.StorageItem getMetadata( CloudURI path ) throws CloudException {
        try {
            return getMetadataAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Void> downloadFileAsync( CloudURI source, Path destination );

    default void downloadFile( CloudURI source, Path destination ) throws CloudException {
        try {
            downloadFileAsync( source, destination ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<Void> copyAsync( CloudURI source, CloudURI destination );

    default void copy( CloudURI source, CloudURI destination ) throws CloudException {
        try {
            copyAsync( source, destination ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<? extends InputStream> getInputStreamAsync( CloudURI path );

    default InputStream getInputStream( CloudURI path ) throws CloudException {
        try {
            return getInputStreamAsync( path ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags );

    CompletableFuture<Void> uploadAsync( CloudURI destination, BlobData blobData );

    default void upload( CloudURI destination, BlobData blobData ) throws CloudException {
        try {
            uploadAsync( destination, blobData ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    CompletableFuture<? extends PageSet<? extends FileSystem.StorageItem>> listAsync( CloudURI path, ListOptions listOptions );

    default PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) throws CloudException {
        try {
            return listAsync( path, listOptions ).join();
        } catch( Exception e ) {
            throw propagate( e );
        }
    }

    default CloudException propagate( Throwable e ) throws CloudException {
        return switch( e ) {
            case CloudException ce -> ce;
            case ExecutionException ee -> new CloudException( ee.getCause() );
            case CompletionException ce -> new CloudException( ce.getCause() );
            case null, default -> new CloudException( e );
        };
    }
}
