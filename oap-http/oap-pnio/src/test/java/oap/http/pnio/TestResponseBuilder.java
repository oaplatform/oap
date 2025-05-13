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

import lombok.extern.slf4j.Slf4j;
import oap.http.Http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class TestResponseBuilder extends PnioRequestHandler<TestState> {
    @Override
    public Type getType() {
        return Type.COMPUTE;
    }

    @Override
    public CompletableFuture<Void> handle( PnioExchange<TestState> pnioExchange, TestState testState ) throws IOException {
        String data = "name 'TestResponseBuilder' type " + getType() + " thread '" + Thread.currentThread().getName().substring( 7, 11 )
            + "' new thread " + !testState.oldThreadName.equals( Thread.currentThread().getName() );

        log.debug( data );

        OutputStream outputStream = null;
        try {
            outputStream = pnioExchange.responseBuffer.getOutputStream();

            if( pnioExchange.gzipSupported() ) {
                outputStream = new GZIPOutputStream( outputStream );
                pnioExchange.httpResponse.headers.put( Http.Headers.CONTENT_ENCODING, "gzip" );
            }
            outputStream.write( testState.sb.toString().getBytes( StandardCharsets.UTF_8 ) );

            pnioExchange.httpResponse.status = Http.StatusCode.OK;
            pnioExchange.httpResponse.contentType = Http.ContentType.TEXT_PLAIN;

            return CompletableFuture.completedFuture( null );
        } finally {
            if( outputStream != null ) outputStream.close();
        }
    }
}
