package oap.storage.cloud;

/**
 * Marker for FileSystemCloudApi implementations where `container` identifies a distinct
 * backend connection (e.g. FTP host) rather than a request-scoped parameter (e.g. S3 bucket).
 * FileSystem uses this to decide whether its instance cache is keyed by scheme alone
 * or by scheme+container.
 */
public interface ContainerScopedCloudApi {
}
