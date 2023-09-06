package oap.hadoop;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.net.URI;
import java.net.URISyntaxException;

public enum OapFileSystemType {
    FILE( "file://" ) {
        @Override
        public Path getPath( String name, Configuration configuration ) {
            return new Path( fsDefaultFS + ( name.startsWith( "/" ) ? "" : "/" ) + name );
        }
    },
    /**
     * fs.sftp.hostname = host
     * fs.sftp.port = 22 // optional
     */
    SFTP( "sftp://" ) {
        @Override
        public Path getPath( String name, Configuration configuration ) {
            String host = configuration.get( "fs.sftp.host" );

            Preconditions.checkNotNull( host, "fs.sftp.host" );

            String user = configuration.get( "fs.sftp.user." + host );

            Preconditions.checkNotNull( user, "fs.sftp.user." + host );

            return new Path( fsDefaultFS + host + ( name.startsWith( "/" ) ? "" : "/" ) + name );
        }

    },

    /**
     * fs.s3a.access.key = access key
     * fs.s3a.secret.key = secret key
     * fs.s3a.endpoint = s3a://s3-website.{REGION}.amazonaws.com/{BUCKET}
     */
    S3A( "s3a://" ) {
        @Override
        public Path getPath( String name, Configuration configuration ) {
            try {
                String endpoint = configuration.get( "fs.s3a.endpoint" );
                String bucket = configuration.get( "fs.s3a.bucket" );

                Preconditions.checkNotNull( endpoint, "fs.s3a.endpoint" );
                if( bucket == null ) {
                    bucket = new URI( endpoint ).getPath();
                }

                return new Path( fsDefaultFS
                    + ( bucket.startsWith( "/" ) ? bucket.substring( 1 ) : "" )
                    + ( name.startsWith( "/" ) ? "" : "/" )
                    + name );
            } catch( URISyntaxException e ) {
                throw new RuntimeException( e );
            }
        }
    };

    protected final String fsDefaultFS;

    OapFileSystemType( String fsDefaultFS ) {
        this.fsDefaultFS = fsDefaultFS;
    }

    public abstract Path getPath( String name, Configuration configuration );
}
