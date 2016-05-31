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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.Request;

@EqualsAndHashCode
@ToString
public class GenericCorsPolicy implements CorsPolicy {
   public static final GenericCorsPolicy DEFAULT = new GenericCorsPolicy();
   public String allowOrigin = "*";
   public String allowHeaders = "Content-type, Authorization";
   public boolean allowCredentials = true;
   public boolean autoOptions = true;

   public GenericCorsPolicy() {
   }

   public GenericCorsPolicy( String allowOrigin, String allowHeaders, boolean allowCredentials ) {
      this.allowOrigin = allowOrigin;
      this.allowHeaders = allowHeaders;
      this.allowCredentials = allowCredentials;
   }

   @Override
   public RequestCors getCors( Request request ) {
      return new RequestCors( allowOrigin, allowHeaders, allowCredentials, autoOptions );
   }
}
