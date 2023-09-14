package oap.hadoop;

import oap.io.IoStreams;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemTest extends Fixtures {
    private static final S3MockFixture s3MockFixture;

    static {
        s3MockFixture = suiteFixture( new S3MockFixture().withInitialBuckets( "test-bucket" ) );
    }

    public FileSystemTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void testCopy() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.S3A,
            Map.of( "fs.s3a.endpoint", "http://localhost:" + s3MockFixture.getPort(),
                "fs.s3a.bucket", "test-bucket",
                "fs.s3a.path.style.access", "true",
                "fs.s3a.access.key", "foo",
                "fs.s3a.secret.key", "bar",
                "fs.s3a.change.detection.mode", "none"
            ) );

        FileSystem fileSystem = new FileSystem( oapHadoopConfiguration );
        Path inFile = TestDirectoryFixture.testPath( "folder/file.txt" );
        Path outFile = TestDirectoryFixture.testPath( "file-out.txt" );
        oap.io.Files.write( inFile, IoStreams.Encoding.PLAIN, "txt", ContentWriter.ofString() );
        var s3File = oapHadoopConfiguration.getPath( "folder/file.txt" );
        assertThat( s3File ).isEqualTo( new org.apache.hadoop.fs.Path( "s3a://test-bucket/folder/file.txt" ) );

        assertThat( fileSystem.exists( s3File ) ).isFalse();
        fileSystem.copy( inFile, s3File, false );
        assertThat( fileSystem.exists( s3File ) ).isTrue();
        assertThat( s3MockFixture.readFile( "test-bucket", "folder/file.txt" ) ).isEqualTo( "txt" );

        fileSystem.copy( s3File, outFile, false );

        assertThat( outFile ).hasContent( "txt" );
    }

    @Test
    public void testgetInputStreamFileNotFound() {
        OapHadoopConfiguration oapHadoopConfiguration = new OapHadoopConfiguration( OapFileSystemType.S3A,
            Map.of( "fs.s3a.endpoint", "http://localhost:" + s3MockFixture.getPort(),
                "fs.s3a.bucket", "test-bucket",
                "fs.s3a.path.style.access", "true",
                "fs.s3a.access.key", "foo",
                "fs.s3a.secret.key", "bar",
                "fs.s3a.change.detection.mode", "none"
            ) );

        FileSystem fileSystem = new FileSystem( oapHadoopConfiguration );

        assertThat( fileSystem.getInputStream( new org.apache.hadoop.fs.Path( "s3a://test-bucket/folder/file.txt" ) ) ).isNull();
    }
}
