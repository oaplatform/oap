package oap.logstream.formats.rowbinary;

import oap.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static oap.util.Pair.__;

public class RowBinaryUtils {
    public static List<List<Object>> read( byte[] bytes ) throws IOException {
        return read( bytes, null, null );
    }

    public static List<List<Object>> read( byte[] bytes, String[] headers, byte[][] types ) throws IOException {
        return read( bytes, 0, bytes.length, headers, types )._1;
    }

    public static Pair<List<List<Object>>, List<String>> read( byte[] bytes, int offset, int length ) throws IOException {
        return read( bytes, offset, length, null, null );
    }

    public static Pair<List<List<Object>>, List<String>> read( byte[] bytes, int offset, int length, String[] headers, byte[][] types ) throws IOException {
        RowBinaryInputStream binaryInputStream = new RowBinaryInputStream( new ByteArrayInputStream( bytes, offset, length ), headers, types );

        ArrayList<List<Object>> res = new ArrayList<>();

        List<Object> row = binaryInputStream.readRow();
        while( row != null ) {
            res.add( row );

            row = binaryInputStream.readRow();
        }

        return __( res, List.of( binaryInputStream.headers ) );
    }

    public static byte[] lines( List<List<Object>> rows ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for( List<Object> row : rows ) {
            baos.write( line( row ) );
        }

        return baos.toByteArray();
    }

    public static <T> byte[] line( List<T> cols ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RowBinaryOutputStream bos = new RowBinaryOutputStream( baos );

        for( Object col : cols ) {
            bos.writeObject( col );
        }

        return baos.toByteArray();
    }
}
