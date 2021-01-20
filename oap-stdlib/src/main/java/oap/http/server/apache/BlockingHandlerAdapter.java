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

import lombok.extern.slf4j.Slf4j;
import oap.http.Context;
import oap.http.Protocol;
import oap.http.Request;
import oap.http.Response;
import oap.http.cors.CorsPolicy;
import oap.http.server.Handler;
import oap.net.Inet;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import static java.util.Objects.requireNonNull;
import static oap.http.HttpResponse.FORBIDDEN;
import static oap.http.HttpResponse.NO_CONTENT;

@Slf4j
class BlockingHandlerAdapter implements HttpRequestHandler {
    private final Protocol protocol;
    private final String location;
    private final Handler handler;
    private final CorsPolicy corsPolicy;

    BlockingHandlerAdapter( String location, Handler handler,
                            CorsPolicy corsPolicy, Protocol protocol ) {
        this.location = location;
        this.handler = requireNonNull( handler );
        this.corsPolicy = corsPolicy;
        this.protocol = protocol;
    }

    @Override
    public void handle( HttpRequest httpRequest,
                        HttpResponse httpResponse,
                        HttpContext httpContext ) {
        log.trace( "Handling [{}]", httpRequest );

        var serverHttpContext = ( ServerHttpContext ) httpContext;

        var connection = serverHttpContext.connection;
        var remoteAddress = connection.getRemoteAddress();

        var request = new Request( httpRequest, new Context( location, remoteAddress, serverHttpContext ) );

        var cors = corsPolicy.getCors( request );
        var response = new Response( request, httpResponse, cors );

        if( Protocol.LOCAL.equals( this.protocol ) && !Inet.isLocalAddress( remoteAddress ) )
            response.respond( FORBIDDEN );
        else if( cors.autoOptions && request.getHttpMethod() == Request.HttpMethod.OPTIONS )
            response.respond( NO_CONTENT );
        else handler.handle( request, response );
    }
}
