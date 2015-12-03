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

import oap.testng.AbstractTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by Igor Petrenko on 03.12.2015.
 */
public class FilenameTest extends AbstractTest {

    @Test
    public void testFormatDate() throws Exception {
        DateTimeUtils.setCurrentMillisFixed( new DateTime( 2015, 12, 3, 11, 28, 30 ).getMillis() );
        final String s = Filename.formatDate( DateTime.now(), 5 );

        assertEquals( s, "2015-12-03-11-05" );
    }

    @Test
    public void testDirectoryName() throws Exception {
        assertEquals(Filename.directoryName( "2015-12-03-11-05" ), "2015-12/03");
    }
}