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
            Map.of( "fs.s3.test-bucket.clouds.endpoint", "http://localhost/s3/tb",
                "fs.s3.clouds.endpoint", "http://localhost/s3",
                "fs", Map.of(
                    "default.clouds.scheme", "s3",
                    "default.clouds.container", "test-bucket"
                )
            )
        );

        assertThat( fileSystemConfiguration.get( "s3", "my-container" ) )
            .contains( entry( "clouds.endpoint", "http://localhost/s3" ) );
        assertThat( fileSystemConfiguration.get( "s3", "test-bucket" ) )
            .contains( entry( "clouds.endpoint", "http://localhost/s3/tb" ) );

        assertThat( fileSystemConfiguration.getDefaultScheme() ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getDefaultContainer() ).isEqualTo( "test-bucket" );
    }

    @Test
    public void testProperties() {
        Env.set( "TMP_S3_SCHEME", "s3" );
        System.setProperty( "TMP_S3_SCHEME", "file" );

        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.s3.clouds.test", "${env.TMP_S3_SCHEME}",
                "fs.s3.clouds.test2", "${TMP_S3_SCHEME}",
                "fs.s3.clouds.test3", "${env.unknown}-${unknown}",
                "fs.default.clouds.scheme", "s3",
                "fs.default.clouds.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test2" ) ).isEqualTo( "file" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test3" ) ).isEqualTo( "${env.unknown}-${unknown}" );
    }

    @Test
    public void testOapCloudsProperties() {
        Env.set( "TMP_S3_SCHEME", "s3" );
        System.setProperty( "TMP_S3_SCHEME", "file" );

        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.s3.clouds.test", "${env.TMP_S3_SCHEME}",
                "fs.s3.clouds.test2", "${TMP_S3_SCHEME}",
                "fs.s3.clouds.test3", "${env.unknown}-${unknown}",
                "fs.default.clouds.scheme", "s3",
                "fs.default.clouds.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test2" ) ).isEqualTo( "file" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "", "clouds.test3" ) ).isEqualTo( "${env.unknown}-${unknown}" );
    }

    @Test
    public void testEscapedDotContainer() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.ftp.ftp\\.server1\\.com.clouds.identity", "as",
                "fs.ftp.localhost:12345.clouds.identity", "as2",
                "fs.default.clouds.scheme", "ftp",
                "fs.default.clouds.container", "localhost:12345"
            )
        );

        assertThat( fileSystemConfiguration.get( "ftp", "ftp.server1.com", "clouds.identity" ) ).isEqualTo( "as" );
        assertThat( fileSystemConfiguration.get( "ftp", "localhost:12345", "clouds.identity" ) ).isEqualTo( "as2" );
    }

    @Test
    public void testCopyWith() {
        FileSystemConfiguration base = new FileSystemConfiguration( Map.of(
            "fs.s3.clouds.identity", "base-id",
            "fs.s3.clouds.region", "us-east-1",
            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( Map.of(
            "fs.s3.clouds.identity", "override-id"
        ) );

        assertThat( merged.get( "s3", "", "clouds.identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "", "clouds.region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "", "clouds.identity" ) ).isEqualTo( "base-id" );
        assertThat( merged.getDefaultScheme() ).isEqualTo( "s3" );
    }

    @Test
    public void testCopyWithFileSystemConfiguration() {
        FileSystemConfiguration base = new FileSystemConfiguration( Map.of(
            "fs.s3.clouds.identity", "base-id",
            "fs.s3.clouds.region", "us-east-1",
            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", "my-bucket"
        ) );

        FileSystemConfiguration overrides = new FileSystemConfiguration( Map.of(
            "fs.s3.clouds.identity", "override-id",
            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( overrides );

        assertThat( merged.get( "s3", "", "clouds.identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "", "clouds.region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "", "clouds.identity" ) ).isEqualTo( "base-id" );
    }
}
