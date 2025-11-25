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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.application.ServiceKernelCommand;
import oap.application.module.Reference;
import oap.util.Result;
import oap.util.Stream;
import oap.util.function.Try;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fory.exception.DeserializationException;
import org.apache.fory.io.ForyInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public final class RemoteInvocationHandler implements InvocationHandler {
    public static final ExecutorService NEW_SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final OkHttpClient globalClient;
    private static final SimpleTimeLimiter SIMPLE_TIME_LIMITER = SimpleTimeLimiter.create( NEW_SINGLE_THREAD_EXECUTOR );

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests( 1024 );
        dispatcher.setMaxRequestsPerHost( 1024 );
        builder.dispatcher( dispatcher );
        globalClient = builder.build();
    }

    private final Counter timeoutMetrics;
    private final Counter errorMetrics;
    private final Counter successMetrics;
    private final String source;
    private final URI uri;
    private final String service;
    private final long timeout;

    private RemoteInvocationHandler( String source,
                                     URI uri,
                                     String service,
                                     long timeout ) {
        this.source = source;
        this.uri = uri;
        this.service = service;
        this.timeout = timeout;

        Preconditions.checkNotNull( uri );
        Preconditions.checkNotNull( service );

        timeoutMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "timeout" ) );
        errorMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "error" ) );
        successMetrics = Metrics.counter( "remote_invocation", Tags.of( "service", service, "status", "success" ) );

        log.debug( "initialize {}", this );
    }

    public static Object proxy( String source, RemoteLocation remote, Class<?> clazz ) {
        return proxy( source, remote.url, remote.name, clazz, remote.timeout );
    }

    private static Object proxy( String source, URI uri, String service, Class<?> clazz, long timeout ) {
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[] { clazz },
            new RemoteInvocationHandler( source, uri, service, timeout ) );
    }

    @NotNull
    private static CompletionStage<Result<Object, Throwable>> retException( Throwable e, boolean async ) {
        if( async ) {
            return CompletableFuture.failedStage( e );
        } else {
            return CompletableFuture.completedStage( Result.failure( e ) );
        }
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if( uri == null ) {
            throw new RemoteInvocationException( "uri == null, source " + source + " -> service " + service + "#" + method.getName() );
        }

        if( method.getDeclaringClass() == Object.class ) {
            return method.invoke( this, args );
        }
        if( method.isDefault() ) {
            return MethodHandles.lookup()
                .unreflectSpecial( method, method.getDeclaringClass() )
                .bindTo( proxy )
                .invokeWithArguments( args );
        }

        Result<?, Throwable> result = invoke( method, args );
        if( result.isSuccess() ) {
            return result.successValue;
        } else {
            throw result.failureValue;
        }
    }

    private Result<?, Throwable> invoke( Method method, Object[] args ) {
        Parameter[] parameters = method.getParameters();
        List<RemoteInvocation.Argument> arguments = new ArrayList<>();

        for( int i = 0; i < parameters.length; i++ )
            arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
                parameters[i].getType(), args[i] ) );


        byte[] invocationB = getInvocation( method, arguments );

        boolean async = CompletableFuture.class.isAssignableFrom( method.getReturnType() );

        try {
            OkHttpClient client = globalClient.newBuilder().callTimeout( Duration.ofMillis( timeout ) ).build();
            Request request = new Request.Builder()
                .url( uri.toURL() )
                .post( RequestBody.create( invocationB ) )
                .build();
            Call call = client.newCall( request );

            CompletableFuture<Response> responseAsync = new CompletableFuture<>();

            if( async ) {
                call.enqueue( new Callback() {
                    @Override
                    public void onFailure( @NotNull Call call, @NotNull IOException e ) {
                        responseAsync.completeExceptionally( e );
                    }

                    @Override
                    public void onResponse( @NotNull Call call, @NotNull Response response ) {
                        responseAsync.complete( response );
                    }
                } );
            } else {
                try {
                    responseAsync.complete( call.execute() );
                } catch( IOException e ) {
                    responseAsync.completeExceptionally( e );
                }
            }

            CompletableFuture<Result<Object, Throwable>> ret = responseAsync.thenCompose( response -> {
                try {
                    if( response.code() == HTTP_OK ) {
                        InputStream inputStream = response.body().byteStream();
                        BufferedInputStream bis = new BufferedInputStream( inputStream );
                        ForyInputStream foryInputStream = new ForyInputStream( bis );
                        DataInputStream dis = new DataInputStream( foryInputStream );
                        boolean success = dis.readBoolean();

                        try {
                            if( !success ) {
                                try {
                                    Throwable throwable = ( Throwable ) ForyConsts.fory.deserialize( foryInputStream );

                                    if( throwable instanceof RemoteInvocationException riex ) {
                                        errorMetrics.increment();
                                        return retException( riex, async );
                                    }

                                    errorMetrics.increment();
                                    return async ? CompletableFuture.<Result<Object, Throwable>>failedStage( throwable ) : CompletableFuture.completedStage( Result.failure( throwable ) );
                                } finally {
                                    dis.close();
                                }
                            } else {
                                boolean stream = dis.readBoolean();
                                if( stream ) {
                                    ChainIterator it = new ChainIterator( foryInputStream );

                                    return CompletableFuture.completedStage( Result.success( Stream.of( it ).onClose( Try.run( () -> {
                                        dis.close();
                                        successMetrics.increment();
                                    } ) ) ) );
                                } else {
                                    try {
                                        Result<Object, Throwable> r = Result.success( ForyConsts.fory.deserialize( foryInputStream ) );
                                        successMetrics.increment();
                                        return CompletableFuture.completedStage( r );
                                    } finally {
                                        dis.close();
                                    }
                                }
                            }
                        } catch( Exception e ) {
                            dis.close();
                            return retException( e, async );
                        }
                    } else {
                        RemoteInvocationException ex = new RemoteInvocationException( "invocation failed " + this + "#" + service + "@" + method.getName()
                            + " code " + response.code()
                            + " body '" + response.body().string() + "'"
                            + " message '" + response.message() + "'" );

                        return retException( ex, async );
                    }
                } catch( Throwable e ) {
                    return retException( e, async );
                }
            } );

            ret.whenComplete( ( _, ex ) -> {
                if( ex != null ) {
                    checkException( ex );
                }
            } );

            if( async ) {
                return Result.success( ret.thenApply( r -> r.successValue ) );
            } else {
                return ret.get();
            }
        } catch( Exception e ) {
            if( async ) {
                return Result.success( CompletableFuture.failedFuture( e ) );
            }

            return Result.failure( e );
        }
    }

    private void checkException( Throwable ex ) {
        if( ex instanceof RemoteInvocationException riex ) {
            checkException( riex.getCause() );
            return;
        }

        if( ex instanceof HttpTimeoutException || ex instanceof TimeoutException ) {
            timeoutMetrics.increment();
        }
    }

    @SneakyThrows
    private byte[] getInvocation( Method method, List<RemoteInvocation.Argument> arguments ) {

        Reference reference = ServiceKernelCommand.INSTANCE.reference( service, null );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream( baos );
        dos.writeInt( RemoteInvocation.VERSION );
        ForyConsts.fory.serialize( dos, new RemoteInvocation( reference.toString(), method.getName(), arguments ) );
        baos.close();

        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return "source:" + source + " -> remote:" + service + "@" + uri;
    }

    private class ChainIterator implements Iterator<Object> {
        private final ForyInputStream dis;
        private Object obj;
        private boolean end;

        ChainIterator( ForyInputStream dis ) {
            this.dis = dis;
            obj = null;
            end = false;
        }

        @SneakyThrows
        @Override
        public boolean hasNext() {
            if( end ) return false;

            if( obj != null ) return true;

            try {
                obj = ForyConsts.fory.deserialize( dis );
            } catch( RuntimeException e ) {
                if( e.getCause() instanceof DeserializationException de && de.getCause() instanceof IndexOutOfBoundsException iobe && iobe.getMessage().startsWith( "No enough data in the stream" ) ) {
                    end = true;
                    obj = null;
                    dis.close();
                } else {
                    throw e;
                }
            }

            return obj != null;
        }

        @SneakyThrows
        @Override
        public Object next() {
            Object o = obj;
            obj = null;
            hasNext();
            return o;
        }
    }
}
