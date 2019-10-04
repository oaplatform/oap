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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class LazyFileOutputStream extends OutputStream {
    private final File file;
    private FileOutputStream fos = null;

    public LazyFileOutputStream( Path file ) {
        this( file.toFile() );
    }

    public LazyFileOutputStream( File file ) {
        this.file = file;
    }

    private void open() throws FileNotFoundException {
        if( fos == null ) {
            Files.ensureFile( file.toPath() );
            fos = new FileOutputStream( file );
        }
    }

    @Override
    public void write( int b ) throws IOException {
        open();

        fos.write( b );
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        open();

        fos.write( b, off, len );
    }

    @Override
    public void close() throws IOException {
        if( fos != null ) fos.close();
    }

    @Override
    public void flush() throws IOException {
        if( fos != null ) fos.flush();
    }

    @Override
    public void write( byte[] b ) throws IOException {
        open();

        fos.write( b );
    }
}
