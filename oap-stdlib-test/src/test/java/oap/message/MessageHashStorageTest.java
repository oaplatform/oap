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

import oap.message.MessageHashStorage;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.io.IOException;

import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MessageHashStorageTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void testPersistence() throws IOException, DecoderException {
        var md5 = DigestUtils.getMd5Digest();
        var md51 = Hex.encodeHexString( md5.digest( "test".getBytes() ) );
        var md52 = Hex.encodeHexString( md5.digest( "test1".getBytes() ) );

        var mhs = new MessageHashStorage( 1024 );
        DateTimeUtils.setCurrentMillisFixed( 12 );
        mhs.add( ( byte ) 1, 11, md51 );
        DateTimeUtils.setCurrentMillisFixed( 456 );
        mhs.add( ( byte ) 1, 11, md52 );

        DateTimeUtils.setCurrentMillisFixed( 124 );
        mhs.add( ( byte ) 2, 12, md51 );

        var path = TestDirectoryFixture.testPath( "test" );
        mhs.store( path );

        assertFile( path ).hasContent( """
            ---
            1 - 11
            098f6bcd4621d373cade4e832627b4f6 - 12
            5a105e8b9d40e1329780d62ea2265d8a - 456
            ---
            2 - 12
            098f6bcd4621d373cade4e832627b4f6 - 124
            """.stripIndent() );

        var path2 = TestDirectoryFixture.testPath( "test2" );
        var mhs2 = new MessageHashStorage( 1024 );
        mhs2.load( path );
        mhs2.store( path2 );

        assertFile( path2 ).hasContent( """
            ---
            1 - 11
            098f6bcd4621d373cade4e832627b4f6 - 12
            5a105e8b9d40e1329780d62ea2265d8a - 456
            ---
            2 - 12
            098f6bcd4621d373cade4e832627b4f6 - 124
            """.stripIndent() );
    }

    @Test
    public void testFifo() {
        var md5 = DigestUtils.getMd5Digest();
        var md51 = Hex.encodeHexString( md5.digest( "test".getBytes() ) );
        var md52 = Hex.encodeHexString( md5.digest( "test1".getBytes() ) );
        var md53 = Hex.encodeHexString( md5.digest( "test3".getBytes() ) );

        var mhs = new MessageHashStorage( 2 );
        mhs.add( ( byte ) 1, 1, md51 );
        mhs.add( ( byte ) 1, 1, md52 );
        mhs.add( ( byte ) 1, 1, md53 );

        assertThat( mhs.size() ).isEqualTo( 2 );
        assertFalse( mhs.contains( ( byte ) 1, md51 ) );
        assertTrue( mhs.contains( ( byte ) 1, md52 ) );
        assertTrue( mhs.contains( ( byte ) 1, md53 ) );
    }
}
