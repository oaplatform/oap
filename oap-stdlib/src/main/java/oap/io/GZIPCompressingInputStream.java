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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

/**
 * Wraps an input stream and compresses it's contents. Similiar to DeflateInputStream but adds GZIP-header and trailer
 * See GzipOutputStream for details.
 */
public class GZIPCompressingInputStream extends SequenceInputStream {
    public GZIPCompressingInputStream( InputStream in ) {
        this( in, 512 );
    }

    public GZIPCompressingInputStream( InputStream in, int bufferSize ) {
        super( new StatefullGzipStreamEnumerator( in, bufferSize ) );
    }

    enum StreamState {
        HEADER,
        CONTENT,
        TRAILER
    }

    protected static class StatefullGzipStreamEnumerator implements Enumeration<InputStream> {

        protected final InputStream in;
        protected final int bufferSize;
        protected StreamState state;

        public StatefullGzipStreamEnumerator( InputStream in, int bufferSize ) {
            this.in = in;
            this.bufferSize = bufferSize;
            state = StreamState.HEADER;
        }

        public boolean hasMoreElements() {
            return state != null;
        }

        public InputStream nextElement() {
            return switch( state ) {
                case HEADER -> {
                    state = StreamState.CONTENT;
                    yield createHeaderStream();
                }
                case CONTENT -> {
                    state = StreamState.TRAILER;
                    yield createContentStream();
                }
                case TRAILER -> {
                    state = null;
                    yield createTrailerStream();
                }
            };
        }

        static final int GZIP_MAGIC = 0x8b1f;
        static final byte[] GZIP_HEADER = new byte[] {
            ( byte ) GZIP_MAGIC,        // Magic number (short)
            ( byte ) ( GZIP_MAGIC >> 8 ),  // Magic number (short)
            Deflater.DEFLATED,        // Compression method (CM)
            0,                        // Flags (FLG)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Modification time MTIME (int)
            0,                        // Extra flags (XFLG)
            0                         // Operating system (OS)
        };

        protected InputStream createHeaderStream() {
            return new ByteArrayInputStream( GZIP_HEADER );
        }

        protected InternalGzipCompressingInputStream contentStream;

        protected InputStream createContentStream() {
            contentStream = new InternalGzipCompressingInputStream( new CRC32InputStream( in ), bufferSize );
            return contentStream;
        }

        protected InputStream createTrailerStream() {
            return new ByteArrayInputStream( contentStream.createTrailer() );
        }
    }

    /**
     * Internal stream without header/trailer
     */
    protected static class InternalGzipCompressingInputStream extends DeflaterInputStream {
        protected final CRC32InputStream crcIn;

        public InternalGzipCompressingInputStream( CRC32InputStream in, int bufferSize ) {
            super( in, new Deflater( Deflater.DEFAULT_COMPRESSION, true ), bufferSize );
            crcIn = in;
        }

        public void close() throws IOException {
            if( in != null ) {
                try {
                    def.end();
                    in.close();
                } finally {
                    in = null;
                }
            }
        }

        protected static final int TRAILER_SIZE = 8;

        public byte[] createTrailer() {
            byte[] trailer = new byte[TRAILER_SIZE];
            writeTrailer( trailer, 0 );
            return trailer;
        }

        /*
         * Writes GZIP member trailer to a byte array, starting at a given
         * offset.
         */
        private void writeTrailer( byte[] buf, int offset ) {
            writeInt( ( int ) crcIn.getCrcValue(), buf, offset ); // CRC-32 of uncompr. data
            writeInt( ( int ) crcIn.getByteCount(), buf, offset + 4 ); // Number of uncompr. bytes
        }

        /*
         * Writes integer in Intel byte order to a byte array, starting at a
         * given offset.
         */
        private void writeInt( int i, byte[] buf, int offset ) {
            writeShort( i & 0xffff, buf, offset );
            writeShort( ( i >> 16 ) & 0xffff, buf, offset + 2 );
        }

        /*
         * Writes short integer in Intel byte order to a byte array, starting
         * at a given offset
         */
        private void writeShort( int s, byte[] buf, int offset ) {
            buf[offset] = ( byte ) ( s & 0xff );
            buf[offset + 1] = ( byte ) ( ( s >> 8 ) & 0xff );
        }
    }

}
