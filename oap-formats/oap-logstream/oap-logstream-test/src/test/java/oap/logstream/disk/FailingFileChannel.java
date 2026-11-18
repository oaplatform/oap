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

package oap.logstream.disk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * a {@link FileChannel} whose writes fail while {@link #failWrites} is set; everything else is delegated
 */
class FailingFileChannel extends FileChannel {
    private final FileChannel delegate;
    volatile boolean failWrites;

    FailingFileChannel( FileChannel delegate ) {
        this.delegate = delegate;
    }

    private void checkWrite() throws IOException {
        if( failWrites ) throw new IOException( "write failed" );
    }

    @Override
    public int read( ByteBuffer dst ) throws IOException {
        return delegate.read( dst );
    }

    @Override
    public long read( ByteBuffer[] dsts, int offset, int length ) throws IOException {
        return delegate.read( dsts, offset, length );
    }

    @Override
    public int read( ByteBuffer dst, long position ) throws IOException {
        return delegate.read( dst, position );
    }

    @Override
    public int write( ByteBuffer src ) throws IOException {
        checkWrite();
        return delegate.write( src );
    }

    @Override
    public long write( ByteBuffer[] srcs, int offset, int length ) throws IOException {
        checkWrite();
        return delegate.write( srcs, offset, length );
    }

    @Override
    public int write( ByteBuffer src, long position ) throws IOException {
        checkWrite();
        return delegate.write( src, position );
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public FileChannel position( long newPosition ) throws IOException {
        delegate.position( newPosition );
        return this;
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public FileChannel truncate( long size ) throws IOException {
        delegate.truncate( size );
        return this;
    }

    @Override
    public void force( boolean metaData ) throws IOException {
        delegate.force( metaData );
    }

    @Override
    public long transferTo( long position, long count, WritableByteChannel target ) throws IOException {
        return delegate.transferTo( position, count, target );
    }

    @Override
    public long transferFrom( ReadableByteChannel src, long position, long count ) throws IOException {
        checkWrite();
        return delegate.transferFrom( src, position, count );
    }

    @Override
    public MappedByteBuffer map( MapMode mode, long position, long size ) throws IOException {
        return delegate.map( mode, position, size );
    }

    @Override
    public FileLock lock( long position, long size, boolean shared ) throws IOException {
        return delegate.lock( position, size, shared );
    }

    @Override
    public FileLock tryLock( long position, long size, boolean shared ) throws IOException {
        return delegate.tryLock( position, size, shared );
    }

    @Override
    protected void implCloseChannel() throws IOException {
        delegate.close();
    }
}
