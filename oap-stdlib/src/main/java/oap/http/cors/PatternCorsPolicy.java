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

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.http.Request;

import java.util.List;
import java.util.regex.Pattern;

import static oap.http.cors.RequestCors.NO_ORIGIN;

@EqualsAndHashCode
@ToString
@Slf4j
public class PatternCorsPolicy implements CorsPolicy {

    /**
     * use generic cors policy
     */
    @Deprecated
    public static final PatternCorsPolicy DEFAULT = new PatternCorsPolicy( "^[^:/]*\\.oaplatform\\.org$",
        "Content-type, Authorization", true, ImmutableList.of( "HEAD", "POST", "GET", "PUT", "DELETE", "OPTIONS" ) );

    public final Pattern domainPattern;
    public final String allowHeaders;
    public final boolean allowCredentials;
    public boolean autoOptions = true;
    public List<String> allowMethods;

    public PatternCorsPolicy( String domainRegexp, String allowHeaders, boolean allowCredentials, List<String> allowMethods ) {
        this.domainPattern = Pattern.compile( domainRegexp );
        this.allowHeaders = allowHeaders;
        this.allowCredentials = allowCredentials;
        this.allowMethods = allowMethods;
    }

    @Override
    public RequestCors getCors( Request request ) {
        String origin = request.header( "Origin" ).orElse( NO_ORIGIN );

        boolean matches = domainPattern.matcher( origin ).matches();
        log.trace( "origin = {}, domainPattern = {}, matches = {}", origin, domainPattern, matches );

        String allowedOrigin = matches ? origin : NO_ORIGIN;

        return new RequestCors( allowedOrigin, allowHeaders, allowCredentials, autoOptions, allowMethods );
    }
}
