package oap.storage.cloud;

import oap.system.Env;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemConfigurationTest {
    @Test
    public void testThreeTierFallback() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "primary",
            "fs.ftp.container", "ftp.example.com:21",
            "fs.ftp.identity", "shared-user",
            "fs.ftp.credential", "shared-pass",
            "fs.ftp.container.secondary", "ftp.example.com:21",
            "fs.ftp.identity.secondary", "other-user",
            "fs.ftp.credential.secondary", "other-pass"
        ) );

        // alias-specific override wins
        assertThat( fileSystemConfiguration.get( "ftp", "secondary", "identity" ) ).isEqualTo( "other-user" );
        // no alias-specific override -> falls to scheme-wide
        assertThat( fileSystemConfiguration.get( "ftp", "secondary", "container" ) ).isEqualTo( "ftp.example.com:21" );
        assertThat( fileSystemConfiguration.get( "ftp", "primary", "identity" ) ).isEqualTo( "shared-user" );
        // no property at all for this alias/scheme -> falls to fs.default.<property> (absent here -> null)
        assertThat( fileSystemConfiguration.get( "ftp", "primary", "domain" ) ).isNull();
    }

    @Test
    public void testFallsBackToFsDefaultProperty() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "primary",
            "fs.default.identity", "default-identity",
            "fs.s3.container", "my-bucket"
        ) );

        // neither fs.s3.identity.primary nor fs.s3.identity is declared -> falls all the way to fs.default.identity
        assertThat( fileSystemConfiguration.get( "s3", "primary", "identity" ) ).isEqualTo( "default-identity" );
    }

    @Test
    public void testGetDefaultAlias() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "my-alias",
            "fs.s3.container.my-alias", "test-bucket"
        ) );

        assertThat( fileSystemConfiguration.getDefaultAlias() ).isEqualTo( "my-alias" );
    }

    @Test( expectedExceptions = NullPointerException.class )
    public void testGetDefaultAliasIsRequired() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.s3.container", "test-bucket"
        ) );

        fileSystemConfiguration.getDefaultAlias();
    }

    @Test
    public void testFindSchemeAndGetScheme() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "primary",
            "fs.s3.container.primary", "bucket-a",
            "fs.s3.container.secondary", "bucket-b"
        ) );

        assertThat( fileSystemConfiguration.getScheme( "primary" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getScheme( "secondary" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.findScheme( "unknown-alias" ) ).isEqualTo( Optional.empty() );
    }

    @Test( expectedExceptions = CloudException.class )
    public void testGetSchemeThrowsForUnknownAlias() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "primary",
            "fs.s3.container", "bucket-a"
        ) );

        fileSystemConfiguration.getScheme( "unknown-alias" );
    }

    @Test
    public void testFindAliasByContainer() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "s3",
            "fs.s3.container", "bucket-a",
            "fs.s3.container.secondary", "bucket-b"
        ) );

        // exact per-alias container match
        assertThat( fileSystemConfiguration.findAliasByContainer( "s3", "bucket-b" ) ).isEqualTo( Optional.of( "secondary" ) );
        // scheme-wide container match -> falls back to the scheme's own name as the alias
        assertThat( fileSystemConfiguration.findAliasByContainer( "s3", "bucket-a" ) ).isEqualTo( Optional.of( "s3" ) );
        // no match at all
        assertThat( fileSystemConfiguration.findAliasByContainer( "s3", "bucket-c" ) ).isEqualTo( Optional.empty() );
        // unknown scheme entirely
        assertThat( fileSystemConfiguration.findAliasByContainer( "ftp", "host:21" ) ).isEqualTo( Optional.empty() );
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
                "fs.default.alias", "primary",
                "fs.s3.container", "test-bucket"
            )
        );

        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test" ) ).isEqualTo( "s3" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test2" ) ).isEqualTo( "file" );
        assertThat( fileSystemConfiguration.getOrThrow( "s3", "primary", "test3" ) ).isEqualTo( "${env.unknown}-${unknown}" );
    }

    @Test
    public void testAliasContainingDotIsSafeWithNoEscaping() {
        // the new lookup mechanism probes exact key strings ("property" + "." + alias) rather than
        // positionally splitting stored keys, so a dot inside an alias name needs no escaping at all.
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration( Map.of(
            "fs.default.alias", "my.alias",
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
            "fs.default.alias", "primary",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( Map.of(
            "fs.s3.identity", "override-id"
        ) );

        assertThat( merged.get( "s3", "primary", "identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "primary", "region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "primary", "identity" ) ).isEqualTo( "base-id" );
        assertThat( merged.getDefaultAlias() ).isEqualTo( "primary" );
    }

    @Test
    public void testCopyWithFileSystemConfiguration() {
        FileSystemConfiguration base = new FileSystemConfiguration( Map.of(
            "fs.s3.identity", "base-id",
            "fs.s3.region", "us-east-1",
            "fs.default.alias", "primary",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration overrides = new FileSystemConfiguration( Map.of(
            "fs.s3.identity", "override-id",
            "fs.default.alias", "primary",
            "fs.s3.container", "my-bucket"
        ) );

        FileSystemConfiguration merged = base.copyWith( overrides );

        assertThat( merged.get( "s3", "primary", "identity" ) ).isEqualTo( "override-id" );
        assertThat( merged.get( "s3", "primary", "region" ) ).isEqualTo( "us-east-1" );
        assertThat( base.get( "s3", "primary", "identity" ) ).isEqualTo( "base-id" );
    }
}
