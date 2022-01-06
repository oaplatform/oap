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

public final class Headers {
    public static final String CONTENT_ENCODING = io.undertow.util.Headers.CONTENT_ENCODING_STRING;
    public static final String ACCEPT_ENCODING = io.undertow.util.Headers.ACCEPT_ENCODING_STRING;
    public static final String CONTENT_TYPE = io.undertow.util.Headers.CONTENT_TYPE_STRING;
    public static final String LOCATION = io.undertow.util.Headers.LOCATION_STRING;
    public static final String AUTHORIZATION = io.undertow.util.Headers.AUTHORIZATION_STRING;
    public static final String DATE = io.undertow.util.Headers.DATE_STRING;
    public static final String CONNECTION = io.undertow.util.Headers.CONNECTION_STRING;

    private Headers() {
    }
}
