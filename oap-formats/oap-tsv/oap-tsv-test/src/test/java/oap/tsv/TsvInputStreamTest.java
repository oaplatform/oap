package oap.tsv;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TsvInputStreamTest {

    @Test
    public void readCellsIndexOf() throws IOException {
        var data = "a\tb\tc\t".getBytes( UTF_8 );
        var is = new TsvInputStream( new ByteArrayInputStream( data ), new byte[1024] );
        assertTrue( is.readCells() );

        assertThat( is.line.indexOf( "a" ) ).isEqualTo( 0 );
        assertThat( is.line.indexOf( "c" ) ).isEqualTo( 2 );
        assertThat( is.line.indexOf( "d" ) ).isEqualTo( -1 );

        assertFalse( is.readCells() );
    }

    @Test
    public void testSplit() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "1", split );
        assertThat( split ).containsExactly( "1" );
    }

    @Test
    public void testEmptyLine() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "", split );
        assertThat( split ).containsExactly( "" );
    }

    @Test
    public void testSplitTab() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "1\t5\tttt", split );
        assertThat( split ).containsExactly( "1", "5", "ttt" );
    }

    @Test
    public void testSplitTabEscape() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "1\\t5\t\\r\\nttt", split );
        assertThat( split ).containsExactly( "1\\t5", "\\r\\nttt" );
    }

    @Test
    public void testEmptyCell() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "start\t\tend", split );
        assertThat( split ).containsExactly( "start", "", "end" );
    }

    @Test
    public void testEmptyCellEnd() {
        ArrayList<String> split = new ArrayList<>();
        TsvInputStream.split( "start\t\t", split );
        assertThat( split ).containsExactly( "start", "", "" );
    }

}
