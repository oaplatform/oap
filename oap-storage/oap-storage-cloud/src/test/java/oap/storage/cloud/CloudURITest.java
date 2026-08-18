package oap.storage.cloud;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudURITest {
    @Test
    public void testParse() {
        CloudURI cloudURI = new CloudURI( "s3://my-bucket/logs/1.txt" );
        assertThat( cloudURI.scheme ).isEqualTo( "s3" );
        assertThat( cloudURI.container ).isEqualTo( "my-bucket" );
        assertThat( cloudURI.path ).isEqualTo( "logs/1.txt" );
    }

    @Test
    public void testParseFile() {
        CloudURI cloudURI = new CloudURI( "file://my-bucket/logs/1.txt" );
        assertThat( cloudURI.scheme ).isEqualTo( "file" );
        assertThat( cloudURI.container ).isEmpty();
        assertThat( cloudURI.path ).isEqualTo( "my-bucket/logs/1.txt" );

        cloudURI = new CloudURI( "file:///my-bucket/logs/1.txt" );
        assertThat( cloudURI.scheme ).isEqualTo( "file" );
        assertThat( cloudURI.container ).isEmpty();
        assertThat( cloudURI.path ).isEqualTo( "/my-bucket/logs/1.txt" );
    }

    @Test
    public void testParseFtp() {
        CloudURI cloudURI = new CloudURI( "ftp://my-server/logs/1.txt" );
        assertThat( cloudURI.scheme ).isEqualTo( "ftp" );
        assertThat( cloudURI.container ).isEqualTo( "my-server" );
        assertThat( cloudURI.path ).isEqualTo( "logs/1.txt" );
    }

    @Test
    public void testParseFtpWithPort() {
        CloudURI cloudURI = new CloudURI( "ftp://localhost:12345/my_path" );
        assertThat( cloudURI.scheme ).isEqualTo( "ftp" );
        assertThat( cloudURI.container ).isEqualTo( "localhost:12345" );
        assertThat( cloudURI.path ).isEqualTo( "my_path" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testParseFtpRequiresContainer() {
        new CloudURI( "ftp:///logs/1.txt" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testParseFtpNoAuthorityRequiresContainer() {
        new CloudURI( "ftp://" );
    }

    @Test
    public void testParseFtps() {
        CloudURI cloudURI = new CloudURI( "ftps://my-server/logs/1.txt" );
        assertThat( cloudURI.scheme ).isEqualTo( "ftps" );
        assertThat( cloudURI.container ).isEqualTo( "my-server" );
        assertThat( cloudURI.path ).isEqualTo( "logs/1.txt" );
    }
}
