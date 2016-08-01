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

import lombok.extern.slf4j.Slf4j;
import oap.http.Client;
import oap.util.Result;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public class RemoteInvocationHandler implements InvocationHandler {

   private static final long DEFAULT_TIMEOUT = 5000L;

   private final URI uri;
   private final FST fst;
   private final String service;
   private final Client client;
   private final long timeout;

   private RemoteInvocationHandler( URI uri, String service, Path certificateLocation, String certificatePassword, Optional<Long> timeout ) {
      this.uri = uri;
      this.service = service;
      this.timeout = timeout.orElse( DEFAULT_TIMEOUT );
      this.fst = new FST();
      this.client = new Client( certificateLocation, certificatePassword, ( int ) this.timeout, ( int ) this.timeout )
         .onTimeout( () -> log.error( "timeout invoking {}", uri ) )
         .onError( e -> log.error( "error invoking {}: {}", uri, e ) );
   }

   public static Object proxy( URI uri, String service, Class<?> clazz,
                               Path certificateLocation, String certificatePassword, Optional<Long> timeout ) {
      log.debug( "remote interface for {} at {} wich certs {}", service, uri, certificateLocation );
      return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz },
         new RemoteInvocationHandler( uri, service, certificateLocation, certificatePassword, timeout ) );
   }

   @Override
   @SuppressWarnings( "unchecked" )
   public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
      if( method.getDeclaringClass() == Object.class ) {
         return method.invoke( this, args );
      }
      Parameter[] parameters = method.getParameters();
      List<RemoteInvocation.Argument> arguments = new ArrayList<>();

      for( int i = 0; i < parameters.length; i++ ) {
         arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
            parameters[i].getType(), args[i] ) );
      }

      return client.post( uri.toString(),
         fst.conf.asByteArray( new RemoteInvocation( service, method.getName(), arguments ) ),
         timeout )
         .<Result<Object, Throwable>>map( response -> {
            if( response.code == HTTP_OK ) {
               return response.content
                  .map( b -> ( Result<Object, Throwable> ) fst.conf.asObject( b ) )
                  .orElse( Result.failure( new RemoteInvocationException( "no content " + uri ) ) );
            } else return Result.failure( new RemoteInvocationException( response.code + " " + response.reasonPhrase
               + "\n" + response.contentString ) );
         } )
         .orElseThrow( () -> new RemoteInvocationException( "invocation failed " + uri ) )
         .orElseThrow( t -> t );
   }

   @Override
   public String toString() {
      return "remote:" + service + "@" + uri;
   }
}
