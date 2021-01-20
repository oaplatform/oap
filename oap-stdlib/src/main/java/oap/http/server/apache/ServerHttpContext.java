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

package oap.http.server.apache;

import lombok.ToString;
import oap.http.Protocol;
import oap.http.server.HttpServer;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;

@ToString
public class ServerHttpContext implements HttpContext, Closeable {
    public final Protocol protocol;
    public final DefaultBHttpServerConnection connection;
    private final HttpContext httpContext;
    public final HttpServer httpServer;
    public long start = System.nanoTime();

    public ServerHttpContext( HttpServer httpServer, HttpContext httpContext, Protocol protocol, DefaultBHttpServerConnection connection ) {
        this.httpServer = httpServer;
        this.httpContext = httpContext;
        this.protocol = protocol;
        this.connection = connection;
    }

    @Override
    public Object getAttribute( String id ) {
        return httpContext.getAttribute( id );
    }

    @Override
    public void setAttribute( String id, Object obj ) {
        httpContext.setAttribute( id, obj );
    }

    @Override
    public Object removeAttribute( String id ) {
        return httpContext.removeAttribute( id );
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
