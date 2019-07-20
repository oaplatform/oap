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

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class WsServicesTest {
    @Test
    public void compile() {
        assertEquals( WsServices.compile( "/y/{year:(\\d\\d\\d\\d)}/{month}/{date}" ).toString(), "^/y/(\\d\\d\\d\\d)/([^/]+)/([^/]+)$" );
        assertEquals( WsServices.compile( "/y/{year:(\\d{4})}/{month}/{date}" ).toString(), "^/y/(\\d{4})/([^/]+)/([^/]+)$" );
        assertEquals( WsServices.compile( "/" ).toString(), "^/?$" );
    }

    @Test
    public void pathParam() {
        String mapping = "/y/{year:(\\d{4})}/{month}/{date}";
        String path = "/y/2009/April/12";
        assertEquals( Optional.of( "2009" ), WsServices.pathParam( mapping, path, "year" ) );
        assertEquals( Optional.of( "April" ), WsServices.pathParam( mapping, path, "month" ) );
        assertEquals( Optional.of( "12" ), WsServices.pathParam( mapping, path, "date" ) );
    }
}
