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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static dev.khbd.interp4j.core.Interpolations.s;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemFtpTest extends Fixtures {
    public static final String CONFIGURATION_ID = "ftp";
    private static final FtpFixture ftpFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-ftp-client" ) );
        ftpFixture = suiteFixture( new FtpFixture() );
    }

    public FileSystemFtpTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    private static CloudURI ftpUri( String path ) {
        return new CloudURI( CONFIGURATION_ID, path );
    }

    @BeforeMethod
    public void beforeMethod() {
        Files.delete( ftpFixture.homeDirectory() );
        Files.ensureDirectory( ftpFixture.homeDirectory() );
    }

    @Test
    public void testGetDefaultURL() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getDefaultURL( CONFIGURATION_ID, "/a.file" ) ).isEqualTo( ftpUri( "a.file" ) );
            assertThat( fileSystem.getDefaultURL( CONFIGURATION_ID, "a.file" ) ).isEqualTo( ftpUri( "a.file" ) );
        }
    }

    @Test
    public void testToUri() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.toUri( ftpUri( "logs/file.txt" ) ) ).isEqualTo( "ftp://" + ftpFixture.hostPort() + "/" + CONFIGURATION_ID + "/logs/file.txt" );
        }
    }

    @Test
    public void testResolveWithConfigurationId() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            CloudURI resolved = fileSystem.resolve( "other-configuration-id", "ftp://" + ftpFixture.hostPort() + "/logs/file.txt" );

            assertThat( resolved ).isEqualTo( new CloudURI( "other-configuration-id", "logs/file.txt" ) );
        }
    }

    @Test( expectedExceptions = NullPointerException.class )
    public void testResolveWithConfigurationIdRequiresNonNull() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.resolve( null, "ftp://" + ftpFixture.hostPort() + "/logs/file.txt" );
        }
    }

    @Test
    public void testGetInputStream() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            InputStream inputStream = fileSystem.getInputStream( ftpUri( "logs/file.txt" ) );

            assertThat( inputStream ).hasContent( "test string" );
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            try( OutputStream outputStream = fileSystem.getOutputStream( ftpUri( "logs/file.txt" ), Map.of() ) ) {
                outputStream.write( "1".getBytes() );
                outputStream.write( "23".getBytes() );
                outputStream.write( "567".getBytes() );
            }

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "logs/file.txt", ContentReader.ofString() ) ).isEqualTo( "123567" );
        }
    }

    @Test
    public void testGetMetadata() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            FileSystem.StorageItem item = fileSystem.getMetadata( ftpUri( "logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );

            assertThat( fileSystem.getMetadata( ftpUri( "unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.downloadFile( ftpUri( "logs/file.txt" ), testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Path path = testDirectoryFixture.testPath( "folder/my-file.txt.gz" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.copy( fileSystem.toLocalFileURI( "file", path ), ftpUri( "logs/my-file.txt.gz" ), Map.of() );

            InputStream inputStream = fileSystem.getInputStream( ftpUri( "logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID );
    }

    @Test
    public void testExistsListAndDelete() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file1.txt", "1", ContentWriter.ofString() );
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file2.txt", "2", ContentWriter.ofString() );
        ftpFixture.createDirectory( CONFIGURATION_ID, "logs/folder1" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) ).isTrue();
            assertThat( fileSystem.blobExists( ftpUri( "logs/file2.txt" ) ) ).isTrue();
            assertThat( fileSystem.containerExists( ftpUri( "" ) ) ).isTrue();

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertThat( list.get( 0 ).getLastModified() ).isNotNull();
            assertThat( list.get( 0 ).getName() ).isEqualTo( "logs/file1.txt" );
            assertThat( list.get( 0 ).getUri() ).isEqualTo( URI.create( "fs://" + CONFIGURATION_ID + "/logs/file1.txt" ) );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/file1.txt" );
            listP = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertThat( listP.get( 0 ).getName() ).isEqualTo( "logs/file2.txt" );

            fileSystem.deleteBlob( ftpUri( "logs/file1.txt" ) );

            assertThat( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) ).isFalse();
            assertThat( fileSystem.blobExists( ftpUri( "logs/file2.txt" ) ) ).isTrue();
            assertThat( fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );
        }
    }

    @Test
    public void testListRecursesIntoNestedFolders() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/a.txt", "a", ContentWriter.ofString() );
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/sub1/b.txt", "b", ContentWriter.ofString() );
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/sub1/sub2/c.txt", "c", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().build() );

            assertThat( list.size() ).isEqualTo( 3 );
            assertThat( list.get( 0 ).getName() ).isEqualTo( "logs/a.txt" );
            assertThat( list.get( 0 ).getUri() ).isEqualTo( URI.create( s( "fs://${CONFIGURATION_ID}/logs/a.txt" ) ) );
            assertThat( list.get( 1 ).getName() ).isEqualTo( "logs/sub1/b.txt" );
            assertThat( list.get( 1 ).getUri() ).isEqualTo( URI.create( s( "fs://${CONFIGURATION_ID}/logs/sub1/b.txt" ) ) );
            assertThat( list.get( 2 ).getName() ).isEqualTo( "logs/sub1/sub2/c.txt" );
            assertThat( list.get( 2 ).getUri() ).isEqualTo( URI.create( s( "fs://${CONFIGURATION_ID}/logs/sub1/sub2/c.txt" ) ) );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( "content".getBytes( UTF_8 ) ).build() );

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadFile() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path source = testDirectoryFixture.testPath( "upload/file.txt" );
            Files.write( source, "content", ContentWriter.ofString() );

            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( source.toFile() ).build() );
            fileSystem.upload( ftpUri( "file.txt2" ), BlobData.builder().content( source.toFile() ).build() );

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
            assertThat( fileSystem.getInputStream( ftpUri( "file.txt" ) ) ).hasContent( "content" );

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt2", ContentReader.ofString() ) ).isEqualTo( "content" );
            assertThat( fileSystem.getInputStream( ftpUri( "file.txt2" ) ) ).hasContent( "content" );
        }
    }

    @Test
    public void testUploadPath() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            Path source = testDirectoryFixture.testPath( "upload/file.txt" );
            Files.write( source, "content", ContentWriter.ofString() );

            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( source ).build() );
            fileSystem.upload( ftpUri( "file.txt2" ), BlobData.builder().content( source ).build() );
            fileSystem.upload( ftpUri( "file.txt3" ), BlobData.builder().content( source ).build() );

            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
            assertThat( fileSystem.getInputStream( ftpUri( "file.txt" ) ) ).hasContent( "content" );
            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt2", ContentReader.ofString() ) ).isEqualTo( "content" );
            assertThat( fileSystem.getInputStream( ftpUri( "file.txt2" ) ) ).hasContent( "content" );
            assertThat( ftpFixture.readFile( CONFIGURATION_ID, "file.txt3", ContentReader.ofString() ) ).isEqualTo( "content" );
            assertThat( fileSystem.getInputStream( ftpUri( "file.txt3" ) ) ).hasContent( "content" );
        }
    }

    @Test
    public void testFolder() {
        ftpFixture.createDirectory( CONFIGURATION_ID, "folder" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getMetadata( ftpUri( "folder" ) ).getContentType() ).isEqualTo( "application/x-directory" );
        }
    }

    @Test
    public void testDeleteFileAndParentFolderIfEmpty() {
        // 1. remove_empty_folders disabled -> parent folders remain after delete
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( ftpUri( "case1/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( ftpUri( "case1/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case1/folder1/folder2/file.txt" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case1/folder1/folder2" ) ).exists();
        }

        // 2. enabled -> empty folder2 removed, folder1 kept (still has file2.txt)
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID, true ) ) ) {
            fileSystem.upload( ftpUri( "case2/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );
            fileSystem.upload( ftpUri( "case2/folder1/file2.txt" ), BlobData.builder().content( "content2" ).build() );

            fileSystem.deleteBlob( ftpUri( "case2/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case2/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case2/folder1" ) ).exists();
        }

        // 3. enabled -> whole empty chain removed
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID, true ) ) ) {
            fileSystem.upload( ftpUri( "case3/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( ftpUri( "case3/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case3/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( CONFIGURATION_ID, "case3/folder1" ) ).doesNotExist();
        }
    }

    @Test
    public void testPoolReusesConnectionSequentially() {
        ftpFixture.writeFile( CONFIGURATION_ID, "logs/file1.txt", "1", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID, false, 1 ) ) ) {
            for( int i = 0; i < 5; i++ ) {
                assertThat( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) ).isTrue();
            }
        }
    }

    @Test
    public void testPoolHandlesConcurrentUploads() {
        int poolMaxSize = 2;
        int uploads = 10;

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID, false, poolMaxSize ) ) ) {
            ExecutorService executor = Executors.newFixedThreadPool( uploads );
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for( int i = 0; i < uploads; i++ ) {
                    int idx = i;
                    futures.add( CompletableFuture.runAsync( () ->
                        fileSystem.upload( ftpUri( s( "concurrent/file${idx}.txt" ) ),
                            BlobData.builder().content( s( "content${idx}" ) ).build() ), executor ) );
                }

                assertThat( CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) ) )
                    .succeedsWithin( 30, TimeUnit.SECONDS );
            } finally {
                executor.shutdown();
            }
        }

        for( int i = 0; i < uploads; i++ ) {
            assertThat( ftpFixture.readFile( CONFIGURATION_ID, s( "concurrent/file${i}.txt" ), ContentReader.ofString() ) ).isEqualTo( "content" + i );
        }
    }

    @Test
    public void testPoolHandlesManyParallelUploads() {
        int poolMaxSize = 8;
        int uploads = 1000;

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( CONFIGURATION_ID, false, poolMaxSize ) ) ) {
            ExecutorService executor = Executors.newFixedThreadPool( 50 );
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for( int i = 0; i < uploads; i++ ) {
                    int idx = i;
                    futures.add( CompletableFuture.runAsync( () ->
                        fileSystem.upload( ftpUri( s( "bulk/file${idx}.txt" ) ),
                            BlobData.builder().content( s( "content${idx}" ) ).build() ), executor ) );
                }

                assertThat( CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) ) )
                    .succeedsWithin( 120, TimeUnit.SECONDS );
            } finally {
                executor.shutdown();
            }

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( ftpUri( "bulk/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( uploads );
        }

        for( int idx : new int[] { 0, 1, 500, 998, 999 } ) {
            assertThat( ftpFixture.readFile( CONFIGURATION_ID, s( "bulk/file${idx}.txt" ), ContentReader.ofString() ) ).isEqualTo( "content" + idx );
        }
    }

    @Test
    public void testBasedir() {
        FileSystemConfiguration configuration = getFileSystemConfiguration()
            .copyWith( Map.of( s( "fs.${CONFIGURATION_ID}.filesystem.basedir.${CONFIGURATION_ID}" ), "sub/dir" ) );

        try( FileSystem fileSystem = new FileSystem( configuration ) ) {
            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( Files.read( ftpFixture.homeDirectory().resolve( "sub/dir/file.txt" ), ContentReader.ofString() ) ).isEqualTo( "content" );

            assertThat( fileSystem.toUri( ftpUri( "file.txt" ) ) ).isEqualTo( s( "ftp://${ftpFixture.hostPort()}/sub/dir/file.txt" ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( ftpUri( "" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 1 );
            assertThat( list.get( 0 ).getName() ).isEqualTo( "file.txt" );

            fileSystem.deleteBlob( ftpUri( "file.txt" ) );
            assertThat( fileSystem.blobExists( ftpUri( "file.txt" ) ) ).isFalse();
        }
    }

    @Test
    public void testDifferentHostsUseIndependentConnectionPools() {
        ftpFixture.writeFile( CONFIGURATION_ID, "shared/file.txt", "primary", ContentWriter.ofString() );

        FtpFixture secondFtpFixture = new FtpFixture();
        secondFtpFixture.before();
        try {
            secondFtpFixture.writeFile( "secondary", "shared/file.txt", "secondary", ContentWriter.ofString() );

            FileSystemConfiguration config = secondFtpFixture.updateWithFtp(
                getFileSystemConfiguration(), "secondary", false, null );

            try( FileSystem fileSystem = new FileSystem( config ) ) {
                CloudURI primaryUri = ftpUri( "shared/file.txt" );
                CloudURI secondaryUri = new CloudURI( "secondary", "shared/file.txt" );

                assertThat( fileSystem.getInputStream( primaryUri ) ).hasContent( "primary" );
                assertThat( fileSystem.getInputStream( secondaryUri ) ).hasContent( "secondary" );
                // re-read primary to prove the pooled connection wasn't reassigned to the second host
                assertThat( fileSystem.getInputStream( primaryUri ) ).hasContent( "primary" );
            }
        } finally {
            secondFtpFixture.after();
        }
    }
}
