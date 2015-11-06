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
package oap.ws;

import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import oap.http.Url;
import org.testng.annotations.Test;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;

public class UrlTest extends AbstractTest {
    @Test
    public void parseQuery() {
        assertEquals( Url.parseQuery( "a=&b=2" ), Maps.listmmap( __( "a", "" ), __( "b", "2" ) ) );
        assertEquals( Url.parseQuery( "a=1&b=2&" ), Maps.listmmap( __( "a", "1" ), __( "b", "2" ) ) );
        assertEquals( Url.parseQuery( "a=1&b=2&b=3&b=2" ),
            Maps.listmmap( __( "a", "1" ), __( "b", "2" ), __( "b", "3" ), __( "b", "2" ) ) );
    }

    @Test
    public void subdomains() {
        assertEquals( Url.subdomains( "test" ), Lists.of( "test" ) );
        assertEquals( Url.subdomains( "test.com" ), Lists.of( "com", "test.com" ) );
        assertEquals( Url.subdomains( "www.test.com" ), Lists.of( "com", "test.com", "www.test.com" ) );
        assertEquals( Url.subdomains( "www.a.test.com" ), Lists.of( "com", "test.com", "a.test.com",
            "www.a.test.com" ) );
    }


}
