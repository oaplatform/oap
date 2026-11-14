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

package oap.logstream;

import com.google.common.base.Preconditions;
import oap.logstream.disk.LogFormat;

/**
 * Payload codec of the logstream protocol. The server appends the received compressed payload to the file as is,
 * so the codec also defines the file format.
 * <ul>
 *     <li>GZIP: level 1..9 (java.util.zip.Deflater)</li>
 *     <li>LZ4: level 0 - fast compressor, 1..17 - LZ4 HC</li>
 *     <li>ZSTD: level -131072..22 (ZSTD_minCLevel..ZSTD_maxCLevel), negative - fast</li>
 * </ul>
 */
public enum LogStreamCompression {
    GZIP( ( byte ) 0, LogFormat.ROW_BINARY_GZ, 1, 9, 1 ),
    LZ4( ( byte ) 1, LogFormat.ROW_BINARY_LZ4, 0, 17, 0 ),
    ZSTD( ( byte ) 2, LogFormat.ROW_BINARY_ZSTD, -( 1 << 17 ), 22, 1 );

    /**
     * protocol value (v4 header), must not change
     */
    public final byte id;
    public final LogFormat logFormat;
    public final int minLevel;
    public final int maxLevel;
    public final int defaultLevel;

    LogStreamCompression( byte id, LogFormat logFormat, int minLevel, int maxLevel, int defaultLevel ) {
        this.id = id;
        this.logFormat = logFormat;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.defaultLevel = defaultLevel;
    }

    public static LogStreamCompression fromId( byte id ) {
        return switch( id ) {
            case 0 -> GZIP;
            case 1 -> LZ4;
            case 2 -> ZSTD;
            default -> throw new IllegalArgumentException( "unknown compression id " + id );
        };
    }

    public int checkLevel( int level ) {
        Preconditions.checkArgument( level >= minLevel && level <= maxLevel,
            "%s compression level must be in [%s..%s], but %s", this, minLevel, maxLevel, level );
        return level;
    }
}
