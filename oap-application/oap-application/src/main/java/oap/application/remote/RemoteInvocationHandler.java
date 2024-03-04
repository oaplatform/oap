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
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.http.Client;
import oap.util.Result;
import oap.util.Stream;
import oap.util.function.Try;
import org.slf4j.event.Level;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.util.Dates.s;

@Slf4j
public final class RemoteInvocationHandler implements InvocationHandler {
    public static final ExecutorService NEW_SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Client client;
    private static final SimpleTimeLimiter SIMPLE_TIME_LIMITER = SimpleTimeLimiter.create( NEW_SINGLE_THREAD_EXECUTOR );

    static {
        client = Client
            .custom()
            .setMaxConnPerRoute( 1024 )
            .setMaxConnTotal( 1024 )
            .build();
    }

    private final Counter timeoutMetrics;
    private final Counter errorMetrics;
    private final Counter successMetrics;
    private final String source;
    private final URI uri;
    private final FST fst;
    private final int retry;
    private final String service;
    private final long timeout;

    private RemoteInvocationHandler( String source,
                                     URI uri,
                                     String service,
                                     long timeout,
                                     FST.SerializationMethod serialization,
                                     int retry ) {
        this.source = source;
        this.uri = uri;
        this.service = service;
        this.timeout = timeout;
        this.fst = new FST( serialization );
        this.retry = retry;

        Preconditions.checkNotNull( uri );
        Preconditions.checkNotNull( service );

        timeoutMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "timeout" ) );
        errorMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "error" ) );
        successMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "success" ) );

        log.debug( "initialize {}", this );
    }

    public static Object proxy( String source, RemoteLocation remote, Class<?> clazz ) {
        return proxy( source, remote.url, remote.name, clazz, remote.timeout, remote.serialization, remote.retry );
    }

    private static Object proxy( String source, URI uri, String service, Class<?> clazz,
                                 long timeout, FST.SerializationMethod serialization, int retry ) {
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[] { clazz },
            new RemoteInvocationHandler( source, uri, service, timeout, serialization, retry ) );
    }

    private RemoteInvocationException throwException( String methodName, Throwable throwable ) {
        if( throwable instanceof RemoteInvocationException riex ) return riex;
        else if( throwable instanceof ExecutionException ) return throwException( methodName, throwable.getCause() );
        else
            return new RemoteInvocationException( "invocation failed " + this + "#" + service + "@" + methodName, throwable );
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if( uri == null )
            throw new RemoteInvocationException( "uri == null, source " + source + " -> service " + service + "#" + method.getName() );

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
                Result<Client.Response, Throwable> responseResult = client.post( uri.toString(), invocationB, timeout );
                if( responseResult.isSuccess() ) {
                    Client.Response response = responseResult.successValue;

                    if( response.code == HTTP_OK ) {
                        var inputStream = response.getInputStream();
                        var bis = new BufferedInputStream( inputStream );
                        var dis = new DataInputStream( bis );
                        var success = SIMPLE_TIME_LIMITER.callUninterruptiblyWithTimeout( dis::readBoolean, timeout, MILLISECONDS );

                        try {
                            if( Boolean.FALSE.equals( success ) ) {
                                try {
                                    var throwable = SIMPLE_TIME_LIMITER.callUninterruptiblyWithTimeout(
                                        () -> ( Throwable ) fst.readObjectWithSize( dis ), timeout, MILLISECONDS );

                                    if( throwable instanceof RemoteInvocationException riex ) {
                                        errorMetrics.increment();
                                        throw riex;
                                    }

                                    var failure = Result.failure( throwable );
                                    errorMetrics.increment();
                                    return failure;
                                } finally {
                                    dis.close();
                                }
                            } else {
                                var stream = SIMPLE_TIME_LIMITER.callUninterruptiblyWithTimeout( dis::readBoolean, timeout, MILLISECONDS );
                                if( Boolean.TRUE.equals( stream ) ) {
                                    var it = new ChainIterator( dis );

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
                    } else
                        throw new RemoteInvocationException( "invocation failed " + this + "#" + service + "@" + method.getName()
                            + " code " + response.code
                            + " body '" + response.contentString() + "'"
                            + " message '" + response.reasonPhrase + "'" );
                } else {
                    throw responseResult.failureValue;
                }
            } catch( HttpTimeoutException | TimeoutException | UncheckedTimeoutException e ) {
                LogConsolidated.log( log, Level.WARN, s( 5 ), "timeout invoking " + method.getName() + "#" + this, null );
                timeoutMetrics.increment();
                lastException = e;
            } catch( Throwable e ) {
                LogConsolidated.log( log, Level.WARN, s( 5 ), "error invoking " + this + "#" + method.getName() + ": " + e.getMessage(), null );
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
        dos.writeInt( RemoteInvocation.VERSION );
        fst.writeObjectWithSize( dos, new RemoteInvocation( service, method.getName(), arguments ) );
        baos.close();

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return "source:" + source + " -> remote:" + service + "(retry=" + retry + ")@" + uri;
    }

    private class ChainIterator implements Iterator<Object> {
        private final DataInputStream dis;
        private Object obj;
        private boolean end;

        ChainIterator( DataInputStream dis ) {
            this.dis = dis;
            obj = null;
            end = false;
        }

        @SneakyThrows
        @Override
        public boolean hasNext() {
            if( end ) return false;

            if( obj != null ) return true;

            SIMPLE_TIME_LIMITER.runWithTimeout( () -> {
                try {
                    var next = dis.readInt();
                    if( next > 0 ) {
                        obj = fst.readObject( dis, next );
                    } else {
                        end = true;
                        obj = null;
                        dis.close();
                    }
                } catch( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            }, timeout, MILLISECONDS );

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
    }
}
