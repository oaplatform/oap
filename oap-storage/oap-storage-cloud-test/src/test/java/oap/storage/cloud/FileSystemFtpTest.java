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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FileSystemFtpTest extends Fixtures {
    private static final FtpFixture ftpFixture;
    private static final TestDirectoryFixture testDirectoryFixture;

    static {
        testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-ftp-client" ) );
        ftpFixture = suiteFixture( new FtpFixture() );
    }

    public FileSystemFtpTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    private static String container() {
        return ftpFixture.hostPort();
    }

    private static CloudURI ftpUri( String path ) {
        return new CloudURI( "ftp", container(), path );
    }

    @BeforeMethod
    public void beforeMethod() {
        Files.delete( ftpFixture.homeDirectory() );
        Files.ensureDirectory( ftpFixture.homeDirectory() );
    }

    @Test
    public void testGetDefaultURL() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertThat( fileSystem.getDefaultURL( "/a.file" ) ).isEqualTo( ftpUri( "a.file" ) );
            assertThat( fileSystem.getDefaultURL( "a.file" ) ).isEqualTo( ftpUri( "a.file" ) );
        }
    }

    @Test
    public void testGetInputStream() {
        ftpFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

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

            assertThat( ftpFixture.readFile( "logs/file.txt", ContentReader.ofString() ) ).isEqualTo( "123567" );
        }
    }

    @Test
    public void testGetMetadata() {
        ftpFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            FileSystem.StorageItem item = fileSystem.getMetadata( ftpUri( "logs/file.txt" ) );
            assertThat( item.getLastModified() ).isLessThanOrEqualTo( new DateTime( DateTimeZone.UTC ) );
            assertThat( item.getSize() ).isEqualTo( 11L );

            assertThat( fileSystem.getMetadata( ftpUri( "unknown.txt" ) ) ).isNull();
        }
    }

    @Test
    public void testDownloadFile() {
        ftpFixture.writeFile( "logs/file.txt", "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.downloadFile( "ftp://" + container() + "/logs/file.txt", testDirectoryFixture.testPath( "file.txt" ) );

            assertThat( testDirectoryFixture.testPath( "file.txt" ) ).hasContent( "test string" );
        }
    }

    @Test
    public void testCopy() {
        Path path = testDirectoryFixture.testPath( "folder/my-file.txt.gz" );
        Files.write( path, "test string", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.copy( fileSystem.toLocalFilePath( path ), ftpUri( "logs/my-file.txt.gz" ), Map.of() );

            InputStream inputStream = fileSystem.getInputStream( ftpUri( "logs/my-file.txt.gz" ) );

            assertThat( IoStreams.in( inputStream, Encoding.GZIP ) ).hasContent( "test string" );
        }
    }

    @NotNull
    private FileSystemConfiguration getFileSystemConfiguration() {
        return ftpFixture.getFileSystemConfiguration();
    }

    @Test
    public void testExistsListAndDelete() {
        ftpFixture.writeFile( "logs/file1.txt", "1", ContentWriter.ofString() );
        ftpFixture.writeFile( "logs/file2.txt", "2", ContentWriter.ofString() );
        ftpFixture.createDirectory( "logs/folder1" );

        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            assertTrue( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( ftpUri( "logs/file2.txt" ) ) );
            assertTrue( fileSystem.containerExists( ftpUri( "" ) ) );

            PageSet<? extends FileSystem.StorageItem> list = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().build() );
            assertThat( list.size() ).isEqualTo( 2 );
            assertNotNull( list.get( 0 ).getLastModified() );
            assertEquals( "logs/file1.txt", list.get( 0 ).getName() );

            PageSet<? extends FileSystem.StorageItem> listP = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file1.txt", listP.get( 0 ).getName() );
            listP = fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().continuationToken( listP.nextContinuationToken ).maxKeys( 1 ).build() );
            assertThat( listP.size() ).isEqualTo( 1 );
            assertEquals( "logs/file2.txt", listP.get( 0 ).getName() );

            fileSystem.deleteBlob( ftpUri( "logs/file1.txt" ) );

            assertFalse( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) );
            assertTrue( fileSystem.blobExists( ftpUri( "logs/file2.txt" ) ) );
            assertThat( fileSystem.list( ftpUri( "logs/" ), ListOptions.builder().build() ).size() ).isEqualTo( 1 );
        }
    }

    @Test
    public void testUploadString() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( ftpFixture.readFile( "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testUploadBytes() {
        try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
            fileSystem.upload( ftpUri( "file.txt" ), BlobData.builder().content( "content".getBytes( UTF_8 ) ).build() );

            assertThat( ftpFixture.readFile( "file.txt", ContentReader.ofString() ) ).isEqualTo( "content" );
        }
    }

    @Test
    public void testFolder() {
        ftpFixture.createDirectory( "folder" );

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

            assertThat( ftpFixture.resolve( "case1/folder1/folder2/file.txt" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case1/folder1/folder2" ) ).exists();
        }

        // 2. enabled -> empty folder2 removed, folder1 kept (still has file2.txt)
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( true ) ) ) {
            fileSystem.upload( ftpUri( "case2/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );
            fileSystem.upload( ftpUri( "case2/folder1/file2.txt" ), BlobData.builder().content( "content2" ).build() );

            fileSystem.deleteBlob( ftpUri( "case2/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( "case2/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case2/folder1" ) ).exists();
        }

        // 3. enabled -> whole empty chain removed
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( true ) ) ) {
            fileSystem.upload( ftpUri( "case3/folder1/folder2/file.txt" ), BlobData.builder().content( "content" ).build() );

            fileSystem.deleteBlob( ftpUri( "case3/folder1/folder2/file.txt" ) );

            assertThat( ftpFixture.resolve( "case3/folder1/folder2" ) ).doesNotExist();
            assertThat( ftpFixture.resolve( "case3/folder1" ) ).doesNotExist();
        }
    }

    @Test
    public void testPoolReusesConnectionSequentially() {
        ftpFixture.writeFile( "logs/file1.txt", "1", ContentWriter.ofString() );

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( false, 1 ) ) ) {
            for( int i = 0; i < 5; i++ ) {
                assertTrue( fileSystem.blobExists( ftpUri( "logs/file1.txt" ) ) );
            }
        }
    }

    @Test
    public void testPoolHandlesConcurrentUploads() {
        int poolMaxSize = 2;
        int uploads = 10;

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( false, poolMaxSize ) ) ) {
            ExecutorService executor = Executors.newFixedThreadPool( uploads );
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for( int i = 0; i < uploads; i++ ) {
                    int idx = i;
                    futures.add( CompletableFuture.runAsync( () ->
                        fileSystem.upload( ftpUri( "concurrent/file" + idx + ".txt" ),
                            BlobData.builder().content( "content" + idx ).build() ), executor ) );
                }

                assertThat( CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) ) )
                    .succeedsWithin( 30, TimeUnit.SECONDS );
            } finally {
                executor.shutdown();
            }
        }

        for( int i = 0; i < uploads; i++ ) {
            assertThat( ftpFixture.readFile( "concurrent/file" + i + ".txt", ContentReader.ofString() ) ).isEqualTo( "content" + i );
        }
    }

    @Test
    public void testPoolHandlesManyParallelUploads() {
        int poolMaxSize = 8;
        int uploads = 1000;

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( false, poolMaxSize ) ) ) {
            ExecutorService executor = Executors.newFixedThreadPool( 50 );
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for( int i = 0; i < uploads; i++ ) {
                    int idx = i;
                    futures.add( CompletableFuture.runAsync( () ->
                        fileSystem.upload( ftpUri( "bulk/file" + idx + ".txt" ),
                            BlobData.builder().content( "content" + idx ).build() ), executor ) );
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
            assertThat( ftpFixture.readFile( s( "bulk/file${idx}.txt" ), ContentReader.ofString() ) ).isEqualTo( "content" + idx );
        }
    }

    @Test
    public void testDifferentHostsUseIndependentConnectionPools() {
        ftpFixture.writeFile( "shared/file.txt", "primary", ContentWriter.ofString() );

        FtpFixture secondFtpFixture = new FtpFixture();
        secondFtpFixture.before();
        try {
            secondFtpFixture.writeFile( "shared/file.txt", "secondary", ContentWriter.ofString() );

            try( FileSystem fileSystem = new FileSystem( getFileSystemConfiguration() ) ) {
                CloudURI primaryUri = ftpUri( "shared/file.txt" );
                CloudURI secondaryUri = new CloudURI( "ftp", secondFtpFixture.hostPort(), "shared/file.txt" );

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
