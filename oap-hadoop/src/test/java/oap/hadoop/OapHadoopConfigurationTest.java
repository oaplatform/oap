package oap.hadoop;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.apache.hadoop.fs.Path;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

public class OapHadoopConfigurationTest extends Fixtures {
    public OapHadoopConfigurationTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void testGetFileSystemFile() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.FILE, Map.of() );

        assertThatCode( () -> new Path( "file:///tmp" ).getFileSystem( oapHadoopConfiguration ) ).doesNotThrowAnyException();
    }

    @Test
    public void testGetFileSystemLocalFile() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.FILE, Map.of() );

        Path path = oapHadoopConfiguration.getPath( "folder/file.txt" );

        assertThatCode( () -> new Path( "s3a://s3.us-east-1.awsamazon.com/mybucket/file.txt" ).getFileSystem( oapHadoopConfiguration ) )
            .doesNotThrowAnyException();
    }

    @Test
    public void testGetFileSystemS3a() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.S3A,
            Map.of( "fs.s3a.endpoint", "https://s3.us-east-1.awsamazon.com" ) );

        Path path = oapHadoopConfiguration.getPath( "folder/file.txt" );

        assertThatCode( () -> new Path( "s3a://s3.us-east-1.awsamazon.com/mybucket/file.txt" ).getFileSystem( oapHadoopConfiguration ) )
            .doesNotThrowAnyException();
    }

    @Test
    public void testGetFileSystemSftp() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.SFTP,
            Map.of( "fs.sftp.host", "hostname", "fs.sftp.user.hostname", "user1" ) );

        Path path = oapHadoopConfiguration.getPath( "folder/file.txt" );

        assertThatCode( () -> new Path( "sftp://hostname/folder/file.txt" ).getFileSystem( oapHadoopConfiguration ) ).doesNotThrowAnyException();
    }

    @Test
    public void testMapOfMap() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.SFTP,
            Map.of( "fs.sftp.host", "hostname", "fs", Map.of( "sftp", Map.of( "user", Map.of( "hostname", "user1" ) ) ) ) );

        assertThatCode( () -> new Path( "sftp://hostname/folder/file1" ).getFileSystem( oapHadoopConfiguration ) ).doesNotThrowAnyException();
    }
}
