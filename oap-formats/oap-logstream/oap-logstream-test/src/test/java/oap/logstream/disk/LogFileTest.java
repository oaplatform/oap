package oap.logstream.disk;

import oap.logstream.LogId;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;


public class LogFileTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LogFileTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void testSave() {
        Path file = testDirectoryFixture.testPath( "file" );

        new LogFile( file ).create( new LogId( "fpp", "type", "host", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } ) );

        assertFile( Path.of( file + ".metadata.yaml" ) ).hasContent( """
            ---
            filePrefixPattern: "fpp"
            type: "type"
            clientHostname: "host"
            headers:
            - "h1"
            - "h2"
            types:
            - - 11
            - - 11
            """ );
    }

    @Test
    public void testLoadWithoutHeaders() throws IOException {
        Files.writeString( testDirectoryFixture.testPath( "file.gz.metadata.yaml" ), """
            ---
            filePrefixPattern: "fpp"
            type: "type"
            clientHostname: "host"
            """ );

        LogFile logFile = new LogFile( testDirectoryFixture.testPath( "file.gz" ) );

        LogMetadata metadata = logFile.getLogMetadata();
        assertThat( metadata.headers ).isNull();
        assertThat( metadata.types ).isNull();
    }

    @Test
    public void testSaveLoad() {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( new LogId( "fpp", "type", "host", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } ) );

        DateTime dt = new DateTime( 2019, 11, 29, 10, 9, 0, 0, UTC );
        logFile.addProperty( "time", dt.toString() );

        LogMetadata newLm = new LogFile( file ).getLogMetadata();
        assertThat( newLm.getDateTime( "time" ) ).isEqualTo( dt );
    }
}
