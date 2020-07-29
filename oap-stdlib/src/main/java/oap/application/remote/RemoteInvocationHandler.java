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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.util.Result;
import oap.util.Stream;
import oap.util.Try;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public final class RemoteInvocationHandler implements InvocationHandler {
    private final Counter timeoutMetrics;
    private final Counter errorMetrics;
    private final Counter successMetrics;

    private final URI uri;
    private final FST fst;
    private final int retry;
    private final String service;
    private final HttpClient client;
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

        timeoutMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "timeout" ) );
        errorMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "error" ) );
        successMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "success" ) );

        log.debug( "initialize {} with certs {}", this, certificateLocation );

        var builder = HttpClient.newBuilder().connectTimeout( Duration.ofMillis( this.timeout ) );
        if( certificateLocation != null ) {
            var sslContext = oap.http.client.HttpClient.createSSLContext( certificateLocation, certificatePassword );
            builder = builder.sslContext( sslContext );
        }
        this.client = builder.build();
    }

    public static Object proxy( RemoteLocation remote, Class<?> clazz ) {
        return proxy( remote.url, remote.name, clazz, remote.certificateLocation,
            remote.certificatePassword, remote.timeout, remote.serialization, remote.retry );
    }

    private static Object proxy( URI uri, String service, Class<?> clazz,
                                 Path certificateLocation, String certificatePassword,
                                 long timeout, FST.SerializationMethod serialization, int retry ) {
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[] { clazz },
            new RemoteInvocationHandler( uri, service, certificateLocation, certificatePassword, timeout, serialization, retry ) );
    }

    private RemoteInvocationException throwException( String methodName, Throwable throwable ) {
        if( throwable instanceof RemoteInvocationException ) return ( RemoteInvocationException ) throwable;
        else if( throwable instanceof ExecutionException ) return throwException( methodName, throwable.getCause() );
        else return new RemoteInvocationException( "invocation failed " + this + "#" + methodName, throwable );
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if( uri == null ) throw new RemoteInvocationException( "uri == null, service " + service + "#" + method.getName() );

        if( method.getDeclaringClass() == Object.class ) return method.invoke( this, args );

        Result<Object, Throwable> result = invoke( method, args );
        if( result.isSuccess() ) return result.successValue;
        else throw result.failureValue;
    }

    private Result<Object, Throwable> invoke( Method method, Object[] args ) {
        Parameter[] parameters = method.getParameters();
        List<RemoteInvocation.Argument> arguments = new ArrayList<>();

        for( int i = 0; i < parameters.length; i++ )
            arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
                parameters[i].getType(), args[i] ) );


        var invocationB = getInvocation( method, arguments );

        Throwable lastException = null;
        for( int i = 0; i <= retry; i++ ) {
            log.trace( "{} {}#{}...", i > 0 ? "retrying" : "invoking", this, method.getName() );
            try {
                var bodyPublisher = HttpRequest.BodyPublishers.ofByteArray( invocationB );
                var request = HttpRequest.newBuilder( uri ).POST( bodyPublisher ).timeout( Duration.ofMillis( timeout ) ).build();
                var responseFuture = client.sendAsync( request, HttpResponse.BodyHandlers.ofInputStream() );
                var response = responseFuture.get( timeout, TimeUnit.MILLISECONDS );
                if( response.statusCode() == HTTP_OK && response.body() != null ) {
                    var inputStream = response.body();
                    var bis = new BufferedInputStream( inputStream );
                    var dis = new DataInputStream( bis );
                    var success = dis.readBoolean();

                    try {
                        if( !success ) {
                            try {
                                var throwable = ( Throwable ) fst.readObjectWithSize( dis );

                                if( throwable instanceof RemoteInvocationException )
                                    throw ( RemoteInvocationException ) throwable;

                                var failure = Result.failure( throwable );
                                successMetrics.increment();
                                return failure;
                            } finally {
                                dis.close();
                            }
                        } else {
                            var stream = dis.readBoolean();
                            if( stream ) {
                                var it = new Iterator<>() {
                                    private Object obj = null;
                                    private boolean end = false;

                                    @SneakyThrows
                                    @Override
                                    public boolean hasNext() {
                                        if( end ) return false;

                                        if( obj != null ) return true;

                                        var next = dis.readInt();
                                        if( next > 0 ) {
                                            obj = fst.readObject( dis, next );
                                        } else {
                                            end = true;
                                            obj = null;
                                            dis.close();
                                        }

                                        return obj != null;
                                    }

                                    @SneakyThrows
                                    @Override
                                    public Object next() {
                                        var o = obj;
                                        obj = null;
                                        hasNext();
                                        return o;
                                    }
                                };

                                return Result.success( Stream.of( it ).onClose( Try.run( () -> {
                                    dis.close();
                                    successMetrics.increment();
                                } ) ) );
                            } else {
                                try {
                                    var ret = Result.<Object, Throwable>success( fst.readObjectWithSize( dis ) );
                                    successMetrics.increment();
                                    return ret;
                                } finally {
                                    dis.close();
                                }
                            }
                        }
                    } catch( Exception e ) {
                        dis.close();
                        throw e;
                    }
                } else throw new RemoteInvocationException( "invocation failed " + this + "#" + method.getName() + " code " + response.statusCode() );
            } catch( HttpTimeoutException | TimeoutException e ) {
                log.error( "timeout invoking {}#{}", method.getName(), this );
                timeoutMetrics.increment();
                lastException = e;
            } catch( Exception e ) {
                log.error( "error invoking {}#{}: {}", this, method.getName(), e.getMessage() );
                errorMetrics.increment();
                lastException = e;
            }
        }

        throw throwException( method.getName(), lastException );
    }

    @SneakyThrows
    private byte[] getInvocation( Method method, List<RemoteInvocation.Argument> arguments ) {
        var baos = new ByteArrayOutputStream();
        var dos = new DataOutputStream( baos );
        dos.writeInt( Remotes.VERSION );
        fst.writeObjectWithSize( dos, new RemoteInvocation( service, method.getName(), arguments ) );
        baos.close();

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return "remote:" + service + "(retry=" + retry + ")@" + uri;
    }
}
