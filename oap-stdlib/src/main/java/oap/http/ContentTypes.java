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

import oap.io.MimeTypes;

/**
 * @see Http.ContentType
 */
@Deprecated
//Use MimeType instead
public class ContentTypes {
    public static final String TEXT_TSV = MimeTypes.TEXT_TAB_SEPARATED_VALUES;
    public static final String TEXT_CSV = "text/csv";
    public static final String TEXT_HTML = MimeTypes.TEXT_HTML;
    public static final String TEXT_PLAIN = MimeTypes.TEXT_PLAIN;
    public static final String TEXT_XML = "text/xml";
    public static final String APPLICATION_JSON = MimeTypes.APPLICATION_JSON;
    public static final String APPLICATION_XML = MimeTypes.APPLICATION_XML;
    public static final String APPLICATION_OCTET_STREAM = MimeTypes.APPLICATION_OCTET_STREAM;
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_X_TAR = MimeTypes.APPLICATION_X_TAR;
}
