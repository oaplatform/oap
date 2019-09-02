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

import com.google.common.base.Preconditions;
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

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public final class RemoteInvocationHandler implements InvocationHandler {

    private final URI uri;
    private final FST fst;
    private final int retry;
    private final String service;
    private final Client client;
    private final long timeout;

    private RemoteInvocationHandler( URI uri,
                                     String service,
                                     Path certificateLocation,
                                     String certificatePassword,
                                     long timeout,
                                     FST.SerializationMethod serialization,
                                     int retry ) {
        this.uri = uri;
        this.service = service;
        this.timeout = timeout;
        this.fst = new FST( serialization );
        this.retry = retry;
        this.client = Client.custom( certificateLocation, certificatePassword, ( int ) this.timeout, ( int ) this.timeout )
            .onTimeout( client -> {
                log.error( "timeout invoking {}", uri );
                client.reset();
            } )
            .onError( ( c, e ) -> log.error( "error invoking {}: {}", uri, e ) )
            .setMaxConnPerRoute( 1 )
            .setMaxConnTotal( 1 )
            .build();
    }

    public static Object proxy( RemoteLocation remote, Class<?> clazz ) {
        return proxy( remote.url, remote.name, clazz, remote.certificateLocation,
            remote.certificatePassword, remote.timeout, remote.serialization, remote.retry );
    }

    private static Object proxy( URI uri, String service, Class<?> clazz,
                                 Path certificateLocation, String certificatePassword,
                                 long timeout, FST.SerializationMethod serialization, int retry ) {
        log.debug( "remote interface for {} at {} wich certs {}", service, uri, certificateLocation );
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[] { clazz },
            new RemoteInvocationHandler( uri, service, certificateLocation, certificatePassword, timeout, serialization, retry ) );
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

        Preconditions.checkNotNull( uri, "uri == null, service name = " + service + ", method name = " + method.getName() );

        final byte[] content = fst.conf.asByteArray( new RemoteInvocation( service, method.getName(), arguments ) );

        Throwable retException = null;

        for( int i = 0; i < retry; i++ ) {
            if( retException != null )
                log.trace( retException.getMessage(), retException );
            try {
                var response = client.post( uri.toString(), content, timeout ).orElse( null );
                if( response == null ) continue;

                if( response.code == HTTP_OK ) {
                    var b = response.content();
                    if( b != null ) {
                        var res = ( Result<Object, Throwable> ) fst.conf.asObject( b );

                        if( res.isSuccess() ) return res.successValue;

                        retException = res.failureValue;
                        continue;
                    }

                    retException = new RemoteInvocationException( "remote service uri = " + uri
                        + ", service name = " + service
                        + ", method name = " + method.getName() + ": no content" );
                } else
                    retException = new RemoteInvocationException( "remote service uri = " + uri
                        + ", service name = " + service
                        + ", method name = " + method.getName()
                        + ": response code = " + response.code
                        + ", phrase = " + response.reasonPhrase
                        + "\n content = " + response.contentString() );
            } catch( Exception e ) {
                retException = e;
            }

            log.trace( "retrying... remote service uri = {}, service name = {}, method name = {}", uri, service, method.getName() );
        }

        throw retException != null ? retException : new RemoteInvocationException( "invocation failed " + uri );
    }

    @Override
    public String toString() {
        return "remote:" + service + "@" + uri;
    }
}
