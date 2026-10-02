package oap.storage.cloud;

import lombok.Builder;

/**
 * Pagination options for {@link FileSystem#list}/{@link FileSystemCloudApi#list} — S3-style paging, honored by
 * every backend (S3 natively, others by skip/limit over a full listing using the same token shape). Page by
 * passing the previous call's {@link PageSet#nextContinuationToken} back in as `continuationToken`.
 */
@Builder
public class ListOptions {
    /** Opaque token from a previous page's {@link PageSet#nextContinuationToken}; {@code null} for the first page. */
    public String continuationToken;
    /** Max items to return in this page; {@code null} for no limit (and no {@link PageSet#nextContinuationToken}). */
    public Integer maxKeys;
}
