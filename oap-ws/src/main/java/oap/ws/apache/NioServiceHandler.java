/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
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
package oap.ws.apache;

import oap.ws.Context;
import oap.ws.RawService;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class NioServiceHandler implements HttpAsyncRequestHandler<HttpRequest> {
    private static Logger logger = getLogger( NioServiceHandler.class );

    private String name;
    private String location;
    private RawService service;

    public NioServiceHandler( String name, String location, RawService service ) {
        this.name = name;
        this.location = location;
        this.service = service;
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest( HttpRequest httpRequest,
        HttpContext httpContext ) throws HttpException, IOException {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle( HttpRequest req, HttpAsyncExchange httpAsyncExchange,
        HttpContext ctx ) throws HttpException, IOException {
        if( logger.isTraceEnabled() ) logger.trace( "handling " + req );
        HttpInetConnection connection = (HttpInetConnection) ctx.getAttribute( HttpCoreContext.HTTP_CONNECTION );
        service.perform(
            new ApacheRequest( req, new Context( name, location, connection.getRemoteAddress() ) ),
            new ApacheResponse( httpAsyncExchange.getResponse() )
        );

        httpAsyncExchange.submitResponse();
    }
}
