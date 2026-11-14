package oap.logstream.disk;

import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;

import static oap.logstream.disk.Frames.concat;
import static oap.logstream.disk.Frames.decompress;
import static oap.logstream.disk.Frames.frame;
import static oap.logstream.disk.Frames.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogFile#recover()}: files left without .metadata.completed by a crash, written by this version
 * and by the previous one (with .metadata.transaction).
 */
@SuppressWarnings( "deprecation" )
public class LogFileRecoveryTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public LogFileRecoveryTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    private static LogId logId() {
        return new LogId( "fpp", "type", Map.of(), new String[] { "h1" }, new byte[][] { new byte[] { Types.STRING.id } } );
    }

    @DataProvider
    public Object[][] compressions() {
        return Arrays.stream( LogStreamCompression.values() ).map( c -> new Object[] { c } ).toArray( Object[][]::new );
    }

    private Path path( LogStreamCompression compression ) {
        return testDirectoryFixture.testPath( "logs/file." + compression.logFormat.extension );
    }

    /**
     * the state after a crash: data + metadata, no .completed
     */
    private LogFile crashed( LogStreamCompression compression, byte[]... frames ) {
        LogFile logFile = new LogFile( path( compression ) ).create( logId() );
        for( byte[] frame : frames ) logFile.append( true, ByteBuffer.wrap( frame ) );
        logFile.close();
        return new LogFile( logFile.outFilename );
    }

    private static void appendRaw( Path file, byte[] bytes ) throws IOException {
        Files.write( file, bytes, StandardOpenOption.APPEND );
    }

    @Test( dataProvider = "compressions" )
    public void tornTail( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        byte[] f2 = frame( compression, text( "b", 100 ) );
        LogFile logFile = crashed( compression, header, f1 );
        appendRaw( logFile.outFilename, Arrays.copyOf( f2, f2.length / 2 ) );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.RECOVERED );

        assertThat( Files.readAllBytes( logFile.outFilename ) ).isEqualTo( concat( header, f1 ) );
        assertThat( decompress( compression, Files.readAllBytes( logFile.outFilename ) ) ).isEqualTo( concat( text( "header", 1 ), text( "a", 100 ) ) );
        assertThat( logFile.isCompleted() ).isTrue();
        assertThat( logFile.getMetadataFile() ).exists();
    }

    @Test( dataProvider = "compressions" )
    public void completeFileIsOnlyMarkedCompleted( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        LogFile logFile = crashed( compression, header, f1 );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.RECOVERED );

        assertThat( Files.readAllBytes( logFile.outFilename ) ).isEqualTo( concat( header, f1 ) );
        assertThat( logFile.isCompleted() ).isTrue();
    }

    @Test( dataProvider = "compressions" )
    public void onlyHeaderIsDeleted( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        LogFile logFile = crashed( compression, header );
        appendRaw( logFile.outFilename, Arrays.copyOf( f1, 10 ) );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.DELETED );

        assertThat( logFile.exists() ).isFalse();
    }

    @Test
    public void emptyFileIsDeleted() {
        LogFile logFile = crashed( LogStreamCompression.ZSTD );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.DELETED );
        assertThat( logFile.exists() ).isFalse();
    }

    @Test
    public void completedIsSkipped() throws IOException {
        byte[] header = frame( LogStreamCompression.ZSTD, text( "header", 1 ) );
        LogFile logFile = crashed( LogStreamCompression.ZSTD, header );
        appendRaw( logFile.outFilename, new byte[] { 1, 2, 3 } );
        logFile.readyForUpload();

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.SKIPPED );
        assertThat( Files.size( logFile.outFilename ) ).isEqualTo( header.length + 3L );
    }

    @Test
    public void unknownFormatIsSkipped() throws IOException {
        Path file = testDirectoryFixture.testPath( "logs/file.tsv.gz" );
        LogFile logFile = new LogFile( file ).create( logId() );
        logFile.append( true, new byte[] { 1, 2, 3 }, 0, 3 );
        logFile.close();

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.SKIPPED );
        assertThat( Files.size( file ) ).isEqualTo( 3L );
        assertThat( logFile.isCompleted() ).isFalse();
    }

    @Test
    public void noMetadataIsSkipped() throws IOException {
        Path file = path( LogStreamCompression.GZIP );
        Files.createDirectories( file.getParent() );
        Files.write( file, frame( LogStreamCompression.GZIP, text( "a", 1 ) ) );

        assertThat( new LogFile( file ).recover() ).isEqualTo( LogFile.Recovery.SKIPPED );
        assertThat( new LogFile( file ).isCompleted() ).isFalse();
    }

    @Test
    public void sidecarsWithoutDataAreDeleted() throws IOException {
        LogFile logFile = crashed( LogStreamCompression.GZIP );
        Files.delete( logFile.outFilename );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.DELETED );
        assertThat( logFile.exists() ).isFalse();
    }

    /**
     * previous version: the data after the committed offset was written, but not committed (the client got no ACK and
     * sends it again) - dropped, no duplicates
     */
    @Test( dataProvider = "compressions" )
    public void legacyTruncatedToCommittedOffset( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        byte[] f2 = frame( compression, text( "b", 100 ) );
        LogFile logFile = crashed( compression, header, f1, f2 );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( header.length + f1.length ) );

        assertThat( logFile.isLegacy() ).isTrue();
        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.RECOVERED );

        assertThat( Files.readAllBytes( logFile.outFilename ) ).isEqualTo( concat( header, f1 ) );
        assertThat( logFile.isCompleted() ).isTrue();
        assertThat( logFile.getTransactionFile() ).hasContent( String.valueOf( header.length + f1.length ) );
    }

    @Test( dataProvider = "compressions" )
    public void legacyCommittedEqualsSize( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        LogFile logFile = crashed( compression, header, f1 );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( header.length + f1.length ) );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.RECOVERED );

        assertThat( Files.readAllBytes( logFile.outFilename ) ).isEqualTo( concat( header, f1 ) );
        assertThat( logFile.isCompleted() ).isTrue();
    }

    @Test
    public void legacyOnlyHeaderCommittedIsDeleted() throws IOException {
        LogStreamCompression compression = LogStreamCompression.GZIP;
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        LogFile logFile = crashed( compression, header, f1 );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( header.length ) );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.DELETED );
        assertThat( logFile.exists() ).isFalse();
    }

    /**
     * the committed offset cannot be trusted - the scan result is used
     */
    @Test( dataProvider = "compressions" )
    public void legacyInvalidCommittedOffset( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        byte[] f2 = frame( compression, text( "b", 100 ) );
        int size = header.length + f1.length + f2.length;

        for( String committed : new String[] { "", "abc", String.valueOf( size + 100 ), String.valueOf( header.length + f1.length / 2 ) } ) {
            LogFile logFile = crashed( compression, header, f1, f2 );
            appendRaw( logFile.outFilename, Arrays.copyOf( f2, 5 ) );
            Files.writeString( logFile.getTransactionFile(), committed );

            assertThat( logFile.recover() ).as( "committed '%s'", committed ).isEqualTo( LogFile.Recovery.RECOVERED );
            assertThat( Files.readAllBytes( logFile.outFilename ) ).as( "committed '%s'", committed ).isEqualTo( concat( header, f1, f2 ) );
            assertThat( logFile.isCompleted() ).isTrue();

            logFile.delete();
        }
    }

    /**
     * files written by the previous version or by not yet updated clients: LZ4 / zstd frames without the content checksum
     */
    @Test( dataProvider = "compressions" )
    public void legacyFramesWithoutChecksum( LogStreamCompression compression ) throws IOException {
        byte[] header = frame( compression, text( "header", 1 ), false );
        byte[] f1 = frame( compression, text( "a", 100 ), false );
        byte[] f2 = frame( compression, text( "b", 100 ), false );
        LogFile logFile = crashed( compression, header, f1, f2 );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( header.length + f1.length + f2.length ) );
        appendRaw( logFile.outFilename, Arrays.copyOf( f2, f2.length - 3 ) );

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.RECOVERED );

        assertThat( decompress( compression, Files.readAllBytes( logFile.outFilename ) ) )
            .isEqualTo( concat( text( "header", 1 ), text( "a", 100 ), text( "b", 100 ) ) );
        assertThat( logFile.isCompleted() ).isTrue();
    }

    @Test
    public void legacyCompletedIsNotChanged() throws IOException {
        LogStreamCompression compression = LogStreamCompression.GZIP;
        byte[] header = frame( compression, text( "header", 1 ) );
        byte[] f1 = frame( compression, text( "a", 100 ) );
        LogFile logFile = crashed( compression, header, f1 );
        Files.writeString( logFile.getTransactionFile(), String.valueOf( header.length ) );
        logFile.readyForUpload();

        assertThat( logFile.recover() ).isEqualTo( LogFile.Recovery.SKIPPED );
        assertThat( Files.readAllBytes( logFile.outFilename ) ).isEqualTo( concat( header, f1 ) );
        assertThat( logFile.getTransactionFile() ).exists();
    }
}
