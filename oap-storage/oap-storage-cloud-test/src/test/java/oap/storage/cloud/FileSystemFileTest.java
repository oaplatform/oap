package oap.storage.cloud;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Slf4j
public class FileSystemFileTest extends Fixtures {
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture() );

    }

    public final Path basedir;

    public FileSystemFileTest() {
        fixture( new SystemTimerFixture( true ) );

        basedir = testDirectoryFixture.testPath( "basedir" );
    }

    @Test
    public void testGetDefaultURL() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getDefaultURL( "/a.file" ) ).isEqualTo( new CloudURI( "file", "", "a.file" ) );
            assertThat( fileSystem.getDefaultURL( "a.file" ) ).isEqualTo( new CloudURI( "file", "", "a.file" ) );
        }
    }

    @Test
    public void testGetInputStream() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path filePath = basedir.resolve( "logs/file.txt" );
            log.debug( "file {}", filePath );
            Files.write( filePath, "test string", ContentWriter.ofString() );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "file://logs/file.txt" ) );

            assertThat( inputStream ).hasContent( "test string" );
        }
    }

    @Test
    public void testGetInputStreamWithoutBasedir() {
        try( FileSystem fileSystem = new FileSystem( new FileSystemConfiguration( Map.of(
            "fs.default.clouds.scheme", "file",
            "fs.default.clouds.container", ""
        ) ) ) ) {
            Path filePath = basedir.resolve( "logs/file.txt" );
            log.debug( "file {}", filePath );
            Files.write( filePath, "test string", ContentWriter.ofString() );

            CloudURI path = new CloudURI( "file://" + FilenameUtils.separatorsToUnix( filePath.toString().substring( 1 ) ) );
            log.debug( "path {}", path );
            InputStream inputStream = fileSystem.getInputStream( path );

            assertThat( inputStream ).hasContent( "test string" );
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            try( OutputStream outputStream = fileSystem.getOutputStream( new CloudURI( "file://logs/file.txt" ), Map.of() ) ) {
                outputStream.write( "1".getBytes() );
                outputStream.write( "23".getBytes() );
                outputStream.write( "567".getBytes() );
            }

            assertFile( basedir.resolve( "logs/file.txt" ) ).hasContent( "123567" );
        }
    }

    @Test
    public void testGetMetadata() throws URISyntaxException {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path filePath = basedir.resolve( "logs/file.txt" );
            log.debug( "file {}", filePath );
            Files.write( filePath, "test string", ContentWriter.ofString() );

            FileSystem.StorageItem item = fileSystem.getMetadata( new CloudURI( "file", "", "logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );
            assertThat( item.getUri() ).isEqualTo( filePath.toUri() );

            assertThat( fileSystem.getMetadata( new CloudURI( "file", "", "/unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path filePath = basedir.resolve( "logs/file.txt" );
            log.debug( "file {}", filePath );
            Files.write( filePath, "test string", ContentWriter.ofString() );

            fileSystem.downloadFile( "file://logs/file.txt", testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Path path = testDirectoryFixture.testPath( "folder/my-file.txt.gz" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.copyAsync( fileSystem.toLocalFilePath( path ), new CloudURI( "file://logs/my-file.txt.gz" ), Map.of() ) )
                .succeedsWithin( 30, TimeUnit.SECONDS );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "file://logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, IoStreams.Encoding.GZIP ) ).hasContent( "test string" );

            assertFile( basedir.resolve( "logs/my-file.txt.gz" ) ).hasContent( "test string", IoStreams.Encoding.GZIP );
        }
    }

    @Nonnull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return getFileSystemConfiguration( false );
    }

    @Nonnull
    private FileSystemConfiguration getFileSystemConfiguration( boolean removeEmptyFolders ) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.putAll( Map.of(
            "fs.default.clouds.scheme", "file",
            "fs.default.clouds.container", "",
            "fs.file.clouds.filesystem.basedir", basedir
        ) );

        if( removeEmptyFolders ) {
            map.put( "fs.file.clouds.filesystem.remove_empty_folders", true );
        }

        return new FileSystemConfiguration( map );
    }

    @Test
    public void testToLocalFilePath() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path path = testDirectoryFixture.testPath( "/container/test.file" );
            assertThat( fileSystem.toLocalFilePath( path ) ).isEqualTo( new CloudURI( "file", "", "../container/test.file" ) );
        }
    }

    @Test
    public void testExistsListAndDelete() {
        Path path1 = testDirectoryFixture.testPath( "folder/my-file.txt" );
        Path path2 = testDirectoryFixture.testPath( "folder/my-file2.txt" );

        Files.write( path1, "1", ContentWriter.ofString() );
        Files.write( path2, "2", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.write( basedir.resolve( "logs/test2/file1.txt" ), "1", ContentWriter.ofString() );
            Files.write( basedir.resolve( "logs/test2/file2.txt" ), "2", ContentWriter.ofString() );
            Files.ensureDirectory( basedir.resolve( "logs/test2/folder1/" ) );

            assertTrue( fileSystem.blobExists( new CloudURI( "file://logs/test2/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "file://logs/test2/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "file://logs/test2/folder1/" ) ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( new CloudURI( "file://logs/test2/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertNotNull( list.get( 0 ).getLastModified() );
            assertThat( list.get( 0 ).getName() ).isEqualTo( "logs/test2/file1.txt" );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( new CloudURI( "file://logs/test2/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/test2/file1.txt" );
            listP = fileSystem.list( new CloudURI( "file://logs/test2/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/test2/file2.txt" );

            fileSystem.deleteBlob( new CloudURI( "file://logs/test2/file1.txt" ) );

            assertFile( basedir.resolve( "logs/test2/file1.txt" ) ).doesNotExist();

            assertFalse( fileSystem.blobExists( new CloudURI( "file://logs/test2/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( new CloudURI( "file://logs/test2/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "file://logs/test2" ) ) );
            assertThat( fileSystem.list( new CloudURI( "file://logs/test2/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );

            assertFalse( fileSystem.deleteContainerIfEmpty( new CloudURI( "file://logs/test2" ) ) );
            fileSystem.deleteContainer( new CloudURI( "file://logs/test2" ) );

            assertFalse( fileSystem.blobExists( new CloudURI( "file://logs/test2/file1.txt" ) ) );
            assertFalse( fileSystem.blobExists( new CloudURI( "file://logs/test2/file2.txt" ) ) );
            assertFalse( fileSystem.containerExists( new CloudURI( "file://logs/test2" ) ) );
            assertTrue( fileSystem.containerExists( new CloudURI( "file://logs" ) ) );
        }
    }

    @Test
    public void testToFile() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.toFile( new CloudURI( "file:///tmp/a/file1" ) ) ).isEqualTo( new File( "/tmp/a/file1" ) );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "file://test-bucket/path1/path2/file.txt" ), BlobData.builder().content( "content" ).build() );

            assertFile( basedir.resolve( "test-bucket/path1/path2/file.txt" ) ).hasContent( "content" );
        }
    }

    @Test
    public void testUploadFile() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path source = testDirectoryFixture.testPath( "test/new-file.txt" );
            Files.write( source, "content", ContentWriter.ofString() );

            fileSystem.upload( new CloudURI( "file://test-bucket/path1/path2/file.txt" ), BlobData.builder().content( source ).build() );

            assertFile( basedir.resolve( "test-bucket/path1/path2/file.txt" ) ).hasContent( "content" );
        }
    }

    @Test
    public void testUploadFileOverwrite() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path source = testDirectoryFixture.testPath( "test/new-file.txt" );
            Files.write( source, "content", ContentWriter.ofString() );
            fileSystem.upload( new CloudURI( "file://test-bucket/path1/path2/file.txt" ), BlobData.builder().content( source ).build() );

            Files.write( source, "content2", ContentWriter.ofString() );
            fileSystem.upload( new CloudURI( "file://test-bucket/path1/path2/file.txt" ), BlobData.builder().content( source ).build() );

            assertFile( basedir.resolve( "test-bucket/path1/path2/file.txt" ) ).hasContent( "content2" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "file://test-bucket/file.txt" ), BlobData.builder().content( "content".getBytes( UTF_8 ) ).build() );

            assertFile( basedir.resolve( "test-bucket/file.txt" ) ).hasContent( "content" );
        }
    }

    @Test
    public void testFolder() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Files.ensureDirectory( basedir.resolve( "test-bucket/folder/" ) );

            assertThat( fileSystem.getMetadata( new CloudURI( "file://test-bucket/folder/" ) ).getContentType() ).isEqualTo( "application/x-directory" );
        }
    }

    @Test
    public void testDeleteFileAndParentFolderIfEmpty() {
        // 1. remove_empty_folders disabled -> parent folders remain after delete
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( new CloudURI( "file://case1/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( new CloudURI( "file://case1/folder1/folder2/file.txt" ) );

            assertFile( basedir.resolve( "case1/folder1/folder2/file.txt" ) ).doesNotExist();
            assertThat( basedir.resolve( "case1/folder1/folder2" ) ).exists();
        }

        // 2. enabled -> empty folder2 removed, folder1 kept (still has file2.txt)
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration( true ) ) ) {
            fileSystem.upload( new CloudURI( "file://case2/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );
            fileSystem.upload( new CloudURI( "file://case2/folder1/file2.txt" ), BlobData.builder().content( "content2" ).build() );

            fileSystem.deleteBlob( new CloudURI( "file://case2/folder1/folder2/file.txt" ) );

            assertThat( basedir.resolve( "case2/folder1/folder2" ) ).doesNotExist();
            assertThat( basedir.resolve( "case2/folder1" ) ).exists();
        }

        // 3. enabled -> whole empty chain removed up to basedir
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration( true ) ) ) {
            fileSystem.upload( new CloudURI( "file://case3/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( new CloudURI( "file://case3/folder1/folder2/file.txt" ) );

            assertThat( basedir.resolve( "case3/folder1/folder2" ) ).doesNotExist();
            assertThat( basedir.resolve( "case3/folder1" ) ).doesNotExist();
        }
    }
}
