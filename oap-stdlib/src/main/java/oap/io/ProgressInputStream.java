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

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static oap.util.function.Functions.empty.consume;

public class ProgressInputStream extends FilterInputStream {
    private final Progress progress;
    private long total;

    protected ProgressInputStream( InputStream in, Progress progress ) {
        super( in );
        this.progress = progress;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        progress.soFar( ++total );
        return read;
    }

    @Override
    public int read( @Nonnull byte[] b ) throws IOException {
        return read( b, 0, b.length );
    }

    @Override
    public int read( @Nonnull byte[] b, int off, int len ) throws IOException {
        int read = super.read( b, off, len );
        progress.soFar( total += read );
        return read;
    }

    @Override
    public long skip( long n ) throws IOException {
        long skip = super.skip( n );
        progress.soFar( total += skip );
        return skip;
    }

    public static Progress progress( long total, Consumer<Integer> percentage ) {
        return new Progress( total ) {
            @Override
            public void percent( int soFar ) {
                percentage.accept( soFar );
            }
        };
    }

    public static Consumer<Integer> scale( int by, Consumer<Integer> progress ) {
        AtomicInteger last = new AtomicInteger( 0 );
        return p -> {
            if( last.get() + by < p || p == 100 ) progress.accept( last.updateAndGet( x -> p ) );
        };
    }

    @SuppressWarnings( "checkstyle:AbstractClassName" )
    public abstract static class Progress {
        public static final Progress EMPTY = progress( Long.MAX_VALUE, consume() );
        private final long total;
        private int lastReport;

        public Progress( long total ) {
            this.total = total;
        }


        void soFar( Long soFar ) {
            int current = ( int ) Math.ceil( soFar * 100d / total );
            if( current > lastReport ) {
                lastReport = current;
                percent( lastReport );
            }
        }

        public abstract void percent( int soFar );
    }

}
