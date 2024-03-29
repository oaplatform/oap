/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.template;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BinaryUtils {
    public static byte[] line( Object... cols ) throws IOException {
        return line( List.of( cols ) );
    }

    public static byte[] line( List<Object> cols ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputStream bos = new BinaryOutputStream( baos );

        for( var col : cols ) bos.writeObject( col );

        baos.write( Types.EOL.id );

        return baos.toByteArray();
    }

    public static byte[] lines( List<List<Object>> rows ) throws IOException {
        var baos = new ByteArrayOutputStream();
        for( var row : rows ) {
            baos.write( line( row ) );
        }

        return baos.toByteArray();
    }

    public static List<List<Object>> read( byte[] bytes ) throws IOException {
        return read( bytes, 0, bytes.length );
    }

    public static List<List<Object>> read( byte[] bytes, int offset, int length ) throws IOException {
        BinaryInputStream binaryInputStream = new BinaryInputStream( new ByteArrayInputStream( bytes, offset, length ) );
        Object obj = binaryInputStream.readObject();
        var line = new ArrayList<Object>();
        var res = new ArrayList<List<Object>>();
        while( obj != null ) {
            if( obj != BinaryInputStream.EOL ) line.add( obj );
            else {
                res.add( line );
                line = new ArrayList<>();
            }
            obj = binaryInputStream.readObject();
        }
        if( !line.isEmpty() ) res.add( line );

        return res;
    }
}
