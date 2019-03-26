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
package oap.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SafeFileOutputStream extends FileOutputStream {
    private final Path path;

    public SafeFileOutputStream( Path path, boolean append, IoStreams.Encoding encoding ) throws IOException {
        super( path + ".unsafe", append );
        this.path = path;

        if( append && java.nio.file.Files.exists( path ) ) {
            var sourceEncoding = IoStreams.Encoding.from( path );
            try( InputStream is = IoStreams.in( path,
                sourceEncoding == encoding ? IoStreams.Encoding.PLAIN : encoding ) ) {
                IoStreams.write( Paths.get( path + ".unsafe" ),
                    sourceEncoding == encoding ? IoStreams.Encoding.PLAIN : encoding, is );
            }
        }
    }

    @Override
    public void close() throws IOException {
        // https://stackoverflow.com/questions/25910173/fileoutputstream-close-is-not-always-writing-bytes-to-file-system
        getFD().sync();
        super.close();
        final Path unsafePath = Paths.get( this.path + ".unsafe" );
        if( !java.nio.file.Files.exists( unsafePath ) || java.nio.file.Files.size( unsafePath ) == 0 )
            Files.delete( unsafePath );
        else
            Files.rename( unsafePath, this.path );
    }


}
