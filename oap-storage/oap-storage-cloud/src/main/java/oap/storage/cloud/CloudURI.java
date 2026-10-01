package oap.storage.cloud;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FilenameUtils;

import java.io.Serial;
import java.io.Serializable;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * Backend-agnostic address of a blob: {@code fs://<configurationId>/<path>} — `configurationId` names a
 * {@link FileSystemConfiguration} entry (which backend/credentials to use), `path` is the blob's path within
 * that backend, always {@code /}-separated regardless of the host OS. See {@link FileSystem} for the facade
 * that resolves these against actual {@link FileSystemCloudApi} backends, and
 * {@link FileSystemCloudApi#toUri(CloudURI)} for rendering a backend-native (non-{@code fs://}) equivalent.
 */
@EqualsAndHashCode
public class CloudURI implements Serializable {
    @Serial
    private static final long serialVersionUID = -435068850003366393L;

    public final String configurationId;
    public final String path;

    /**
     * Parses an {@code fs://<configurationId>/<path>} string (scheme may be omitted, i.e. {@code <configurationId>/<path>}).
     *
     * @throws CloudException if the scheme is present but isn't {@code fs}, or `configurationId` is missing
     */
    public CloudURI( String uri ) throws CloudException {
        int schemeEnd = uri.indexOf( "://" );
        if( schemeEnd < 0 ) {
            throw new CloudException( "fs: configurationId is required in the URI, e.g. fs://<configurationId>/<path>" );
        }

        String scheme = uri.substring( 0, schemeEnd );
        if( !scheme.isEmpty() && !"fs".equals( scheme ) ) {
            throw new CloudException( s( "fs: expected URI scheme 'fs', got '${scheme}' — use fs://<configurationId>/<path>" ) );
        }

        String rest = uri.substring( schemeEnd + 3 );
        int slashIdx = rest.indexOf( '/' );
        String configurationId = slashIdx >= 0 ? rest.substring( 0, slashIdx ) : rest;
        String uriPath = slashIdx >= 0 ? rest.substring( slashIdx + 1 ) : "";

        if( configurationId.isEmpty() ) {
            throw new CloudException( "fs: configurationId is required in the URI, e.g. fs://<configurationId>/<path>" );
        }

        this.configurationId = configurationId;
        this.path = FilenameUtils.separatorsToUnix( uriPath );
    }

    /** `path` is normalized to {@code /}-separators and stripped of any leading {@code /}. */
    public CloudURI( String configurationId, String path ) {
        this.configurationId = configurationId;

        String unixPath = FilenameUtils.separatorsToUnix( path );

        this.path = unixPath.startsWith( "/" ) ? unixPath.substring( 1 ) : unixPath;
    }

    /** @return a copy of this URI under a different `configurationId`, same `path`. */
    public CloudURI withConfigurationId( String configurationId ) {
        return new CloudURI( configurationId, this.path );
    }

    /** @return a copy of this URI with a different `path`, same `configurationId`. */
    public CloudURI withPath( String path ) {
        return new CloudURI( this.configurationId, path );
    }

    /** @return the {@code fs://<configurationId>/<path>} form. */
    @Override
    public String toString() {
        return s( "fs://${configurationId}/${path}" );
    }
}
