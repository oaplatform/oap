package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

@ToString
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
            if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );
            path = uriPath;

            Preconditions.checkArgument( !path.isEmpty(), "container is required" );

            getProvider();
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
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

    public CloudURI resolve( String name ) {
        return new CloudURI( scheme + "://" + container + path + "/" + name );
    }
}
