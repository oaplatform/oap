package oap.storage.cloud;

import oap.system.Env;
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

    @Test
    public void testProperties() {
        Env.set( "TMP_S3_SCHEME", "s3" );
        System.setProperty( "TMP_S3_SCHEME", "file" );

        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.s3.jclouds.test", "${env.TMP_S3_SCHEME}",
                "fs.s3.jclouds.test2", "${TMP_S3_SCHEME}",
                "fs.s3.jclouds.test3", "${env.unknown}-${unknown}",
                "fs.default.jclouds.scheme", "s3",
                "fs.default.jclouds.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "jclouds.test" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "jclouds.test2" ) ).isEqualTo( "file" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "jclouds.test3" ) ).isEqualTo( "${env.unknown}-${unknown}" );
    }
}
