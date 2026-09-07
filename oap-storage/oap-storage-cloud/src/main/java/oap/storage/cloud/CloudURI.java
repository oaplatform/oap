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

    public final String alias;
    public final String path;

    public CloudURI( String uri ) throws CloudException {
        try {
            // java.net.URI rejects a bare "fs://" (empty authority with no path) as malformed;
            // normalize it to the equivalent, parseable triple-slash form.
            URI u = new URI( uri.endsWith( "://" ) ? uri + "/" : uri );

            String scheme = u.getScheme();
            if( scheme != null && !"fs".equals( scheme ) ) {
                throw new CloudException( "fs: expected URI scheme 'fs', got '" + scheme + "' — use fs://<alias>/<path>" );
            }

            String alias = u.getHost();
            if( alias == null || alias.isEmpty() ) {
                throw new CloudException( "fs: alias is required in the URI, e.g. fs://<alias>/<path>" );
            }

            String uriPath = FilenameUtils.separatorsToUnix( u.getPath() );
            if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );

            this.alias = alias;
            this.path = uriPath;
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
    }

    public CloudURI( String alias, String path ) {
        this.alias = alias;

        String unixPath = FilenameUtils.separatorsToUnix( path );

        this.path = unixPath.startsWith( "/" ) ? unixPath.substring( 1 ) : unixPath;
    }

    public CloudURI withAlias( String alias ) {
        return new CloudURI( alias, this.path );
    }

    public CloudURI withPath( String path ) {
        return new CloudURI( this.alias, path );
    }

    @Override
    public String toString() {
        return "fs://" + alias + "/" + path;
    }
}
