package oap.io.io;


import oap.io.GZIPCompressingInputStream;
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
        compressor( "test1 test2 test3" );
        compressor( "1MiB binary data", createTestPattern( 1024 * 1024 ) );
        for( int i = 0; i < 4096; i++ ) compressor( i + " bytes of binary data", createTestPattern( i ) );
    }

    protected byte[] createTestPattern( int size ) {
        byte[] data = new byte[size];
        byte pattern = 0;
        for( int i = 0; i < size; i++ ) data[i] = pattern++;
        return data;
    }

    protected void compressor( String data ) throws IOException {
        compressor( "String: " + data, data.getBytes() );
    }

    protected void compressor( String dataInfo, byte[] data ) throws IOException {
        try ( InputStream uncompressedIn = new ByteArrayInputStream( data );
              InputStream compressedIn = new GZIPCompressingInputStream( uncompressedIn );
              InputStream uncompressedOut = new GZIPInputStream( compressedIn ) ) {


            byte[] result = IOUtils.toByteArray( uncompressedOut );

            assertThat( result )
                    .withFailMessage( "Test failed for: " + dataInfo )
                    .containsExactly( data );
        }
    }
}
