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

import oap.http.Url;
import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class UrlTest {
    @Test
    public void parseQuery() {
        assertEquals( Url.parseQuery( "a=&b=2" ), Maps.listmmap( __( "a", "" ), __( "b", "2" ) ) );
        assertEquals( Url.parseQuery( "a=1&b=2&" ), Maps.listmmap( __( "a", "1" ), __( "b", "2" ) ) );
        assertEquals( Url.parseQuery( "a=1&b=2&b=3&b=2" ),
            Maps.listmmap( __( "a", "1" ), __( "b", "2" ), __( "b", "3" ), __( "b", "2" ) ) );
    }

    @Test
    public void subdomains() {
        assertThat( Url.subdomains( null ) ).isEmpty();
        assertThat( Url.subdomains( "test" ) ).containsSequence( "test" );
        assertThat( Url.subdomains( "test.com" ) ).containsSequence( "com", "test.com" );
        assertThat( Url.subdomains( "www.test.com" ) ).containsSequence( "com", "test.com", "www.test.com" );
        assertThat( Url.subdomains( "www.a.test.com" ) ).containsSequence( "com", "test.com", "a.test.com",
            "www.a.test.com" );
    }

    @Test
    public void domainOf() {
        assertThat( Url.domainOf( null ) ).isNull();
        assertString( Url.domainOf( "test" ) ).isEqualTo( "test" );
        assertString( Url.domainOf( "test.com" ) ).isEqualTo( "test.com" );
        assertString( Url.domainOf( "http://test.com" ) ).isEqualTo( "test.com" );
        assertString( Url.domainOf( "https://test.com" ) ).isEqualTo( "test.com" );
        assertString( Url.domainOf( "https://test.com/" ) ).isEqualTo( "test.com" );
        assertString( Url.domainOf( "https://test.com?aaa=bbb" ) ).isEqualTo( "test.com" );
        assertString( Url.domainOf( "https://test.com#sss" ) ).isEqualTo( "test.com" );
    }
}
