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
import oap.io.FixedLengthArrayOutputStream;
import oap.io.IoStreams;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractResponseTask<State> extends AbstractRequestTask<State> {
    @Override
    public void accept( RequestTaskState<State> requestState, State state ) {
        try {
            FixedLengthArrayOutputStream fixedLengthArrayOutputStream = IoStreams.out( requestState.responseBuffer );
            OutputStream out = fixedLengthArrayOutputStream;
            boolean gzipSupported = requestState.gzipSupported();
            if( gzipSupported ) out = new GZIPOutputStream( out );
            accept( requestState, state, requestState.httpResponse, out );
            out.close();
            requestState.responseLength = fixedLengthArrayOutputStream.size();

            if( gzipSupported ) {
                requestState.exchange.setResponseHeader( Http.Headers.CONTENT_ENCODING, "gzip" );
            }
        } catch( BufferOverflowException e ) {
            requestState.completeWithBufferOverflow( false );
        } catch( Throwable e ) {
            requestState.completeWithFail( e );
        }
    }

    public abstract void accept( RequestTaskState<State> requestState, State state, RequestTaskState.HttpResponse httpResponse, OutputStream outputStream ) throws IOException;

    @Override
    public boolean isCpu() {
        return true;
    }
}
