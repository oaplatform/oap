package oap.storage.cloud;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * One storage backend (S3, SMB, FTP/FTPS, local file, ...), scoped to a single {@code configurationId} —
 * implementations are constructed per-configurationId by {@link FileSystem} (via reflection, see
 * {@code cloud-service.properties}) and cached/closed there; callers should go through {@link FileSystem}
 * rather than using an implementation directly.
 */
public interface FileSystemCloudApi extends AutoCloseable {
    /** @return {@code true} if a blob exists at `path`. */
    boolean blobExists( CloudURI path ) throws CloudException;

    /** @return {@code true} if `path`'s container exists. */
    boolean containerExists( CloudURI path ) throws CloudException;

    /**
     * @return {@code true} on success
     */
    boolean deleteBlob( CloudURI path ) throws CloudException;

    /** Deletes `path`'s container unconditionally (backend-specific whether it must be empty first). */
    void deleteContainer( CloudURI path ) throws CloudException;

    /** @return {@code true} if `path`'s container was created; {@code false} if it already existed. */
    boolean createContainer( CloudURI path ) throws CloudException;

    /** @return {@code true} if `path`'s container was deleted; {@code false} if it was non-empty (not deleted). */
    boolean deleteContainerIfEmpty( CloudURI path ) throws CloudException;

    /** @return metadata for `path`, or {@code null} if it doesn't exist. */
    FileSystem.StorageItem getMetadata( CloudURI path ) throws CloudException;

    /** Downloads `source` to the local `destination`. */
    void downloadFile( CloudURI source, Path destination ) throws CloudException;

    /** Copies `source` to `destination` within this backend. */
    void copy( CloudURI source, CloudURI destination ) throws CloudException;

    /** Opens `path` for reading. */
    InputStream getInputStream( CloudURI path ) throws CloudException;

    /** Opens `cloudURI` for writing, tagged with `tags` (backend-specific; may be ignored). */
    OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags );

    /** Uploads `blobData` to `destination`. */
    void upload( CloudURI destination, BlobData blobData ) throws CloudException;

    /** Lists blobs under `path` (a container/prefix) per `listOptions`. */
    PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) throws CloudException;

    /**
     * Renders {@code path} as a "native"-looking URL for this backend instead of the {@code fs://<configurationId>/<path>}
     * address. Default falls back to {@code fs://<configurationId>/<path>} ({@code path.toString()}).
     */
    default String toUri( CloudURI path ) throws CloudException {
        return path.toString();
    }
}
