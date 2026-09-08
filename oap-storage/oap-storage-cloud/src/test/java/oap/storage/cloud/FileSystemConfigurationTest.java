package oap.storage.cloud;

import oap.system.Env;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemConfigurationTest {
    @Test
    public void testThreeTierFallback() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.ftp.container", "ftp.example.com:21",
            "fs.ftp.identity", "shared-user",
            "fs.ftp.credential", "shared-pass",
            "fs.ftp.container.secondary", "ftp.example.com:21",
            "fs.ftp.identity.secondary", "other-user",
            "fs.ftp.credential.secondary", "other-pass"
        ) );

        // configurationId-specific override wins
        assertThat( fileSystemConfiguration.get( "ftp", "secondary", "identity" ) ).isEqualTo( "other-user" );
        // no configurationId-specific override -> falls to scheme-wide
        assertThat( fileSystemConfiguration.get( "ftp", "secondary", "container" ) ).isEqualTo( "ftp.example.com:21" );
        assertThat( fileSystemConfiguration.get( "ftp", "primary", "identity" ) ).isEqualTo( "shared-user" );
        // no property at all for this configurationId/scheme -> falls to fs.default.<property> (absent here -> null)
        assertThat( fileSystemConfiguration.get( "ftp", "primary", "domain" ) ).isNull();
    }

    @Test
    public void testFallsBackToFsDefaultProperty() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.identity", "default-identity",
            "fs.s3.container", "my-bucket"
        ) );

        // neither fs.s3.identity.primary nor fs.s3.identity is declared -> falls all the way to fs.default.identity
        assertThat( fileSystemConfiguration.get( "s3", "primary", "identity" ) ).isEqualTo( "default-identity" );
    }

    @Test
    public void testFsDefaultIsOptional() {
        // no fs.default.* key at all -> construction succeeds, property lookups still work
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container", "test-bucket"
        ) );

        assertThat( fileSystemConfiguration.get( "s3", "unused", "container" ) ).isEqualTo( "test-bucket" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testConfigurationIdCannotBeUsedByMultipleSchemes() {
        new FileSystemConfiguration( Map.of(
            "fs.s3.container.shared", "bucket",
            "fs.ftp.container.shared", "host:21"
        ) );
    }

    @Test
    public void testGetSchemeAndGetSchemeOrThrow() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container.primary", "bucket-a",
            "fs.s3.container.secondary", "bucket-b"
        ) );

        assertThat( fileSystemConfiguration.getSchemeOrThrow( "primary" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getSchemeOrThrow( "secondary" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getScheme( "unknown-configurationId" ) ).isNull();
    }

    @Test( expectedExceptions = CloudException.class )
    public void testGetSchemeOrThrowThrowsForUnknownConfigurationId() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container", "bucket-a"
        ) );

        fileSystemConfiguration.getSchemeOrThrow( "unknown-configurationId" );
    }

    @Test
    public void testRequired() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container.primary", "bucket-a"
        ) );

        fileSystemConfiguration.required( "primary" );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testRequiredThrowsForUnknownConfigurationId() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container.primary", "bucket-a"
        ) );

        fileSystemConfiguration.required( "unknown-configurationId" );
    }

    @Test
    public void testProperties() {
        Env.set( "TMP_S3_SCHEME", "s3" );
        System.setProperty( "TMP_S3_SCHEME", "file" );

        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.s3.test", "${env.TMP_S3_SCHEME}",
                "fs.s3.test2", "${TMP_S3_SCHEME}",
                "fs.s3.test3", "${env.unknown}-${unknown}",
                "fs.s3.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test2" ) ).isEqualTo( "file" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test3" ) ).isEqualTo( "${env.unknown}-${unknown}" );
    }

    @Test
    public void testSystemPropertyOverridesMapValue() {
        System.setProperty( "fs.s3.container", "from-system-property" );
        try {
            FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
                "fs.s3.container", "from-map"
            ) );

            assertThat( fileSystemConfiguration.get( "s3", "unused", "container" ) ).isEqualTo( "from-system-property" );
        } finally {
            System.clearProperty( "fs.s3.container" );
        }
    }

    @Test
    public void testEnvOverridesSystemPropertyAndMapValue() {
        System.setProperty( "fs.s3.container", "from-system-property" );
        Env.set( "fs.s3.container", "from-env" );
        try {
            FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
                "fs.s3.container", "from-map"
            ) );

            assertThat( fileSystemConfiguration.get( "s3", "unused", "container" ) ).isEqualTo( "from-env" );
        } finally {
            System.clearProperty( "fs.s3.container" );
            Env.set( "fs.s3.container", null );
        }
    }

    @Test
    public void testSystemPropertyUnderscoreNormalizedToHyphen() {
        System.setProperty( "fs.file.filesystem.remove_empty_folders", "true" );
        try {
            FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of() );

            assertThat( fileSystemConfiguration.get( "file", "unused", "filesystem.remove-empty-folders" ) ).isEqualTo( "true" );
        } finally {
            System.clearProperty( "fs.file.filesystem.remove_empty_folders" );
        }
    }

    @Test
    public void testEnvUnderscoreNormalizedToHyphen() {
        Env.set( "fs.file.filesystem.remove_empty_folders", "true" );
        try {
            FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of() );

            assertThat( fileSystemConfiguration.get( "file", "unused", "filesystem.remove-empty-folders" ) ).isEqualTo( "true" );
        } finally {
            Env.set( "fs.file.filesystem.remove_empty_folders", null );
        }
    }

    @Test
    public void testConfigurationIdContainingDotIsSafeWithNoEscaping() {
        // the new lookup mechanism probes exact key strings ("property" + "." + configurationId) rather than
        // positionally splitting stored keys, so a dot inside a configurationId needs no escaping at all.
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.ftp.container", "host:21",
            "fs.ftp.identity.my.alias", "dotted-user"
        ) );

        assertThat( fileSystemConfiguration.get( "ftp", "my.alias", "identity" ) ).isEqualTo( "dotted-user" );
    }

    @Test
    public void testCopyWith() {
        FileSystemConfiguration base = new FileSystemConfiguration( Map.of(
            "fs.s3.identity", "base-id",
            "fs.s3.region", "us-east-1",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( Map.of(
            "fs.s3.identity", "override-id"
        ) );

        assertThat( merged.get( "s3", "primary", "identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "primary", "region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "primary", "identity" ) ).isEqualTo( "base-id" );
    }

    @Test
    public void testCopyWithFileSystemConfiguration() {
        FileSystemConfiguration base = new FileSystemConfiguration( Map.of(
            "fs.s3.identity", "base-id",
            "fs.s3.region", "us-east-1",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration overrides = new FileSystemConfiguration( Map.of(
            "fs.s3.identity", "override-id",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( overrides );

        assertThat( merged.get( "s3", "primary", "identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "primary", "region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "primary", "identity" ) ).isEqualTo( "base-id" );
    }
}
