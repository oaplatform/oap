package oap.storage.cloud;

import org.testng.annotations.Test;

import java.io.File;
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
    public void testToFile() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.file.jclouds.filesystem.basedir", "/tmp",
                "fs.file.tmp.jclouds.filesystem.basedir", "/container",
                "fs.default.jclouds.scheme", "s3",
                "fs.default.jclouds.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.toFile( new CloudURI( "file://container/a/file1" ) ) ).isEqualTo( new File( "/tmp/container/a/file1" ) );
        assertThat( fileSystemConfiguration.toFile( new CloudURI( "file://tmp/a/file1" ) ) ).isEqualTo( new File( "/container/tmp/a/file1" ) );
    }
}
