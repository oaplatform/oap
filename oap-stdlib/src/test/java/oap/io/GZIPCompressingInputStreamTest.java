package oap.io;


import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class GZIPCompressingInputStreamTest {


    @Test
    public void test() throws IOException {
        testCompressor( "test1 test2 test3" );
        testCompressor( "1MB binary data", createTestPattern( 1024 * 1024 ) );
        for( int i = 0; i < 4096; i++ ) testCompressor( i + " bytes of binary data", createTestPattern( i ) );
    }

    protected byte[] createTestPattern( int size ) {
        byte[] data = new byte[size];
        byte pattern = 0;
        for( int i = 0; i < size; i++ ) data[i] = pattern++;
        return data;
    }

    protected void testCompressor( String data ) throws IOException {
        testCompressor( "String: " + data, data.getBytes() );
    }

    protected void testCompressor( String dataInfo, byte[] data ) throws IOException {
        InputStream uncompressedIn = new ByteArrayInputStream( data );
        InputStream compressedIn = new GZIPCompressingInputStream( uncompressedIn );
        InputStream uncompressedOut = new GZIPInputStream( compressedIn );

        byte[] result = IOUtils.toByteArray( uncompressedOut );

        assertThat( result )
            .withFailMessage( ( "Test failed for: " + dataInfo ) )
            .containsExactly( data );

    }

}
