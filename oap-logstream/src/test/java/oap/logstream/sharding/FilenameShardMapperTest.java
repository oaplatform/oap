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

package oap.logstream.sharding;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class FilenameShardMapperTest {

    @Test
    public void shardParsing() {
        FilenameShardMapper mapper = new FilenameShardMapper( "(^\\d+)" );
        assertEquals( 100, mapper.getShardNumber( "myhost", "100/traffic/some/log/3/2016" ) );
        assertEquals( 24, mapper.getShardNumber( "myhost", "24/100/traffic/some/log/3/2016" ) );
    }

    @Test( expectedExceptions = IllegalArgumentException.class )
    public void noShardInPath() {
        FilenameShardMapper mapper = new FilenameShardMapper( "(^\\d+)" );
        assertEquals( 100, mapper.getShardNumber( "myhost", "blah/traffic/some/log/3/2016" ) );
    }
}
