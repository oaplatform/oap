package oap.logstream.disk;

import oap.logstream.LogStreamCompression;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static oap.logstream.disk.Frames.concat;
import static oap.logstream.disk.Frames.frame;
import static oap.logstream.disk.Frames.random;
import static oap.logstream.disk.Frames.text;
import static org.assertj.core.api.Assertions.assertThat;

public class FrameScannerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public FrameScannerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @DataProvider
    public Object[][] compressions() {
        return Arrays.stream( LogStreamCompression.values() ).map( c -> new Object[] { c } ).toArray( Object[][]::new );
    }

    private FrameScanner.Scan scan( LogStreamCompression compression, byte[] content ) throws IOException {
        Path file = testDirectoryFixture.testPath( "file" );
        Files.write( file, content );
        try( FileChannel channel = FileChannel.open( file, StandardOpenOption.READ ) ) {
            return FrameScanner.scan( channel, compression );
        }
    }

    @Test( dataProvider = "compressions" )
    public void complete( LogStreamCompression compression ) throws IOException {
        byte[] f1 = frame( compression, text( "a", 10 ) );
        byte[] f2 = frame( compression, text( "b", 1000 ) );
        byte[] f3 = frame( compression, random( 300_000, 1 ) );

        FrameScanner.Scan scan = scan( compression, concat( f1, f2, f3 ) );

        assertThat( scan.ends() ).containsExactly( f1.length, f1.length + f2.length, f1.length + f2.length + f3.length );
        assertThat( scan.complete() ).isTrue();
        assertThat( scan.isBoundary( 0 ) ).isTrue();
        assertThat( scan.isBoundary( f1.length ) ).isTrue();
        assertThat( scan.isBoundary( f1.length + 1 ) ).isFalse();
        assertThat( scan.framesUpTo( f1.length + f2.length ) ).isEqualTo( 2 );
        assertThat( scan.framesUpTo( f1.length + 1 ) ).isEqualTo( 1 );
    }

    @Test( dataProvider = "compressions" )
    public void tornLastFrame( LogStreamCompression compression ) throws IOException {
        byte[] f1 = frame( compression, text( "a", 10 ) );
        byte[] f2 = frame( compression, text( "b", 1000 ) );
        byte[] f3 = frame( compression, text( "c", 1000 ) );

        for( int cut : new int[] { 1, 4, f3.length / 2, f3.length - 1 } ) {
            FrameScanner.Scan scan = scan( compression, concat( f1, f2, Arrays.copyOf( f3, cut ) ) );

            assertThat( scan.validEnd() ).as( "cut %s", cut ).isEqualTo( f1.length + f2.length );
            assertThat( scan.complete() ).isFalse();
        }
    }

    /**
     * the frame has the right structure and length, only the checksum tells it is corrupted
     */
    @Test( dataProvider = "compressions" )
    public void corruptedContentIsCaughtByChecksum( LogStreamCompression compression ) throws IOException {
        byte[] f1 = frame( compression, text( "a", 10 ) );
        byte[] f2 = frame( compression, random( 100_000, 2 ) );
        f2[f2.length / 2] ^= 0x55;

        FrameScanner.Scan scan = scan( compression, concat( f1, f2 ) );

        assertThat( scan.validEnd() ).isEqualTo( f1.length );
    }

    @Test( dataProvider = "compressions" )
    public void framesWithoutChecksumAreAccepted( LogStreamCompression compression ) throws IOException {
        byte[] f1 = frame( compression, text( "a", 10 ), false );
        byte[] f2 = frame( compression, text( "b", 1000 ), false );
        byte[] f3 = frame( compression, text( "c", 1000 ) );

        FrameScanner.Scan scan = scan( compression, concat( f1, f2, f3 ) );

        assertThat( scan.complete() ).isTrue();
        assertThat( scan.ends() ).hasSize( 3 );
    }

    @Test( dataProvider = "compressions" )
    public void empty( LogStreamCompression compression ) throws IOException {
        FrameScanner.Scan scan = scan( compression, new byte[0] );

        assertThat( scan.ends() ).isEmpty();
        assertThat( scan.validEnd() ).isZero();
        assertThat( scan.complete() ).isTrue();
    }

    @Test( dataProvider = "compressions" )
    public void garbage( LogStreamCompression compression ) throws IOException {
        byte[] f1 = frame( compression, text( "a", 10 ) );

        assertThat( scan( compression, random( 1000, 3 ) ).validEnd() ).isZero();
        assertThat( scan( compression, concat( f1, random( 1000, 3 ) ) ).validEnd() ).isEqualTo( f1.length );
        assertThat( scan( compression, concat( f1, new byte[100] ) ).validEnd() ).isEqualTo( f1.length );
    }

    @Test
    public void otherCodecIsInvalid() throws IOException {
        byte[] zstd = frame( LogStreamCompression.ZSTD, text( "a", 10 ) );

        assertThat( scan( LogStreamCompression.GZIP, zstd ).validEnd() ).isZero();
        assertThat( scan( LogStreamCompression.LZ4, zstd ).validEnd() ).isZero();
    }
}
