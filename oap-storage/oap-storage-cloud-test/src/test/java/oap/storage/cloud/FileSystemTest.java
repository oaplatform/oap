package oap.storage.cloud;

import oap.io.Files;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private static final S3MockFixture s3mockFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture() );
        s3mockFixture = suiteFixture( new S3MockFixture( testDirectoryFixture ) ).withInitialBuckets( TEST_BUCKET, "test2" );
    }

    public FileSystemTest() {
        fixture( new SystemTimerFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        s3mockFixture.deleteAll();
    }

    @Test
    public void testGetDefaultURL() {
        try( FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( Map.of(
            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", TEST_BUCKET
        ) ) ) ) {
            assertThat( fileSystem.getDefaultURL( "/a.file" ) ).isEqualTo( new CloudURI( "s3", TEST_BUCKET, "a.file" ) );
            assertThat( fileSystem.getDefaultURL( "a.file" ) ).isEqualTo( new CloudURI( "s3", TEST_BUCKET, "a.file" ) );
        }
    }

    @Test
    public void testGetInputStream() {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            s3mockFixture.uploadFile( TEST_BUCKET, "logs/file.txt", path, Map.of( "test-tag", "tag-val" ) );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "s3://" + TEST_BUCKET + "/logs/file.txt" ) );

            assertThat( inputStream ).hasContent( "test string" );
            assertThat( s3mockFixture.readTags( TEST_BUCKET, "logs/file.txt" ) ).contains( entry( "test-tag", "tag-val" ) );
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            try( OutputStream outputStream = fileSystem.getOutputStream( new CloudURI( "s3://" + TEST_BUCKET + "/logs/file.txt" ), Map.of( "test-tag", "tag-val" ) ) ) {
                outputStream.write( "1".getBytes() );
                outputStream.write( "23".getBytes() );
                outputStream.write( "567".getBytes() );
            }

            assertThat( s3mockFixture.readTags( TEST_BUCKET, "logs/file.txt" ) ).contains( entry( "test-tag", "tag-val" ) );
            assertThat( s3mockFixture.readFile( TEST_BUCKET, "logs/file.txt", ContentReader.ofString(), Encoding.from( "logs/file.txt" ) ) ).isEqualTo( "123567" );
        }
    }

    @Test
    public void testGetMetadata() throws URISyntaxException {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            s3mockFixture.uploadFile( TEST_BUCKET, "logs/file.txt", path, Map.of( "test-tag", "tag-val" ) );

            FileSystem.StorageItem item = fileSystem.getMetadata( new CloudURI( "s3", TEST_BUCKET, "/logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );
            assertThat( item.getUri() ).isEqualTo( new URI( "http://localhost:" + s3mockFixture.getHttpPort() + "/test-bucket/logs/file.txt" ) );
            assertThat( item.getCloudURI() ).isEqualTo( new CloudURI( "s3://test-bucket/logs/file.txt" ) );

            assertThat( fileSystem.getMetadata( new CloudURI( "s3", TEST_BUCKET, "/unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        Path path = testDirectoryFixture.testPath( "my-file.txt" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            s3mockFixture.uploadFile( TEST_BUCKET, "logs/file.txt", path, Map.of( "test-tag", "tag-val" ) );

            fileSystem.downloadFile( "s3://" + TEST_BUCKET + "/logs/file.txt", testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Files.write( testDirectoryFixture.testPath( "folder/my-file.txt.gz" ), "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.copyAsync( new CloudURI( "file://folder/my-file.txt.gz" ), new CloudURI( "s3://" + TEST_BUCKET + "/logs/my-file.txt.gz" ), Map.of( "tag1", "va1", "tag2", "val2" ) ) )
                .succeedsWithin( 30, TimeUnit.SECONDS );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "s3://" + TEST_BUCKET + "/logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );

            assertThat( s3mockFixture.readFile( TEST_BUCKET, "logs/my-file.txt.gz", ofString(), Encoding.from( "logs/my-file.txt.gz" ) ) ).isEqualTo( "test string" );

            assertThat( s3mockFixture.readTags( TEST_BUCKET, "logs/my-file.txt.gz" ) ).contains(
                entry( "tag1", "va1" ),
                entry( "tag2", "val2" )
            );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return s3mockFixture.getFileSystemConfiguration( TEST_BUCKET );
    }

    @Test
    public void testToLocalFilePath() {
        try( FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( Map.of(
            "fs.file.clouds.filesystem.basedir", testDirectoryFixture.testDirectory(),

            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", TEST_BUCKET
        ) ) ) ) {
            assertThat( fileSystem.toLocalFilePath( testDirectoryFixture.testPath( "/container/test.file" ) ) )
                .isEqualTo( new CloudURI( "file", "container", "test.file" ) );
        }
    }

    @Test
    public void testExistsListAndDelete() {
        Path path1 = testDirectoryFixture.testPath( "folder/my-file.txt" );
        Path path2 = testDirectoryFixture.testPath( "folder/my-file2.txt" );

        Files.write( path1, "1", ContentWriter.ofString() );
        Files.write( path2, "2", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            s3mockFixture.uploadFile( "test2", "logs/file1.txt", path1 );
            s3mockFixture.uploadFile( "test2", "logs/file2.txt", path2 );

            assertTrue( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "s3://test2" ) ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( new CloudURI( "s3://test2/logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertNotNull( list.get( 0 ).getLastModified() );
            assertEquals( "logs/file1.txt", list.get( 0 ).getName() );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( new CloudURI( "s3://test2/logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file1.txt", listP.get( 0 ).getName() );
            listP = fileSystem.list( new CloudURI( "s3://test2/logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file2.txt", listP.get( 0 ).getName() );

            fileSystem.deleteBlob( new CloudURI( "s3://test2/logs/file1.txt" ) );

            assertFalse( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "s3://test2" ) ) );
            assertThat( fileSystem.list( new CloudURI( "s3://test2/logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );

            assertFalse( fileSystem.deleteContainerIfEmpty( new CloudURI( "s3://test2" ) ) );
            fileSystem.deleteContainer( new CloudURI( "s3://test2" ) );

            assertFalse( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file1.txt" ) ) );
            assertFalse( fileSystem.blobExists( new CloudURI( "s3://test2/logs/file2.txt" ) ) );
            assertFalse( fileSystem.containerExists( new CloudURI( "s3://test2" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "s3://" + TEST_BUCKET ) ) );
        }
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

        try( FileSystem fileSystem = new FileSystem( fileSystemConfiguration ) ) {
            assertThat( fileSystem.toFile( new CloudURI( "file://container/a/file1" ) ) ).isEqualTo( new File( "/tmp/container/a/file1" ) );
            assertThat( fileSystem.toFile( new CloudURI( "file://tmp/a/file1" ) ) ).isEqualTo( new File( "/container/tmp/a/file1" ) );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "s3://test-bucket/file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( s3mockFixture.readFile( TEST_BUCKET, "file.txt", ofString(), Encoding.from( "file.txt" ) ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "s3://test-bucket/file.txt" ), BlobData.builder().content( "content".getBytes( UTF_8 ) ).build() );

            assertThat( s3mockFixture.readFile( TEST_BUCKET, "file.txt", ofString(), Encoding.from( "file.txt" ) ) ).isEqualTo( "content" );
        }
    }
}
