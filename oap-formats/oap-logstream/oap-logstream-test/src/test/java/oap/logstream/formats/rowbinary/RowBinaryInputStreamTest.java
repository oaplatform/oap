package oap.logstream.formats.rowbinary;

import oap.template.Types;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RowBinaryInputStreamTest {
    @Test
    public void testEof() throws IOException {
        byte[] bytes = RowBinaryUtils.line( List.of( ( byte ) 0, false ) );

        RowBinaryInputStream rowBinaryInputStream = new RowBinaryInputStream( new ByteArrayInputStream( bytes ), new String[] { "BYTE", "BOOLEAN" }, new byte[][] { { Types.BYTE.id }, { Types.BOOLEAN.id } } );

        List<Object> objects = rowBinaryInputStream.readRow();
        assertThat( objects ).containsExactly( ( byte ) 0, false );

        assertThat( rowBinaryInputStream.readRow() ).isNull();
    }
}
