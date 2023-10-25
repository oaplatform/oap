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

package oap.message;

import lombok.ToString;
import oap.util.ByteSequence;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

@ToString( exclude = { "data" } )
final class Message {
    public final ByteSequence md5;
    public final byte messageType;
    public final short version;
    public final long clientId;
    public final byte[] data;

    Message( long clientId, byte messageType, short version, ByteSequence md5, byte[] data, int from, int length ) {
        this( clientId, messageType, version, md5, Arrays.copyOfRange( data, from, from + length ) );
    }

    Message( long clientId, byte messageType, short version, ByteSequence md5, byte[] data ) {
        this.clientId = clientId;
        this.version = version;
        this.md5 = md5;
        this.messageType = messageType;
        this.data = data;
    }

    public String getHexMd5() {
        return Hex.encodeHexString( md5.bytes );
    }

    public String getHexData() {
        return Hex.encodeHexString( data );
    }
}
