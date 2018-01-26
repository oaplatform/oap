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

package oap.ws.cat;

import oap.http.HttpResponse;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Strings;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;
import static oap.ws.cat.CatApi.table;

/**
 * Created by Admin on 09.06.2016.
 */
public class CatApiTest extends AbstractTest {
    @Test
    public void testTable() throws Exception {
        final HttpResponse table = table( Lists.of( "1", "test23" ), Lists.of( "bbbb", "2" ) );

        assertString( Strings.readString( table.contentEntity.getContent() ) ).isEqualTo( "1    test23\n"
            + "bbbb 2     \n" );
    }

}