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

/**
 * @see Http.StatusCode
 */
@Deprecated
public final class HttpStatusCodes {
    public static final int CONTINUE = io.undertow.util.StatusCodes.CONTINUE;
    public static final int SWITCHING_PROTOCOLS = io.undertow.util.StatusCodes.SWITCHING_PROTOCOLS;
    public static final int PROCESSING = io.undertow.util.StatusCodes.PROCESSING;
    public static final int OK = io.undertow.util.StatusCodes.OK;
    public static final int CREATED = io.undertow.util.StatusCodes.CREATED;
    public static final int ACCEPTED = io.undertow.util.StatusCodes.ACCEPTED;
    public static final int NON_AUTHORITATIVE_INFORMATION = io.undertow.util.StatusCodes.NON_AUTHORITATIVE_INFORMATION;
    public static final int NO_CONTENT = io.undertow.util.StatusCodes.NO_CONTENT;
    public static final int RESET_CONTENT = io.undertow.util.StatusCodes.RESET_CONTENT;
    public static final int PARTIAL_CONTENT = io.undertow.util.StatusCodes.PARTIAL_CONTENT;
    public static final int MULTI_STATUS = io.undertow.util.StatusCodes.MULTI_STATUS;
    public static final int ALREADY_REPORTED = io.undertow.util.StatusCodes.MULTI_STATUS;
    public static final int IM_USED = io.undertow.util.StatusCodes.IM_USED;
    public static final int MULTIPLE_CHOICES = io.undertow.util.StatusCodes.MULTIPLE_CHOICES;
    public static final int MOVED_PERMANENTLY = io.undertow.util.StatusCodes.MOVED_PERMANENTLY;
    public static final int FOUND = io.undertow.util.StatusCodes.FOUND;
    public static final int SEE_OTHER = io.undertow.util.StatusCodes.SEE_OTHER;
    public static final int NOT_MODIFIED = io.undertow.util.StatusCodes.NOT_MODIFIED;
    public static final int USE_PROXY = io.undertow.util.StatusCodes.USE_PROXY;
    public static final int TEMPORARY_REDIRECT = io.undertow.util.StatusCodes.TEMPORARY_REDIRECT;
    public static final int PERMANENT_REDIRECT = io.undertow.util.StatusCodes.PERMANENT_REDIRECT;
    public static final int BAD_REQUEST = io.undertow.util.StatusCodes.BAD_REQUEST;
    public static final int UNAUTHORIZED = io.undertow.util.StatusCodes.UNAUTHORIZED;
    public static final int FORBIDDEN = io.undertow.util.StatusCodes.FORBIDDEN;
    public static final int NOT_FOUND = io.undertow.util.StatusCodes.NOT_FOUND;
    public static final int METHOD_NOT_ALLOWED = io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;
    public static final int NOT_ACCEPTABLE = io.undertow.util.StatusCodes.NOT_ACCEPTABLE;
    public static final int PROXY_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.PROXY_AUTHENTICATION_REQUIRED;
    public static final int REQUEST_TIME_OUT = io.undertow.util.StatusCodes.REQUEST_TIME_OUT;
    public static final int CONFLICT = io.undertow.util.StatusCodes.CONFLICT;
    public static final int GONE = io.undertow.util.StatusCodes.GONE;
    public static final int LENGTH_REQUIRED = io.undertow.util.StatusCodes.LENGTH_REQUIRED;
    public static final int PRECONDITION_FAILED = io.undertow.util.StatusCodes.PRECONDITION_FAILED;
    public static final int REQUEST_ENTITY_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_ENTITY_TOO_LARGE;
    public static final int REQUEST_URI_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_URI_TOO_LARGE;
    public static final int UNSUPPORTED_MEDIA_TYPE = io.undertow.util.StatusCodes.UNSUPPORTED_MEDIA_TYPE;
    public static final int REQUEST_RANGE_NOT_SATISFIABLE = io.undertow.util.StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE;
    public static final int EXPECTATION_FAILED = io.undertow.util.StatusCodes.EXPECTATION_FAILED;
    public static final int UNPROCESSABLE_ENTITY = io.undertow.util.StatusCodes.UNPROCESSABLE_ENTITY;
    public static final int LOCKED = io.undertow.util.StatusCodes.LOCKED;
    public static final int FAILED_DEPENDENCY = io.undertow.util.StatusCodes.FAILED_DEPENDENCY;
    public static final int UPGRADE_REQUIRED = io.undertow.util.StatusCodes.UPGRADE_REQUIRED;
    public static final int PRECONDITION_REQUIRED = io.undertow.util.StatusCodes.PRECONDITION_REQUIRED;
    public static final int TOO_MANY_REQUESTS = io.undertow.util.StatusCodes.TOO_MANY_REQUESTS;
    public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = io.undertow.util.StatusCodes.REQUEST_HEADER_FIELDS_TOO_LARGE;
    public static final int INTERNAL_SERVER_ERROR = io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
    public static final int NOT_IMPLEMENTED = io.undertow.util.StatusCodes.NOT_IMPLEMENTED;
    public static final int BAD_GATEWAY = io.undertow.util.StatusCodes.BAD_GATEWAY;
    public static final int SERVICE_UNAVAILABLE = io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;
    public static final int GATEWAY_TIME_OUT = io.undertow.util.StatusCodes.GATEWAY_TIME_OUT;
    public static final int HTTP_VERSION_NOT_SUPPORTED = io.undertow.util.StatusCodes.HTTP_VERSION_NOT_SUPPORTED;
    public static final int INSUFFICIENT_STORAGE = io.undertow.util.StatusCodes.INSUFFICIENT_STORAGE;
    public static final int LOOP_DETECTED = io.undertow.util.StatusCodes.LOOP_DETECTED;
    public static final int NOT_EXTENDED = io.undertow.util.StatusCodes.NOT_EXTENDED;
    public static final int NETWORK_AUTHENTICATION_REQUIRED = io.undertow.util.StatusCodes.NETWORK_AUTHENTICATION_REQUIRED;

    private HttpStatusCodes() {
    }
}
