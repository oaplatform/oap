package oap.hadoop;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

public enum OapFileSystemType {
    FILE( "file://" ) {
        @Override
        public String root( Configuration configuration ) {
            String root = configuration.get( "fs.file.root" );

            Preconditions.checkNotNull( root, "fs.file.root" );
            Preconditions.checkNotNull( root.startsWith( "/" ), "The path must start with a '/', but " + root );

            return fsDefaultFS + root;
        }
    },
    /**
     * fs.sftp.hostname = host
     * fs.sftp.port = 22 // optional
     */
    SFTP( "sftp://" ) {
        @Override
        public String root( Configuration configuration ) {
            try {
                String host = configuration.get( "fs.sftp.hostname" );
                String port = configuration.get( "fs.sftp.port" );
                String user = configuration.get( "fs.sftp.user" );

                Preconditions.checkNotNull( host, "fs.sftp.hostname" );
                Preconditions.checkNotNull( user, "fs.sftp.user" );
                URI uri = new URI( "sftp", user, host, port != null ? Integer.parseInt( port ) : -1, "/", null, null );
                return uri.toString();
            } catch( URISyntaxException e ) {
                throw new RuntimeException( e );
            }
        }

    },

    /**
     * fs.s3a.access.key = access key
     * fs.s3a.secret.key = secret key
     * fs.s3a.backet = backet name
     * fs.s3a.region = region
     * fs.s3a.aws.credentials.provider = provider
     */
    S3A( "s3a://" ) {
        @Override
        public String root( Configuration configuration ) {
            String region = configuration.get( "fs.s3a.region" );
            String bucket = configuration.get( "fs.s3a.bucket" );

            Preconditions.checkNotNull( region, "fs.s3a.region" );
            Preconditions.checkNotNull( configuration.get( bucket, "fs.s3a.bucket" ) );


            return fsDefaultFS + "s3." + region + ".amazonaws.com/" + bucket;
        }
    };

    protected final String fsDefaultFS;

    OapFileSystemType( String fsDefaultFS ) {
        this.fsDefaultFS = fsDefaultFS;
    }

    public abstract String root( Configuration configuration );
}
