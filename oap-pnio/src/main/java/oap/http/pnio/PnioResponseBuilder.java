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

package oap.http.pnio;

import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class PnioResponseBuilder<State> extends PnioRequestHandler<State> {
    @Override
    public void handle( PnioExchange<State> pnioExchange, State state ) {
        try {
            OutputStream out = pnioExchange.responseBuffer.getOutputStream();
            boolean gzipSupported = pnioExchange.gzipSupported();
            if( gzipSupported ) out = new GZIPOutputStream( out );
            PnioExchange.HttpResponse httpResponse = pnioExchange.httpResponse;
            accept( pnioExchange, state, httpResponse, out );
            out.close();

            HttpServerExchange exchange = pnioExchange.exchange;
            httpResponse.headers.forEach( exchange::setResponseHeader );
            httpResponse.cookies.forEach( exchange::setResponseCookie );
            if( gzipSupported ) {
                exchange.setResponseHeader( Http.Headers.CONTENT_ENCODING, "gzip" );
            }

        } catch( BufferOverflowException e ) {
            pnioExchange.completeWithBufferOverflow( false );
        } catch( Throwable e ) {
            pnioExchange.completeWithFail( e );
        }
    }

    public abstract void accept( PnioExchange<State> requestState, State state, PnioExchange.HttpResponse httpResponse, OutputStream outputStream ) throws IOException;

    @Override
    public boolean isCpu() {
        return true;
    }
}
