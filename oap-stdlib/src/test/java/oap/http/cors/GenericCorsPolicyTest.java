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

package oap.http.cors;

import oap.http.Context;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.ServerHttpContext;
import oap.http.testng.MockHttpServer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericCorsPolicyTest {

    @Test
    public void shouldVerifyDefaultAllowMethods() throws UnknownHostException {
        var basicHttpRequest = new BasicHttpRequest( "GET", "http://test.com" );
        basicHttpRequest.addHeader( "Origin", "*" );
        basicHttpRequest.addHeader( "Host", "some-host" );

        var request = new Request( basicHttpRequest, new Context( "not important",
            InetAddress.getLocalHost(),
            new ServerHttpContext( new MockHttpServer(), new BasicHttpContext(), Protocol.HTTP, null ) )
        );

        final RequestCors requestCors = GenericCorsPolicy.DEFAULT.getCors( request );

        assertThat( requestCors.allowMethods ).isEqualTo( "HEAD, POST, GET, PUT, DELETE, OPTIONS" );
    }

}
