package oap.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

public enum OapFileSystemType {
    FILE( "file:///" ),
    SFTP( "sftp://" ),
    S3A( "s3a://" );

    protected final String fsDefaultFS;

    OapFileSystemType( String fsDefaultFS ) {
        this.fsDefaultFS = fsDefaultFS;
    }

    public Path getPath( String name, Configuration configuration ) {
        return new Path( fsDefaultFS + ( name.startsWith( "/" ) ? name.substring( 1 ) : name ) );
    }
}
