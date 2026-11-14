package oap.logstream.disk;

import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LoggerException;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joda.time.DateTimeZone.UTC;

@SuppressWarnings( "deprecation" )
public class LogFileTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LogFileTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    private static LogId newLogId() {
        return new LogId( "fpp", "type", Map.of(),
            new String[] { "h1" }, new byte[][] { new byte[] { Types.STRING.id } } );
    }

    private static ByteBuffer bytes( String s ) {
        return ByteBuffer.wrap( s.getBytes( StandardCharsets.UTF_8 ) );
    }

    @Test
    public void testSave() {
        Path file = testDirectoryFixture.testPath( "file" );

        new LogFile( file ).create( new LogId( "fpp", "type", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } ) ).close();

        assertFile( Path.of( file + ".metadata.yaml" ) ).hasContent( """
            ---
            filePrefixPattern: "fpp"
            type: "type"
            headers:
            - "h1"
            - "h2"
            types:
            - - 11
            - - 11
            """ );
    }

    @Test
    public void testCreateWithProperties() {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( newLogId(), Map.of( "VERSION", "abc-1" ) );
        logFile.close();

        assertThat( new LogFile( file ).getLogMetadata().getString( "VERSION" ) ).isEqualTo( "abc-1" );
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA + ".tmp" ) ).doesNotExist();
        assertThat( logFile.getTransactionFile() ).doesNotExist();
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
        // written by the previous versions - ignored, not a property
        assertThat( metadata.getProperties() ).isEmpty();
    }

    @Test
    public void testSaveLoad() {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( new LogId( "fpp", "type", Map.of(),
            new String[] { "h1", "h2" }, new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } } ) );

        DateTime dt = new DateTime( 2019, 11, 29, 10, 9, 0, 0, UTC );
        logFile.addProperty( "time", dt.toString() );
        logFile.close();

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
    public void testCompressionOf() {
        assertThat( LogFile.compressionOf( Path.of( "a/log.tsv.gz.rb.gz" ) ) ).contains( LogStreamCompression.GZIP );
        assertThat( LogFile.compressionOf( Path.of( "a/log.rb.lz4" ) ) ).contains( LogStreamCompression.LZ4 );
        assertThat( LogFile.compressionOf( Path.of( "a/log.rb.zst" ) ) ).contains( LogStreamCompression.ZSTD );
        assertThat( LogFile.compressionOf( Path.of( "a/log.tsv.gz" ) ) ).isEmpty();
    }

    @Test
    public void testAppend() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.append( true, bytes( "hello" ), bytes( "world" ) );
        byte[] tail = "!".getBytes( StandardCharsets.UTF_8 );
        logFile.append( true, tail, 0, tail.length );

        assertThat( logFile.position() ).isEqualTo( 11L );
        assertThat( logFile.hasUnsyncedData() ).isFalse();
        logFile.close();

        assertThat( Files.readString( file ) ).isEqualTo( "helloworld!" );
        assertThat( logFile.getDataSize() ).isEqualTo( 11L );
        assertThat( logFile.getTransactionFile() ).doesNotExist();
    }

    @Test
    public void testAppendWithoutSync() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.append( false, bytes( "hello" ) );
        assertThat( logFile.hasUnsyncedData() ).isTrue();

        logFile.sync();
        assertThat( logFile.hasUnsyncedData() ).isFalse();

        logFile.append( false, bytes( "world" ) );
        logFile.close();
        assertThat( logFile.hasUnsyncedData() ).isFalse();

        assertThat( Files.readString( file ) ).isEqualTo( "helloworld" );
    }

    @Test
    public void testAppendFailureKeepsCommittedData() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.append( true, bytes( "hello" ) );

        logFile.out.close();
        assertThatThrownBy( () -> logFile.append( true, bytes( "world" ) ) ).isInstanceOf( LoggerException.class );
        assertThat( logFile.position() ).isEqualTo( 5L );

        logFile.close();
        assertThat( Files.readString( file ) ).isEqualTo( "hello" );
    }

    @Test
    public void testAppendNotOpen() {
        LogFile logFile = new LogFile( testDirectoryFixture.testPath( "file" ) );

        assertThatThrownBy( () -> logFile.append( true, bytes( "hello" ) ) ).isInstanceOf( IllegalStateException.class );
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
        assertThat( logFile.isValid() ).isTrue();
        assertThat( logFile.existsAndValid() ).isTrue();
        logFile.readyForUpload();
        assertThat( logFile.existsAndValid() ).isTrue();
        logFile.close();
    }

    @Test
    public void testExists() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.exists() ).isFalse();

        Files.writeString( logFile.getTransactionFile(), "0" );
        assertThat( logFile.exists() ).isTrue();
        assertThat( logFile.isLegacy() ).isTrue();
        Files.delete( logFile.getTransactionFile() );

        logFile.readyForUpload();
        assertThat( logFile.exists() ).isTrue();
        Files.delete( logFile.getCompletedFile() );

        logFile.create( newLogId() );
        logFile.close();
        assertThat( logFile.exists() ).isTrue();
        assertThat( logFile.isLegacy() ).isFalse();
    }

    @Test
    public void testLegacyCommittedPosition() throws IOException {
        LogFile logFile = new LogFile( testDirectoryFixture.testPath( "file" ) );
        assertThat( logFile.legacyCommittedPosition() ).isEmpty();

        Files.writeString( logFile.getTransactionFile(), "15" );
        assertThat( logFile.legacyCommittedPosition() ).hasValue( 15L );

        Files.writeString( logFile.getTransactionFile(), "" );
        assertThat( logFile.legacyCommittedPosition() ).isEmpty();

        Files.writeString( logFile.getTransactionFile(), "abc" );
        assertThat( logFile.legacyCommittedPosition() ).isEmpty();

        Files.writeString( logFile.getTransactionFile(), "-1" );
        assertThat( logFile.legacyCommittedPosition() ).isEmpty();
    }

    @Test
    public void testGetMaxModificationTime() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file );
        assertThat( logFile.getMaxModificationTime() ).isEqualTo( -1L );

        logFile.create( newLogId() );
        logFile.close();
        logFile.readyForUpload();

        Files.setLastModifiedTime( file, FileTime.fromMillis( 1_000_000L ) );
        Files.setLastModifiedTime( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ), FileTime.fromMillis( 3_000_000L ) );
        Files.setLastModifiedTime( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ), FileTime.fromMillis( 2_000_000L ) );

        assertThat( logFile.getMaxModificationTime() ).isEqualTo( 3_000_000L );

        // legacy file
        Files.writeString( logFile.getTransactionFile(), "0" );
        Files.setLastModifiedTime( logFile.getTransactionFile(), FileTime.fromMillis( 4_000_000L ) );

        assertThat( logFile.getMaxModificationTime() ).isEqualTo( 4_000_000L );
    }

    @Test
    public void testClose() {
        Path file = testDirectoryFixture.testPath( "file" );
        new LogFile( file ).close();

        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.close();
        logFile.close();
    }

    @Test
    public void testMoveTo() {
        Path baseDir = testDirectoryFixture.testPath( "base" );
        Path destDir = testDirectoryFixture.testPath( "dest" );

        Path file = baseDir.resolve( "a/b/file.gz" );
        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.close();
        logFile.readyForUpload();

        logFile.moveTo( destDir, baseDir );

        assertThat( file ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).doesNotExist();
        assertThat( logFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).doesNotExist();

        LogFile destLogFile = new LogFile( destDir.resolve( "a/b/file.gz" ) );
        assertThat( destLogFile.outFilename ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).doesNotExist();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).exists();
    }

    @Test
    public void testMoveToLegacy() throws IOException {
        Path baseDir = testDirectoryFixture.testPath( "base" );
        Path destDir = testDirectoryFixture.testPath( "dest" );

        Path file = baseDir.resolve( "a/b/file.gz" );
        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.close();
        Files.writeString( logFile.getTransactionFile(), "0" );
        logFile.readyForUpload();

        logFile.moveTo( destDir, baseDir );

        assertThat( logFile.getTransactionFile() ).doesNotExist();

        LogFile destLogFile = new LogFile( destDir.resolve( "a/b/file.gz" ) );
        assertThat( destLogFile.outFilename ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_METADATA ) ).exists();
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_TRANSACTION ) ).hasContent( "0" );
        assertThat( destLogFile.pathFor( LogFile.EXTENSION_LOG_COMPLETED ) ).exists();
    }

    @Test
    public void testDelete() throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        LogFile logFile = new LogFile( file ).create( newLogId() );
        logFile.close();
        Files.writeString( logFile.getTransactionFile(), "0" );
        logFile.readyForUpload();

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
