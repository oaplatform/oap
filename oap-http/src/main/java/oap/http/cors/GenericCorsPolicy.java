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
import oap.http.Request;

import java.util.List;

import static oap.http.Request.HttpMethod.DELETE;
import static oap.http.Request.HttpMethod.GET;
import static oap.http.Request.HttpMethod.HEAD;
import static oap.http.Request.HttpMethod.OPTIONS;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.Request.HttpMethod.PUT;

@EqualsAndHashCode
@ToString
public class GenericCorsPolicy implements CorsPolicy {

   public static final GenericCorsPolicy DEFAULT = new GenericCorsPolicy("*", "Content-type, Authorization",
       true, ImmutableList.of( HEAD, POST, GET, PUT, DELETE, OPTIONS ) );

   public final String allowOrigin;
   public final String allowHeaders;
   public final boolean allowCredentials;
   public final boolean autoOptions = true;
   public final List<Request.HttpMethod> allowMethods;

   public GenericCorsPolicy( final String allowOrigin, final String allowHeaders,
                             final boolean allowCredentials, final List<Request.HttpMethod> allowMethods ) {
      this.allowOrigin = allowOrigin;
      this.allowHeaders = allowHeaders;
      this.allowCredentials = allowCredentials;
      this.allowMethods = allowMethods;
   }

   @Override
   public RequestCors getCors( final Request request ) {
      return new RequestCors( allowOrigin, allowHeaders, allowCredentials, autoOptions, allowMethods );
   }
}
