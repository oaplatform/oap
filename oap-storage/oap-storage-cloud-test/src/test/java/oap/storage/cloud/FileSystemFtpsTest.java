package oap.storage.cloud;

import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemFtpsTest extends Fixtures {
    private static final FtpFixture ftpFixture;

    static {
        TestDirectoryFixture testDirectoryFixture = suiteFixture( new TestDirectoryFixture( "-ftps-client" ) );
        ftpFixture = suiteFixture( new FtpFixture( testDirectoryFixture ).withTls() );
    }

    public FileSystemFtpsTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    @BeforeMethod
    public void beforeMethod() {
        oap.io.Files.delete( ftpFixture.homeDirectory() );
        oap.io.Files.ensureDirectory( ftpFixture.homeDirectory() );
    }

    @Test
    public void testUploadAndGetInputStream() {
        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( null ) ) ) {
            fileSystem.upload( new CloudURI( "ftps://file.txt" ), BlobData.builder().content( "content" ).build() );

            assertThat( ftpFixture.readFile( "file.txt" ) ).isEqualTo( "content" );

            InputStream inputStream = fileSystem.getInputStream( new CloudURI( "ftps://file.txt" ) );
            assertThat( inputStream ).hasContent( "content" );
        }
    }

    @Test
    public void testDownloadFile() {
        ftpFixture.writeFile( "logs/file.txt", "test string" );

        try( FileSystem fileSystem = new FileSystem( ftpFixture.getFileSystemConfiguration( null ) ) ) {
            assertThat( fileSystem.blobExists( new CloudURI( "ftps://logs/file.txt" ) ) ).isTrue();
        }
    }
}
