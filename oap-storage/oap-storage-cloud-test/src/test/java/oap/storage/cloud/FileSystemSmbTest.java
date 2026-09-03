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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FileSystemSmbTest extends Fixtures {
    private static final SambaServerFixture smbFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-smb-client" ) );
        smbFixture = suiteFixture( new SambaServerFixture() );
    }

    public FileSystemSmbTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    private static String container() {
        return smbFixture.container();
    }

    private static CloudURI smbUri( String path ) {
        return new CloudURI( "smb", container(), path );
    }

    @BeforeMethod
    public void beforeMethod() {
        Files.delete( smbFixture.homeDirectory() );
        Files.ensureDirectory( smbFixture.homeDirectory() );
    }

    @Test
    public void testGetInputStream() {
        smbFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            InputStream inputStream = fileSystem.getInputStream( smbUri( "logs/file.txt" ) );

            assertThat( inputStream ).hasContent( "test string" );
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            try( OutputStream outputStream = fileSystem.getOutputStream( smbUri( "logs/file.txt" ), Map.of() ) ) {
                outputStream.write( "1".getBytes() );
                outputStream.write( "23".getBytes() );
                outputStream.write( "567".getBytes() );
            }

            assertThat( smbFixture.readFile( "logs/file.txt", ContentReader.ofString() ) ).isEqualTo( "123567" );
        }
    }

    @Test
    public void testGetMetadata() {
        smbFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            FileSystem.StorageItem item = fileSystem.getMetadata( smbUri( "logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );

            assertThat( fileSystem.getMetadata( smbUri( "unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        smbFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.downloadFile( smbUri( "logs/file.txt" ).toString(), testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Path path = testDirectoryFixture.testPath( "folder/my-file.txt.gz" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.copy( fileSystem.toLocalFilePath( path ), smbUri( "logs/my-file.txt.gz" ), Map.of() );

            InputStream inputStream = fileSystem.getInputStream( smbUri( "logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return smbFixture.getFileSystemConfiguration();
    }

    @Test
    public void testExistsListAndDelete() {
        smbFixture.writeFile( "logs/file1.txt", "1", ContentWriter.ofString() );
        smbFixture.writeFile( "logs/file2.txt", "2", ContentWriter.ofString() );
        smbFixture.createDirectory( "logs/folder1" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertTrue( fileSystem.blobExists( smbUri( "logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( smbUri( "logs/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( smbUri( "" ) ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertNotNull( list.get( 0 ).getLastModified() );
            assertEquals( "logs/file1.txt", list.get( 0 ).getName() );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file1.txt", listP.get( 0 ).getName() );
            listP = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file2.txt", listP.get( 0 ).getName() );

            fileSystem.deleteBlob( smbUri( "logs/file1.txt" ) );

            assertFalse( fileSystem.blobExists( smbUri( "logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( smbUri( "logs/file2.txt" ) ) );
            assertThat( fileSystem.list( smbUri( "logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( smbUri( "file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( smbFixture.readFile( "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( smbUri( "file.txt" ), BlobData.builder().content( "content".getBytes( java.nio.charset.StandardCharsets.UTF_8 ) ).build() );

            assertThat( smbFixture.readFile( "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testFolder() {
        smbFixture.createDirectory( "folder" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getMetadata( smbUri( "folder" ) ).getContentType() ).isEqualTo( "application/x-directory" );
        }
    }
}
