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

import org.apache.http.entity.ContentType;

import java.util.Map;
import java.util.Optional;

public interface SimpleClient {
    class Response {
        public final int code;
        public final byte[] raw;
        public final String body;
        public final String contentType;
        public final String reasonPhrase;
        private final Map<String, String> headers;

        public Response( int code, String reasonPhrase, ContentType contentType, Map<String, String> headers,
                         byte[] body ) {
            this.code = code;
            this.reasonPhrase = reasonPhrase;
            this.headers = headers;
            this.contentType = contentType != null ? contentType.toString() : null;
            this.raw = body;
            this.body = raw != null ? new String( raw ) : null;
        }

        public Response( int code, String reasonPhrase, Map<String, String> headers ) {
            this( code, reasonPhrase, null, headers, null );
        }

        public Optional<String> getHeader( String name ) {
            return Optional.ofNullable( headers.get( name ) );
        }

        @Override
        public String toString() {
            return code + " " + reasonPhrase;
        }
    }
}
