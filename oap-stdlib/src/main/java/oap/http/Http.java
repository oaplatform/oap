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

@SuppressWarnings( "checkstyle:InterfaceIsType" )
public interface Http {
    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface StatusCode {
        int CONTINUE = io.undertow.util.StatusCodes.CONTINUE;
        int SWITCHING_PROTOCOLS = io.undertow.util.StatusCodes.SWITCHING_PROTOCOLS;
        int PROCESSING = io.undertow.util.StatusCodes.PROCESSING;
        int OK = io.undertow.util.StatusCodes.OK;
        int CREATED = io.undertow.util.StatusCodes.CREATED;
        int ACCEPTED = io.undertow.util.StatusCodes.ACCEPTED;
        int NON_AUTHORITATIVE_INFORMATION = io.undertow.util.StatusCodes.NON_AUTHORITATIVE_INFORMATION;
        int NO_CONTENT = io.undertow.util.StatusCodes.NO_CONTENT;
        int RESET_CONTENT = io.undertow.util.StatusCodes.RESET_CONTENT;
        int PARTIAL_CONTENT = io.undertow.util.StatusCodes.PARTIAL_CONTENT;
        int MULTI_STATUS = io.undertow.util.StatusCodes.MULTI_STATUS;
        int ALREADY_REPORTED = io.undertow.util.StatusCodes.MULTI_STATUS;
        int IM_USED = io.undertow.util.StatusCodes.IM_USED;
        int MULTIPLE_CHOICES = io.undertow.util.StatusCodes.MULTIPLE_CHOICES;
        int MOVED_PERMANENTLY = io.undertow.util.StatusCodes.MOVED_PERMANENTLY;
        int FOUND = io.undertow.util.StatusCodes.FOUND;
        int SEE_OTHER = io.undertow.util.StatusCodes.SEE_OTHER;
        int NOT_MODIFIED = io.undertow.util.StatusCodes.NOT_MODIFIED;
        int USE_PROXY = io.undertow.util.StatusCodes.USE_PROXY;
        int TEMPORARY_REDIRECT = io.undertow.util.StatusCodes.TEMPORARY_REDIRECT;
        int PERMANENT_REDIRECT = io.undertow.util.StatusCodes.PERMANENT_REDIRECT;
        int BAD_REQUEST = io.undertow.util.StatusCodes.BAD_REQUEST;
        int UNAUTHORIZED = io.undertow.util.StatusCodes.UNAUTHORIZED;
        int FORBIDDEN = io.undertow.util.StatusCodes.FORBIDDEN;
        int NOT_FOUND = io.undertow.util.StatusCodes.NOT_FOUND;
        int METHOD_NOT_ALLOWED = io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
        int NOT_ACCEPTABLE = io.undertow.util.StatusCodes.NOT_ACCEPTABLE;
        int PROXY_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.PROXY_AUTHENTICATION_REQUIRED;
        int REQUEST_TIME_OUT = io.undertow.util.StatusCodes.REQUEST_TIME_OUT;
        int CONFLICT = io.undertow.util.StatusCodes.CONFLICT;
        int GONE = io.undertow.util.StatusCodes.GONE;
        int LENGTH_REQUIRED = io.undertow.util.StatusCodes.LENGTH_REQUIRED;
        int PRECONDITION_FAILED = io.undertow.util.StatusCodes.PRECONDITION_FAILED;
        int REQUEST_ENTITY_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_ENTITY_TOO_LARGE;
        int REQUEST_URI_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_URI_TOO_LARGE;
        int UNSUPPORTED_MEDIA_TYPE = io.undertow.util.StatusCodes.UNSUPPORTED_MEDIA_TYPE;
        int REQUEST_RANGE_NOT_SATISFIABLE = io.undertow.util.StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE;
        int EXPECTATION_FAILED = io.undertow.util.StatusCodes.EXPECTATION_FAILED;
        int UNPROCESSABLE_ENTITY = io.undertow.util.StatusCodes.UNPROCESSABLE_ENTITY;
        int LOCKED = io.undertow.util.StatusCodes.LOCKED;
        int FAILED_DEPENDENCY = io.undertow.util.StatusCodes.FAILED_DEPENDENCY;
        int UPGRADE_REQUIRED = io.undertow.util.StatusCodes.UPGRADE_REQUIRED;
        int PRECONDITION_REQUIRED = io.undertow.util.StatusCodes.PRECONDITION_REQUIRED;
        int TOO_MANY_REQUESTS = io.undertow.util.StatusCodes.TOO_MANY_REQUESTS;
        int REQUEST_HEADER_FIELDS_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_HEADER_FIELDS_TOO_LARGE;
        int INTERNAL_SERVER_ERROR = io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
        int NOT_IMPLEMENTED = io.undertow.util.StatusCodes.NOT_IMPLEMENTED;
        int BAD_GATEWAY = io.undertow.util.StatusCodes.BAD_GATEWAY;
        int SERVICE_UNAVAILABLE = io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;
        int GATEWAY_TIME_OUT = io.undertow.util.StatusCodes.GATEWAY_TIME_OUT;
        int HTTP_VERSION_NOT_SUPPORTED = io.undertow.util.StatusCodes.HTTP_VERSION_NOT_SUPPORTED;
        int INSUFFICIENT_STORAGE = io.undertow.util.StatusCodes.INSUFFICIENT_STORAGE;
        int LOOP_DETECTED = io.undertow.util.StatusCodes.LOOP_DETECTED;
        int NOT_EXTENDED = io.undertow.util.StatusCodes.NOT_EXTENDED;
        int NETWORK_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.NETWORK_AUTHENTICATION_REQUIRED;
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface Headers {
        String CONTENT_ENCODING = io.undertow.util.Headers.CONTENT_ENCODING_STRING;
        String ACCEPT_ENCODING = io.undertow.util.Headers.ACCEPT_ENCODING_STRING;
        String CONTENT_TYPE = io.undertow.util.Headers.CONTENT_TYPE_STRING;
        String LOCATION = io.undertow.util.Headers.LOCATION_STRING;
        String AUTHORIZATION = io.undertow.util.Headers.AUTHORIZATION_STRING;
        String DATE = io.undertow.util.Headers.DATE_STRING;
        String CONNECTION = io.undertow.util.Headers.CONNECTION_STRING;
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    interface ContentType {
        String TEXT_TSV = "text/tab-separated-values";
        String TEXT_CSV = "text/csv";
        String TEXT_HTML = "text/html";
        String TEXT_PLAIN = "text/plain";
        String TEXT_XML = "text/xml";
        String APPLICATION_JSON = "application/json";
        String APPLICATION_XML = "application/xml";
        String APPLICATION_OCTET_STREAM = "application/octet-stream";
        String MULTIPART_FORM_DATA = "multipart/form-data";
        String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        String APPLICATION_X_TAR = "application/x-tar";
    }
}
