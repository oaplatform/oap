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

package oap.http.cors;

import java.util.List;
import java.util.stream.Collectors;

public class RequestCors {

    public static final String NO_ORIGIN = "";

    public final String allowOrigin;
    public final String allowHeaders;
    public final String allowCredentials;
    public final String allowMethods;
    public final boolean autoOptions;

    public RequestCors( final String allowOrigin, final String allowHeaders, final boolean allowCredentials,
                        final boolean autoOptions, final List<String> allowMethods ) {
        this.allowOrigin = allowOrigin;
        this.allowHeaders = allowHeaders;
        this.allowCredentials = String.valueOf( allowCredentials );
        this.autoOptions = autoOptions;
        this.allowMethods = allowMethods.stream().collect( Collectors.joining( ", " ) );
    }

    public void setHeaders( final org.apache.http.HttpResponse response ) {
        response.setHeader( "Access-Control-Allow-Origin", allowOrigin );
        response.setHeader( "Access-Control-Allow-Headers", allowHeaders );
        response.setHeader( "Access-Control-Allow-Credentials", allowCredentials );
        response.setHeader( "Access-Control-Allow-Methods", allowMethods );
    }

}
