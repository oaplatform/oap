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

import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.ByteSequence;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static oap.testng.Asserts.assertFile;

/**
 * Created by igor.petrenko on 2019-12-13.
 */
public class MessageHashStorageTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    @Test
    public void testPersistence() throws IOException, DecoderException {
        var md5 = DigestUtils.getMd5Digest();
        var md5_1 = md5.digest( "test".getBytes() );
        var md5_2 = md5.digest( "test1".getBytes() );

        var mhs = new MessageHashStorage();
        DateTimeUtils.setCurrentMillisFixed( 12 );
        mhs.add( 1, 11, md5_1 );
        DateTimeUtils.setCurrentMillisFixed( 456 );
        mhs.add( 1, 11, md5_2 );

        DateTimeUtils.setCurrentMillisFixed( 124 );
        mhs.add( 2, 12, md5_1 );

        var path = Env.tmpPath( "test" );
        mhs.store( path );

        assertFile( path ).hasContent( """
            ---
            2 - 12
            098f6bcd4621d373cade4e832627b4f6 - 124
            ---
            1 - 11
            5a105e8b9d40e1329780d62ea2265d8a - 456
            098f6bcd4621d373cade4e832627b4f6 - 12
            """.stripIndent() );

        var path2 = Env.tmpPath( "test2" );
        var mhs2 = new MessageHashStorage();
        mhs2.load( path );
        mhs2.store( path2 );

        assertFile( path2 ).hasContent( """
            ---
            2 - 12
            098f6bcd4621d373cade4e832627b4f6 - 124
            ---
            1 - 11
            5a105e8b9d40e1329780d62ea2265d8a - 456
            098f6bcd4621d373cade4e832627b4f6 - 12
            """.stripIndent() );
    }
}
