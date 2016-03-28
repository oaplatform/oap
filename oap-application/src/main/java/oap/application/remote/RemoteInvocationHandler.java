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
package oap.application.remote;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.http.SimpleAsyncHttpClient;
import oap.http.SimpleClient;
import oap.io.IoStreams;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.io.IoStreams.Encoding.PLAIN;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class RemoteInvocationHandler implements InvocationHandler {

   private final URI uri;
   private final FST fst;
   private final String service;
   private final CloseableHttpAsyncClient closeableHttpAsyncClient;
   private final long timeout;

   private RemoteInvocationHandler( URI uri, String service,
                                    Path certificateLocation, String certificatePassword, Long timeout ) {
      this.uri = uri;
      this.service = service;
      this.timeout = timeout == null ? 5000 : timeout;
      this.fst = new FST();
      this.closeableHttpAsyncClient = HttpAsyncClients
         .custom()
         .setSSLContext( createSSLContext( certificateLocation, certificatePassword ) )
         .setMaxConnPerRoute( 1000 )
         .setMaxConnTotal( 10000 )
         .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
         .setDefaultRequestConfig( RequestConfig.custom().setRedirectsEnabled( false ).build() )
         .build();

      closeableHttpAsyncClient.start();
   }

   @Override
   public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
      Parameter[] parameters = method.getParameters();
      List<RemoteInvocation.Argument> arguments = new ArrayList<>();

      for( int i = 0; i < parameters.length; i++ ) {
         arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
            parameters[i].getType(), args[i] ) );
      }

      try {
         HttpPost post = new HttpPost( uri );

         post.setEntity( new ByteArrayEntity(
            fst.conf.asByteArray( new RemoteInvocation( service, method.getName(), arguments ) ),
            APPLICATION_OCTET_STREAM
         ) );

         SimpleClient.Response response = SimpleAsyncHttpClient.execute( closeableHttpAsyncClient, post, timeout );
         switch( response.code ) {
            case HTTP_OK:
               return method.getReturnType().equals( void.class ) ? null :
                  fst.conf.asObject( response.raw );
            default:
               throw new RemoteInvocationException( "code: " + response.code + ", message: " +
                  response.reasonPhrase + "\n" + response.body );
         }
      } catch( Exception e ) {
         if( log.isTraceEnabled() ) log.trace( e.getMessage(), e );
         else log.error( e.getMessage() );
         throw Throwables.propagate( e );
      }
   }

   public static Object proxy( URI uri, String service, Class<?> clazz,
                               Path certificateLocation, String certificatePassword, Long timeout ) {
      return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz },
         new RemoteInvocationHandler( uri, service, certificateLocation, certificatePassword, timeout ) );
   }

   private static SSLContext createSSLContext( Path certificateLocation, String certificatePassword ) {
      try {
         KeyStore keyStore = KeyStore.getInstance( "JKS" );
         keyStore.load( IoStreams.in( certificateLocation, PLAIN ),
            certificatePassword.toCharArray() );

         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
         trustManagerFactory.init( keyStore );

         SSLContext sslContext = SSLContext.getInstance( "TLS" );
         sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

         return sslContext;
      } catch( KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException e ) {
         throw Throwables.propagate( e );
      }
   }
}
