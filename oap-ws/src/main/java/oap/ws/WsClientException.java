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

import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

public class WsClientException extends WsException {
    public final List<String> errors;
    public int code = HTTP_BAD_REQUEST;

    public WsClientException( String message, int code, List<String> errors ) {
        super( message );
        this.code = code;
        this.errors = errors;
    }

    public WsClientException( String message, List<String> errors ) {
        super( message );
        this.errors = errors;
    }

    public WsClientException( String message ) {
        this( message, List.of( message ) );
    }

    public WsClientException( String message, Throwable cause ) {
        super( message, cause );
        this.errors = List.of( message );
    }

    public WsClientException( Throwable cause ) {
        super( cause );
        this.errors = List.of( cause.getMessage() );
    }
}
