package oap.logstream.formats.rowbinary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RowBinaryUtils {
    public static List<List<Object>> read( byte[] bytes, String[] headers, byte[][] types ) throws IOException {
        return read( bytes, 0, bytes.length, headers, types );
    }

    public static List<List<Object>> read( byte[] bytes, int offset, int length, String[] headers, byte[][] types ) throws IOException {
        RowBinaryInputStream binaryInputStream = new RowBinaryInputStream( new ByteArrayInputStream( bytes, offset, length ), headers, types );

        ArrayList<List<Object>> res = new ArrayList<>();

        List<Object> row = binaryInputStream.readRow();
        while( row != null ) {
            res.add( row );

            row = binaryInputStream.readRow();
        }

        return res;
    }

    public static byte[] lines( List<List<Object>> rows ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for( List<Object> row : rows ) {
            baos.write( line( row ) );
        }

        return baos.toByteArray();
    }

    public static byte[] line( Object... cols ) throws IOException {
        return line( List.of( cols ) );
    }

    public static byte[] line( List<Object> cols ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RowBinaryOutputStream bos = new RowBinaryOutputStream( baos );

        for( Object col : cols ) bos.writeObject( col );

        return baos.toByteArray();
    }
}
