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
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;
import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemSmbTest extends Fixtures {
    public static final String CONFIGURATION_ID = "smb";
    private static final SambaServerFixture smbFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-smb-client" ) );
        smbFixture = suiteFixture( new SambaServerFixture() );
    }

    public FileSystemSmbTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    private static CloudURI smbUri( String path ) {
        return new CloudURI( CONFIGURATION_ID, path );
    }

    @BeforeMethod
    public void beforeMethod() {
        smbFixture.reset();
    }

    @Test
    public void testToUri() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.toUri( smbUri( "logs/file.txt" ) ) )
                .isEqualTo( s( "smb://${smbFixture.container()}/logs/file.txt" ) );
        }
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
            fileSystem.copy( fileSystem.toLocalFileURI( "file", path ), smbUri( "logs/my-file.txt.gz" ), Map.of() );

            InputStream inputStream = fileSystem.getInputStream( smbUri( "logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return smbFixture.getFileSystemConfiguration( CONFIGURATION_ID );
    }

    @Test
    public void testExistsListAndDelete() {
        smbFixture.writeFile( "logs/file1.txt", "1", ContentWriter.ofString() );
        smbFixture.writeFile( "logs/file2.txt", "2", ContentWriter.ofString() );
        smbFixture.createDirectory( "logs/folder1" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.blobExists( smbUri( "logs/file1.txt" ) ) ).isTrue();
            assertThat( fileSystem.blobExists( smbUri( "logs/file2.txt" ) ) ).isTrue();
            assertThat( fileSystem.containerExists( smbUri( "" ) ) ).isTrue();

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertThat( list.get( 0 ).getLastModified() ).isNotNull();
            assertThat( list.get( 0 ).getName() ).isEqualTo( "logs/file1.txt" );
            assertThat( list.get( 0 ).getUri() ).isEqualTo( URI.create( "fs://" + CONFIGURATION_ID + "/logs/file1.txt" ) );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/file1.txt" );
            listP = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/file2.txt" );

            fileSystem.deleteBlob( smbUri( "logs/file1.txt" ) );

            assertThat( fileSystem.blobExists( smbUri( "logs/file1.txt" ) ) ).isFalse();
            assertThat( fileSystem.blobExists( smbUri( "logs/file2.txt" ) ) ).isTrue();
            assertThat( fileSystem.list( smbUri( "logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );
        }
    }

    @Test
    public void testListRecursesIntoNestedFolders() {
        smbFixture.writeFile( "logs/a.txt", "a", ContentWriter.ofString() );
        smbFixture.writeFile( "logs/sub1/b.txt", "b", ContentWriter.ofString() );
        smbFixture.writeFile( "logs/sub1/sub2/c.txt", "c", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( smbUri( "logs/" ), ListOptions.builder().build() );

            assertThat( list.size() ).isEqualTo( 3 );
            assertThat( list.get( 0 ).getName() ).isEqualTo( "logs/a.txt" );
            assertThat( list.get( 0 ).getUri() ).isEqualTo( URI.create( "fs://" + CONFIGURATION_ID + "/logs/a.txt" ) );
            assertThat( list.get( 1 ).getName() ).isEqualTo( "logs/sub1/b.txt" );
            assertThat( list.get( 1 ).getUri() ).isEqualTo( URI.create( "fs://" + CONFIGURATION_ID + "/logs/sub1/b.txt" ) );
            assertThat( list.get( 2 ).getName() ).isEqualTo( "logs/sub1/sub2/c.txt" );
            assertThat( list.get( 2 ).getUri() ).isEqualTo( URI.create( "fs://" + CONFIGURATION_ID + "/logs/sub1/sub2/c.txt" ) );
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
    public void testBasedir() {
        FileSystemConfiguration configuration = getFileSystemConfiguration()
            .copyWith( Map.of( "fs.smb.filesystem.basedir", "sub/dir" ) );

        try( FileSystem fileSystem = new FileSystem( configuration ) ) {
            fileSystem.upload( smbUri( "file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( smbFixture.readFile( "sub/dir/file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );

            assertThat( fileSystem.toUri( smbUri( "file.txt" ) ) )
                .isEqualTo( s( "smb://${smbFixture.container()}/sub/dir/file.txt" ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( smbUri( "" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 1 );
            assertThat( list.get( 0 ).getName() ).isEqualTo( "file.txt" );

            fileSystem.deleteBlob( smbUri( "file.txt" ) );
            assertThat( fileSystem.blobExists( smbUri( "file.txt" ) ) ).isFalse();
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
