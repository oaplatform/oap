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
}
