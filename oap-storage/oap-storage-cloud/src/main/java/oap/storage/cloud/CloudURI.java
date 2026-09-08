package oap.storage.cloud;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FilenameUtils;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

@EqualsAndHashCode
public class CloudURI implements Serializable {
    @Serial
    private static final long serialVersionUID = -435068850003366393L;

    public final String configurationId;
    public final String path;

    public CloudURI( String uri ) throws CloudException {
        try {
            // java.net.URI rejects a bare "fs://" (empty authority with no path) as malformed;
            // normalize it to the equivalent, parseable triple-slash form.
            URI u = new URI( uri.endsWith( "://" ) ? uri + "/" : uri );

            String scheme = u.getScheme();
            if( scheme != null && !"fs".equals( scheme ) ) {
                throw new CloudException( "fs: expected URI scheme 'fs', got '" + scheme + "' — use fs://<configurationId>/<path>" );
            }

            String configurationId = u.getHost();
            if( configurationId == null || configurationId.isEmpty() ) {
                throw new CloudException( "fs: configurationId is required in the URI, e.g. fs://<configurationId>/<path>" );
            }

            String uriPath = FilenameUtils.separatorsToUnix( u.getPath() );
            if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );

            this.configurationId = configurationId;
            this.path = uriPath;
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
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
        return "fs://" + configurationId + "/" + path;
    }
}
