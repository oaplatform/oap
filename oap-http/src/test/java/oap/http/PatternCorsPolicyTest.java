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


import oap.http.cors.PatternCorsPolicy;
import oap.http.cors.RequestCors;
import org.apache.http.message.BasicHttpRequest;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.testng.Assert.assertEquals;


public class PatternCorsPolicyTest {

   PatternCorsPolicy cors = new PatternCorsPolicy( "^(http)s?(://)[^/]*[\\.]?oaplatform\\.org/?", "Autorization", true );

   @Test
   public void testSameDomainOrigin( ) throws UnknownHostException {
      Request request = getRequest("http://oaplatform.org/", "http://oaplatform.org/api");
      assertEquals( cors.getCors( request ).allowOrigin, "http://oaplatform.org/");
   }

   @Test
   public void testSubDomainOrigin( ) throws UnknownHostException {
      String origin = "https://oap.oaplatform.org/";
      Request request = getRequest( origin, "https://oap.oaplatform.org/cors");
      assertEquals( cors.getCors( request ).allowOrigin, origin);
   }

   @Test
   public void testAnotherDomainOrigin( ) throws UnknownHostException {
      Request request = getRequest("http://example.com/", "http://example.com/path/to/api");
      assertEquals( cors.getCors( request ).allowOrigin, RequestCors.NO_ORIGIN);
   }

   private Request getRequest(String origin, String url) throws UnknownHostException {
      BasicHttpRequest get = new BasicHttpRequest( "GET", url );
      get.addHeader( "Origin", origin );
      get.addHeader( "Host", "some-host" );

      Context context = new Context( "not important", InetAddress.getLocalHost(), "not important" );
      return new Request( get, context );
   }


}