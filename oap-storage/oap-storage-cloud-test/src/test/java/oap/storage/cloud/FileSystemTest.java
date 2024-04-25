package oap.storage.cloud;

import oap.io.Files;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.content.ContentReader.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileSystemTest extends Fixtures {
    public static final String TEST_BUCKET = "test-bucket";
    private final S3MockFixture s3mockFixture;
    private final TestDirectoryFixture testDirectoryFixture;

    public FileSystemTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        s3mockFixture = fixture( new S3MockFixture() ).withInitialBuckets( TEST_BUCKET, "test2" );
    }

    @Test
    public void testGetDefaultURL() {
        FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( Map.of(
            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", TEST_BUCKET
        ) ) );

        assertThat( fileSystem.getDefaultURL( "/a.file" ) ).isEqualTo( new CloudURI( "s3", TEST_BUCKET, "a.file" ) );
        assertThat( fileSystem.getDefaultURL( "a.file" ) ).isEqualTo( new CloudURI( "s3", TEST_BUCKET, "a.file" ) );
    }

    @Test
    public void testGetInputStream() {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() );

        s3mockFixture.uploadFile( TEST_BUCKET, "logs/file.txt", path, Map.of( "test-tag", "tag-val" ) );

        CloudInputStream inputStream = fileSystem.getInputStream( "s3://" + TEST_BUCKET + "/logs/file.txt" );

        assertThat( inputStream ).hasContent( "test string" );
        assertThat( s3mockFixture.readTags( TEST_BUCKET, "logs/file.txt" ) ).contains( entry( "test-tag", "tag-val" ) );
    }

    @Test
    public void testDownloadFile() {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() );

        s3mockFixture.uploadFile( TEST_BUCKET, "logs/file.txt", path, Map.of( "test-tag", "tag-val" ) );

        fileSystem.downloadFile( "s3://" + TEST_BUCKET + "/logs/file.txt", testDirectoryFixture.testPath( "file.txt" ) );

        assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
    }

    @Test
    public void testCopy() {
        Files.write( testDirectoryFixture.testPath( "folder/my-file.txt.gz" ), "test string", ContentWriter.ofString() );

        FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() );

        fileSystem.copy( "file://folder/my-file.txt.gz", "s3://" + TEST_BUCKET + "/logs/my-file.txt.gz",
            Map.of(), Map.of( "tag1", "va1", "tag2=&+", "val2=&+" ) );

        CloudInputStream inputStream = fileSystem.getInputStream( "s3://" + TEST_BUCKET + "/logs/my-file.txt.gz" );

        assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );

        assertThat( s3mockFixture.readFile( TEST_BUCKET, "logs/my-file.txt.gz", ofString() ) ).isEqualTo( "test string" );

        assertThat( s3mockFixture.readTags( TEST_BUCKET, "logs/my-file.txt.gz" ) ).contains(
            entry( "tag1", "va1" ),
            entry( "tag2=&+", "val2=&+" )
        );
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return new FileSystemConfiguration( Map.of(
            "fs.s3.clouds.identity", "access_key",
            "fs.s3.clouds.credential", "access_secret",
            "fs.s3.clouds.s3.virtual-host-buckets", false,
            "fs.s3.clouds.endpoint", "http://localhost:" + s3mockFixture.getPort(),
            "fs.s3.clouds.headers", "DEBUG",

            "fs.file.clouds.filesystem.basedir", testDirectoryFixture.testDirectory(),

            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", TEST_BUCKET
        ) );
    }

    @Test
    public void testToLocalFilePath() {
        FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( Map.of(
            "fs.file.clouds.filesystem.basedir", testDirectoryFixture.testDirectory(),

            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", TEST_BUCKET
        ) ) );

        assertThat( fileSystem.toLocalFilePath( testDirectoryFixture.testPath( "/container/test.file" ) ) )
            .isEqualTo( new CloudURI( "file", "container", "test.file" ) );
    }

    @Test
    public void testExistsListAndDelete() {
        Path path1 = testDirectoryFixture.testPath( "folder/my-file.txt" );
        Path path2 = testDirectoryFixture.testPath( "folder/my-file2.txt" );

        Files.write( path1, "1", ContentWriter.ofString() );
        Files.write( path2, "2", ContentWriter.ofString() );

        FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() );

        s3mockFixture.uploadFile( "test2", "logs/file1.txt", path1 );
        s3mockFixture.uploadFile( "test2", "logs/file2.txt", path2 );

        assertTrue( fileSystem.blobExists( "s3://test2/logs/file1.txt" ) );
        assertTrue( fileSystem.blobExists( "s3://test2/logs/file2.txt" ) );
        assertTrue( fileSystem.containerExists( "s3://test2" ) );
        var list = fileSystem.list("s3://test2/logs/" );
        assertThat( list.size() ).isEqualTo( 2 );
        assertNotNull( list.stream().toList().get( 0 ).getLastModified() );
        assertEquals( "logs/file1.txt", list.stream().toList().get( 0 ).getName() );

        fileSystem.deleteBlob( "s3://test2/logs/file1.txt" );

        assertFalse( fileSystem.blobExists( "s3://test2/logs/file1.txt" ) );
        assertTrue( fileSystem.blobExists( "s3://test2/logs/file2.txt" ) );
        assertTrue( fileSystem.containerExists( "s3://test2" ) );
        assertThat( fileSystem.list( "s3://test2/logs/" ).size() ).isEqualTo( 1 );

        assertFalse( fileSystem.deleteContainerIfEmpty( "s3://test2" ) );
        fileSystem.deleteContainer( "s3://test2" );

        assertFalse( fileSystem.blobExists( "s3://test2/logs/file1.txt" ) );
        assertFalse( fileSystem.blobExists( "s3://test2/logs/file2.txt" ) );
        assertFalse( fileSystem.containerExists( "s3://test2" ) );
        assertTrue( fileSystem.containerExists( "s3://" + TEST_BUCKET ) );
    }

    @Test
    public void testToFile() {
        FileSystemConfiguration fileSystemConfiguration = new FileSystemConfiguration(
            Map.of(
                "fs.file.clouds.filesystem.basedir", "/tmp",
                "fs.file.tmp.clouds.filesystem.basedir", "/container",
                "fs.default.clouds.scheme", "s3",
                "fs.default.clouds.container", "test-bucket"
            )
        );

        FileSystem fileSystem = new FileSystem( fileSystemConfiguration );

        assertThat( fileSystem.toFile( new CloudURI( "file://container/a/file1" ) ) ).isEqualTo( new File( "/tmp/container/a/file1" ) );
        assertThat( fileSystem.toFile( new CloudURI( "file://tmp/a/file1" ) ) ).isEqualTo( new File( "/container/tmp/a/file1" ) );
    }

    @Test
    public void testUploadByteArray() {
        FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() );

        fileSystem.upload( "s3://test-bucket/file.txt", "content".getBytes( UTF_8 ), Map.of(), Map.of() );

        assertThat( s3mockFixture.readFile( TEST_BUCKET, "file.txt", ofString() ) ).isEqualTo( "content" );
    }
}
