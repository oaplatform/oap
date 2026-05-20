package oap.logstream.disk;

import oap.logstream.LogId;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;


public class LogFileTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LogFileTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    private static LogId newLogId() {
        return new LogId( "fpp", "type", "host", Map.of(),
            new String[] { "h1" }, new byte[][] { new byte[] { Types.STRING.id } } );
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

    @Test
    public void testLoadFromPath() {
        Path base = testDirectoryFixture.testPath( "file.gz" );
        LogFile logFile = new LogFile( base );

        assertThat( LogFile.loadFromPath( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).outFilename ).isEqualTo( base );
        assertThat( LogFile.loadFromPath( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).outFilename ).isEqualTo( base );
        assertThat( LogFile.loadFromPath( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).outFilename ).isEqualTo( base );
        assertThat( LogFile.loadFromPath( base ).outFilename ).isEqualTo( base );
    }

    @Test
    public void testBeginTransactionNoFile() {
        assertThat( new LogFile( testDirectoryFixture.testPath( "file" ) ).beginTransaction() ).isEqualTo( 0L );
    }

    @Test
    public void testCommitTransaction() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        logFile.commitTransaction( 10 );
        assertThat( Files.readString( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ) ).isEqualTo( "10" );
        logFile.commitTransaction( 5 );
        assertThat( Files.readString( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ) ).isEqualTo( "15" );
    }

    @Test
    public void testReadyForUpload() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        logFile.readyForUpload();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).exists();
        logFile.readyForUpload();
    }

    @Test
    public void testIsCompleted() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.isCompleted() ).isFalse();
        logFile.readyForUpload();
        assertThat( logFile.isCompleted() ).isTrue();
    }

    @Test
    public void testIsValid() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.isValid() ).isFalse();
        logFile.create( newLogId() );
        assertThat( logFile.isValid() ).isFalse();
        logFile.commitTransaction( 0 );
        assertThat( logFile.isValid() ).isTrue();
        logFile.close();
    }

    @Test
    public void testExistsAndValid() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.existsAndValid() ).isFalse();

        logFile.create( newLogId() );
        logFile.commitTransaction( 0 );
        assertThat( logFile.existsAndValid() ).isTrue();

        logFile.readyForUpload();
        assertThat( logFile.existsAndValid() ).isFalse();

        logFile.close();
    }

    @Test
    public void testWriteAndCommitTransaction() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        byte[] data = "hello".getBytes( StandardCharsets.UTF_8 );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.writeAndCommitTransaction( data, 0, data.length );

        assertThat( Files.readAllBytes( file ) ).isEqualTo( data );
        assertThat( Files.readString( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ) )
            .isEqualTo( String.valueOf( data.length ) );
        logFile.close();
    }

    @Test
    public void testBeginTransactionWriteAndCommitTransaction() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        byte[] first = "hello".getBytes( StandardCharsets.UTF_8 );
        byte[] second = "world".getBytes( StandardCharsets.UTF_8 );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.beginTransactionWriteAndCommitTransaction( first, 0, first.length );
        logFile.beginTransactionWriteAndCommitTransaction( second, 0, second.length );

        assertThat( Files.readAllBytes( file ) ).isEqualTo( "helloworld".getBytes( StandardCharsets.UTF_8 ) );
        logFile.close();
    }

    @Test
    public void testGetDataSize() {
        Path file = testDirectoryFixture.testPath( "file" );
        byte[] data = "hello".getBytes( StandardCharsets.UTF_8 );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.writeAndCommitTransaction( data, 0, data.length );

        assertThat( logFile.getDataSize() ).isEqualTo( data.length );
        logFile.close();
    }

    @Test
    public void testGetMaxModificationTime() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.getMaxModificationTime() ).isEqualTo( -1L );

        logFile.create( newLogId() );
        logFile.commitTransaction( 0 );
        logFile.readyForUpload();

        Files.setLastModifiedTime( file, FileTime.fromMillis( 1_000_000L ) );
        Files.setLastModifiedTime( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ), FileTime.fromMillis( 2_000_000L ) );
        Files.setLastModifiedTime( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ), FileTime.fromMillis( 3_000_000L ) );
        Files.setLastModifiedTime( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ), FileTime.fromMillis( 4_000_000L ) );

        assertThat( logFile.getMaxModificationTime() ).isEqualTo( 4_000_000L );
        logFile.close();
    }

    @Test
    public void testClose() {
        Path file = testDirectoryFixture.testPath( "file" );
        new LogFile( file ).close();

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.close();
    }

    @Test
    public void testGetTransactionPosition() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.getTransactionPosition() ).isEqualTo( 0L );

        logFile.commitTransaction( 10 );
        assertThat( logFile.getTransactionPosition() ).isEqualTo( 10L );

        logFile.commitTransaction( 5 );
        assertThat( logFile.getTransactionPosition() ).isEqualTo( 15L );
    }

    @Test
    public void testMoveTo() {
        Path baseDir = testDirectoryFixture.testPath( "base" );
        Path destDir = testDirectoryFixture.testPath( "dest" );

        Path file = baseDir.resolve( "a/b/file.gz" );
        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.commitTransaction( 0 );
        logFile.readyForUpload();
        logFile.close();

        logFile.moveTo( destDir, baseDir );

        assertThat( file ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).doesNotExist();

        LogFile destLogFile = new LogFile( destDir.resolve( "a/b/file.gz" ) );
        assertThat( destLogFile.outFilename ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).exists();
    }

    @Test
    public void testDelete() {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.commitTransaction( 0 );
        logFile.readyForUpload();
        logFile.close();

        assertThat( file ).exists();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).exists();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).exists();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).exists();

        logFile.delete();

        assertThat( file ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).doesNotExist();

        logFile.delete();
    }
}
