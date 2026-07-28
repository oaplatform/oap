package oap.storage.cloud;

import oap.io.Files;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FileSystemFtpTest extends Fixtures {
    public static final String CONTAINER = "ftp-container";
    private static final FtpFixture ftpFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-ftp-client" ) );
        ftpFixture = suiteFixture( new FtpFixture() );
    }

    public FileSystemFtpTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    @BeforeMethod
    public void beforeMethod() {
        Files.delete( ftpFixture.homeDirectory() );
        Files.ensureDirectory( ftpFixture.homeDirectory() );
    }

    @Test
    public void testGetDefaultURL() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getDefaultURL( "/a.file" ) ).isEqualTo( new CloudURI( "ftp", CONTAINER, "a.file" ) );
            assertThat( fileSystem.getDefaultURL( "a.file" ) ).isEqualTo( new CloudURI( "ftp", CONTAINER, "a.file" ) );
        }
    }

    @Test
    public void testGetInputStream() {
        ftpFixture.writeFile( "logs/file.txt", "test string" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "ftp://" + CONTAINER + "/logs/file.txt" ) );

            assertThat( inputStream ).hasContent( "test string" );
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            try( OutputStream outputStream = fileSystem.getOutputStream( new CloudURI( "ftp://" + CONTAINER + "/logs/file.txt" ), Map.of() ) ) {
                outputStream.write( "1".getBytes() );
                outputStream.write( "23".getBytes() );
                outputStream.write( "567".getBytes() );
            }

            assertThat( ftpFixture.readFile( "logs/file.txt" ) ).isEqualTo( "123567" );
        }
    }

    @Test
    public void testGetMetadata() {
        ftpFixture.writeFile( "logs/file.txt", "test string" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            FileSystem.StorageItem item = fileSystem.getMetadata( new CloudURI( "ftp", CONTAINER, "logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );

            assertThat( fileSystem.getMetadata( new CloudURI( "ftp", CONTAINER, "unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        ftpFixture.writeFile( "logs/file.txt", "test string" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.downloadFile( "ftp://" + CONTAINER + "/logs/file.txt", testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Path path = testDirectoryFixture.testPath( "folder/my-file.txt.gz" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.copyAsync( fileSystem.toLocalFilePath( path ), new CloudURI( "ftp://" + CONTAINER + "/logs/my-file.txt.gz" ), Map.of() ) )
                .succeedsWithin( 30, TimeUnit.SECONDS );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "ftp://" + CONTAINER + "/logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return ftpFixture.getFileSystemConfiguration( CONTAINER );
    }

    @Test
    public void testExistsListAndDelete() {
        ftpFixture.writeFile( "logs/file1.txt", "1" );
        ftpFixture.writeFile( "logs/file2.txt", "2" );
        ftpFixture.createDirectory( "logs/folder1" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertTrue( fileSystem.blobExists( new CloudURI( "ftp://" + CONTAINER + "/logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "ftp://" + CONTAINER + "/logs/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "ftp://" + CONTAINER ) ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( new CloudURI( "ftp://" + CONTAINER + "/logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertNotNull( list.get( 0 ).getLastModified() );
            assertEquals( "logs/file1.txt", list.get( 0 ).getName() );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( new CloudURI( "ftp://" + CONTAINER + "/logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file1.txt", listP.get( 0 ).getName() );
            listP = fileSystem.list( new CloudURI( "ftp://" + CONTAINER + "/logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file2.txt", listP.get( 0 ).getName() );

            fileSystem.deleteBlob( new CloudURI( "ftp://" + CONTAINER + "/logs/file1.txt" ) );

            assertFalse( fileSystem.blobExists( new CloudURI( "ftp://" + CONTAINER + "/logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "ftp://" + CONTAINER + "/logs/file2.txt" ) ) );
            assertThat( fileSystem.list( new CloudURI( "ftp://" + CONTAINER + "/logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( ftpFixture.readFile( "file.txt" ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/file.txt" ), BlobData.builder().content( "content".getBytes( UTF_8 ) ).build() );

            assertThat( ftpFixture.readFile( "file.txt" ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testFolder() {
        ftpFixture.createDirectory( "folder" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getMetadata( new CloudURI( "ftp", CONTAINER, "folder" ) ).getContentType() ).isEqualTo( "application/x-directory" );
        }
    }

    @Test
    public void testDeleteFileAndParentFolderIfEmpty() {
        // 1. remove_empty_folders disabled -> parent folders remain after delete
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/case1/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( new CloudURI( "ftp://" + CONTAINER + "/case1/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( "case1/folder1/folder2/file.txt" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case1/folder1/folder2" ) ).exists();
        }

        // 2. enabled -> empty folder2 removed, folder1 kept (still has file2.txt)
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONTAINER, true ) ) ) {
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/case2/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/case2/folder1/file2.txt" ), BlobData.builder().content( "content2" ).build() );

            fileSystem.deleteBlob( new CloudURI( "ftp://" + CONTAINER + "/case2/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( "case2/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case2/folder1" ) ).exists();
        }

        // 3. enabled -> whole empty chain removed
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONTAINER, true ) ) ) {
            fileSystem.upload( new CloudURI( "ftp://" + CONTAINER + "/case3/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( new CloudURI( "ftp://" + CONTAINER + "/case3/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( "case3/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case3/folder1" ) ).doesNotExist();
        }
    }
}
