package oap.storage.cloud;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FilenameUtils;

import java.io.Serial;
import java.io.Serializable;

import static dev.khbd.interp4j.core.Interpolations.s;

@EqualsAndHashCode
public class CloudURI implements Serializable {
    @Serial
    private static final long serialVersionUID = -435068850003366393L;

    public final String configurationId;
    public final String path;

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

    public CloudURI( String configurationId, String path ) {
        this.configurationId = configurationId;

        String unixPath = FilenameUtils.separatorsToUnix( path );

        this.path = unixPath.startsWith( "/" ) ? unixPath.substring( 1 ) : unixPath;
    }

    public CloudURI withConfigurationId( String configurationId ) {
        return new CloudURI( configurationId, this.path );
    }

    public CloudURI withPath( String path ) {
        return new CloudURI( this.configurationId, path );
    }

    @Override
    public String toString() {
        return s( "fs://${configurationId}/${path}" );
    }
}
