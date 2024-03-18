package oap.storage.cloud;

import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class FileSystemConfigurationTest {
    @Test
    public void getDefault() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of( "fs.s3.test-bucket.jclouds.endpoint", "http://localhost/s3/tb",
                "fs.s3.jclouds.endpoint", "http://localhost/s3",
                "fs", Map.of(
                    "default.jclouds.scheme", "s3",
                    "default.jclouds.container", "test-bucket"
                )
            )
        );

        assertThat( fileSystemConfiguration.get( "s3", "my-container" ) )
            .contains( entry( "jclouds.endpoint", "http://localhost/s3" ) );
        assertThat( fileSystemConfiguration.get( "s3", "test-bucket" ) )
            .contains( entry( "jclouds.endpoint", "http://localhost/s3/tb" ) );

        assertThat( fileSystemConfiguration.getDefaultScheme() ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getDefaultContainer() ).isEqualTo( "test-bucket" );
    }

}
