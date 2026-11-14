package oap.logstream.disk;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import net.jpountz.lz4.LZ4FrameOutputStream;
import oap.compression.Compression;
import oap.logstream.LogStreamCompression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * compressed frames for the recovery tests
 */
final class Frames {
    private Frames() {
    }

    /**
     * a frame as written by the current code (gzip CRC32, LZ4 / zstd content checksum)
     */
    static byte[] frame( LogStreamCompression compression, byte[] data ) throws IOException {
        return frame( compression, data, true );
    }

    /**
     * @param checksum false - LZ4 / zstd frame without the content checksum, as written before checksums were enabled
     */
    static byte[] frame( LogStreamCompression compression, byte[] data, boolean checksum ) throws IOException {
        if( checksum ) {
            return switch( compression ) {
                case GZIP -> Compression.gzip( data, 0, data.length );
                case LZ4 -> Compression.lz4( data, 0, data.length );
                case ZSTD -> Compression.zstd( data, 0, data.length, 1 );
            };
        }

        return switch( compression ) {
            case GZIP -> Compression.gzip( data, 0, data.length );
            case LZ4 -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try( LZ4FrameOutputStream out = new LZ4FrameOutputStream( baos, LZ4FrameOutputStream.BLOCKSIZE.SIZE_256KB ) ) {
                    out.write( data );
                }
                yield baos.toByteArray();
            }
            case ZSTD -> {
                try( ZstdCompressCtx ctx = new ZstdCompressCtx() ) {
                    ctx.setLevel( 1 );
                    ctx.setChecksum( false );
                    ctx.setContentSize( true );
                    byte[] dst = new byte[( int ) Zstd.compressBound( data.length )];
                    int size = ctx.compressByteArray( dst, 0, dst.length, data, 0, data.length );
                    yield Arrays.copyOf( dst, size );
                }
            }
        };
    }

    static byte[] decompress( LogStreamCompression compression, byte[] data ) throws IOException {
        return switch( compression ) {
            case GZIP -> Compression.ungzip( data );
            case LZ4 -> Compression.unlz4( data );
            case ZSTD -> Compression.unzstd( data );
        };
    }

    /**
     * incompressible: the codecs store it as is (stored deflate block, uncompressed LZ4 block, raw zstd block),
     * so a flipped byte in the middle is caught only by the checksum
     */
    static byte[] random( int length, long seed ) {
        byte[] bytes = new byte[length];
        new Random( seed ).nextBytes( bytes );
        return bytes;
    }

    static byte[] text( String prefix, int lines ) {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < lines; i++ ) sb.append( prefix ).append( ' ' ).append( i ).append( '\n' );
        return sb.toString().getBytes( java.nio.charset.StandardCharsets.UTF_8 );
    }

    static byte[] concat( byte[]... parts ) {
        int length = 0;
        for( byte[] part : parts ) length += part.length;
        byte[] result = new byte[length];
        int pos = 0;
        for( byte[] part : parts ) {
            System.arraycopy( part, 0, result, pos, part.length );
            pos += part.length;
        }
        return result;
    }
}
