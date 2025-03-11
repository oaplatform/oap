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
    private static final long serialVersionUID = -435068850003366392L;

    public final String scheme;
    public final String container;
    public final String path;

    public CloudURI( String uri ) throws CloudException {
        try {
            URI u = new URI( uri );

            scheme = u.getScheme();
            container = u.getHost();
            String uriPath = u.getPath();
            uriPath = FilenameUtils.separatorsToUnix( uriPath );
            if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );
            path = uriPath;

            getProvider();
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
    }

    public CloudURI( String scheme, String container, String path ) {
        this.scheme = scheme;
        this.container = container;

        String unixPath = FilenameUtils.separatorsToUnix( path );

        this.path = unixPath.startsWith( "/" ) ? unixPath.substring( 1 ) : unixPath;
    }

    public CloudURI withContainer( String container ) {
        return new CloudURI( this.scheme, container, this.path );
    }

    public CloudURI withPath( String path ) {
        return new CloudURI( this.scheme, this.container, path );
    }

    public CloudURI withSchema( String scheme ) {
        return new CloudURI( scheme, this.container, this.path );
    }

    public String getProvider() {
        return switch( scheme ) {
            case "s3" -> "aws-s3";
            case "gcs" -> "google-cloud-storage";
            case "ab" -> "azureblob";
            case "file" -> "filesystem";
            default -> throw new CloudException( "unsupported schema " + scheme );
        };
    }

    @Override
    public String toString() {
        return scheme + "://" + ( container != null ? container : "" ) + "/" + path;
    }
}
