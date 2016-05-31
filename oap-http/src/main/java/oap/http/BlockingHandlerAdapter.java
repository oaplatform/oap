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
package oap.http;

import lombok.extern.slf4j.Slf4j;
import oap.http.cors.CorsPolicy;
import oap.http.cors.RequestCors;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.net.InetAddress;

import static oap.http.HttpResponse.HTTP_FORBIDDEN;
import static oap.http.HttpResponse.NO_CONTENT;
import static org.apache.http.protocol.HttpCoreContext.HTTP_CONNECTION;

@Slf4j
class BlockingHandlerAdapter implements HttpRequestHandler {
    private final Protocol protocol;
    private final String location;
    private final Handler handler;
    private final CorsPolicy corsPolicy;

    public BlockingHandlerAdapter( final String location, final Handler handler,
                                   final CorsPolicy corsPolicy, final Protocol protocol ) {
        this.location = location;
        this.handler = handler;
        this.corsPolicy = corsPolicy;
        this.protocol = protocol;
    }

    @Override
    public void handle( final HttpRequest httpRequest, final HttpResponse httpResponse,
                        final HttpContext httpContext ) throws IOException {
        log.trace( "Handling [{}]", httpRequest );

        final HttpInetConnection connection = ( HttpInetConnection ) httpContext.getAttribute( HTTP_CONNECTION );
        final InetAddress remoteAddress = connection.getRemoteAddress();

        final String httpContextProtocol = httpContext.getAttribute( "protocol" ).toString();
        final Request request = new Request( httpRequest, new Context( location, remoteAddress, httpContextProtocol ) );

        RequestCors cors = corsPolicy.getCors( request );
        final Response response = new Response( httpResponse, cors );

        if( Protocol.isLocal( remoteAddress, this.protocol ) ||
            Protocol.doesNotMatch( httpContextProtocol, this.protocol ) ) {
            response.respond( HTTP_FORBIDDEN );
        } else if( cors.autoOptions && request.httpMethod == Request.HttpMethod.OPTIONS ) {
            response.respond( NO_CONTENT );
        } else {
            handler.handle( request, response );
        }
    }
}
