package oap.hadoop;

import org.apache.hadoop.fs.FileSystem;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class OapHadoopConfigurationTest {
    @Test
    public void testGetPathFile() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.FILE,
            Map.of( "fs.file.root", "/tmp/root" ) );

        assertThat( oapHadoopConfiguration.getPath( "folder/file.txt" ) )
            .isEqualTo( "file:///tmp/root/folder/file.txt" );

        assertThatCode( () -> FileSystem.get( oapHadoopConfiguration ) ).doesNotThrowAnyException();
    }

    @Test
    public void testGetPathS3a() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.S3A,
            Map.of( "fs.s3a.endpoint", "s3a://s3-website.us-east-1.amazonaws.com/my-bucket" ) );

        assertThat( oapHadoopConfiguration.getPath( "folder/file.txt" ).toString() )
            .isEqualTo( "s3a://s3-website.us-east-1.amazonaws.com/my-bucket/folder/file.txt" );

        assertThatCode( () -> FileSystem.get( oapHadoopConfiguration ) ).doesNotThrowAnyException();
    }

    @Test
    public void testGetPathSftp() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.SFTP,
            Map.of( "fs.sftp.hostname", "hostname", "fs.sftp.user", "user1" ) );

        assertThat( oapHadoopConfiguration.getPath( "folder/file.txt" ).toString() )
            .isEqualTo( "sftp://user1@hostname/folder/file.txt" );

        oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.SFTP,
            Map.of( "fs.sftp.hostname", "hostname", "fs.sftp.user", "user1", "fs.sftp.port", "33" ) );

        assertThat( oapHadoopConfiguration.getPath( "folder/file.txt" ).toString() )
            .isEqualTo( "sftp://user1@hostname:33/folder/file.txt" );

        OapHadoopConfiguration finalOapHadoopConfiguration = oapHadoopConfiguration;
        assertThatCode( () -> FileSystem.get( finalOapHadoopConfiguration ) ).doesNotThrowAnyException();
    }

    @Test
    public void testMapOfMap() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.S3A,
            Map.of( "fs.s3a.endpoint", "s3a://s3-website.us-east-1.amazonaws.com/my-bucket" ) );

        assertThat( oapHadoopConfiguration.getPath( "folder/file.txt" ).toString() )
            .isEqualTo( "s3a://s3-website.us-east-1.amazonaws.com/my-bucket/folder/file.txt" );
    }
}
