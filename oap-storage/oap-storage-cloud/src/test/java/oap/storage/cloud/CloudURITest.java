package oap.storage.cloud;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudURITest {
    @Test
    public void testParse() {
        CloudURI cloudURI = new CloudURI( "fs://my-alias/logs/1.txt" );
        assertThat( cloudURI.configurationId ).isEqualTo( "my-alias" );
        assertThat( cloudURI.path ).isEqualTo( "logs/1.txt" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testParseRejectsNonFsScheme() {
        new CloudURI( "s3://my-bucket/logs/1.txt" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testParseRequiresConfigurationId() {
        new CloudURI( "fs:///logs/1.txt" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testParseNoAuthorityRequiresConfigurationId() {
        new CloudURI( "fs://" );
    }

    @Test
    public void testDoubleSlashPreservesLeadingSlashInPath() {
        // an extra leading slash after the configurationId survives into `path` -- this is how an absolute
        // local filesystem path is represented (see FileSystemCloudApiLocalFs.getPath), since the
        // normal (configurationId, path) constructor always strips a leading slash.
        CloudURI cloudURI = new CloudURI( "fs://file//tmp/a/file1" );
        assertThat( cloudURI.configurationId ).isEqualTo( "file" );
        assertThat( cloudURI.path ).isEqualTo( "/tmp/a/file1" );
    }

    @Test
    public void testTwoArgConstructorStripsLeadingSlash() {
        CloudURI cloudURI = new CloudURI( "my-alias", "/logs/1.txt" );
        assertThat( cloudURI.configurationId ).isEqualTo( "my-alias" );
        assertThat( cloudURI.path ).isEqualTo( "logs/1.txt" );
    }

    @Test
    public void testToString() {
        assertThat( new CloudURI( "my-alias", "logs/1.txt" ).toString() ).isEqualTo( "fs://my-alias/logs/1.txt" );
    }

    @Test
    public void testWithConfigurationIdAndWithPath() {
        CloudURI cloudURI = new CloudURI( "my-alias", "logs/1.txt" );

        assertThat( cloudURI.withConfigurationId( "other-alias" ) ).isEqualTo( new CloudURI( "other-alias", "logs/1.txt" ) );
        assertThat( cloudURI.withPath( "logs/2.txt" ) ).isEqualTo( new CloudURI( "my-alias", "logs/2.txt" ) );
    }
}
