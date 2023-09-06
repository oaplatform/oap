package oap.hadoop;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import static oap.io.Resources.IS_WINDOWS;

public enum OapFileSystemType {
    FILE( "file://" ) {
        @Override
        public Path _getPath( String name, Configuration configuration ) {
            return new Path( fsDefaultFS + ( name.startsWith( "/" ) ? "" : "/" ) + name );
        }
    },
    /**
     * fs.sftp.hostname = host
     * fs.sftp.port = 22 // optional
     */
    SFTP( "sftp://" ) {
        @Override
        public Path _getPath( String name, Configuration configuration ) {
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
     * fs.s3a.bucket = bucket
     * fs.s3a.endpoint = s3a://s3.{REGION}.amazonaws.com}
     */
    S3A( "s3a://" ) {
        @Override
        public Path _getPath( String name, Configuration configuration ) {
            String bucket = configuration.get( "fs.s3a.bucket" );

            Preconditions.checkNotNull( bucket, "fs.s3a.bucket" );
            return new Path( fsDefaultFS + bucket + '/' + ( name.startsWith( "/" ) ? "" : "/" ) + name );
        }
    };

    protected final String fsDefaultFS;

    OapFileSystemType( String fsDefaultFS ) {
        this.fsDefaultFS = fsDefaultFS;
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    public final Path getPath( String name, Configuration configuration ) {
        if( IS_WINDOWS ) {
            name = FilenameUtils.separatorsToUnix( name );
        }

        return _getPath( name, configuration );
    }

    @SuppressWarnings( "checkstyle:MethodName" )
    protected abstract Path _getPath( String name, Configuration configuration );
}
