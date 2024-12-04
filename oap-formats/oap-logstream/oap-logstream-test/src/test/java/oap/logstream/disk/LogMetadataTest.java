package oap.logstream.disk;

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


public class LogMetadataTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LogMetadataTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void testSave() {
        Path file = testDirectoryFixture.testPath( "file" );

        LogMetadata metadata = new LogMetadata( "fpp", "type", "host", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } );
        metadata.writeFor( file );

        assertFile( Path.of( file.toString() + ".metadata.yaml" ) ).hasContent( """
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

        LogMetadata metadata = LogMetadata.readFor( testDirectoryFixture.testPath( "file.gz" ) );
        assertThat( metadata.headers ).isNull();
        assertThat( metadata.types ).isNull();
    }

    @Test
    public void testSaveLoad() {
        Path file = testDirectoryFixture.testPath( "file" );

        LogMetadata metadata = new LogMetadata( "fpp", "type", "host", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } );
        metadata.writeFor( file );

        DateTime dt = new DateTime( 2019, 11, 29, 10, 9, 0, 0, UTC );
        LogMetadata.addProperty( file, "time", dt.toString() );

        LogMetadata newLm = LogMetadata.readFor( file );
        assertThat( newLm.getDateTime( "time" ) ).isEqualTo( dt );
    }
}
