package oap.http.pniov3;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.undertow.util.StatusCodes;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static oap.http.pniov3.PnioExchange.ProcessState.CONNECTION_CLOSED;
import static oap.http.pniov3.PnioExchange.ProcessState.DONE;
import static oap.http.pniov3.PnioExchange.ProcessState.EXCEPTION;
import static oap.http.pniov3.PnioExchange.ProcessState.INTERRUPTED;
import static oap.http.pniov3.PnioExchange.ProcessState.REJECTED;
import static oap.http.pniov3.PnioExchange.ProcessState.REQUEST_BUFFER_OVERFLOW;
import static oap.http.pniov3.PnioExchange.ProcessState.RESPONSE_BUFFER_OVERFLOW;
import static oap.http.pniov3.PnioExchange.ProcessState.RUNNING;
import static oap.http.pniov3.PnioExchange.ProcessState.TIMEOUT;

public class PnioExchange<RequestState> {
    private static final AtomicLong idGenerator = new AtomicLong();
    private static final VarHandle PROCESS_STATE_HANDLE;

    private static ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();


    static {
        try {
            PROCESS_STATE_HANDLE = MethodHandles.lookup().findVarHandle( PnioExchange.class, "processState", int.class );
        } catch( NoSuchFieldException | IllegalAccessException e ) {
            throw new Error( e );
        }
    }

    public final long id = idGenerator.incrementAndGet();

    public final long startTimeNano;
    public final byte[] requestBuffer;
    public final HttpResponse httpResponse;
    public final PnioController controller;
    public final PnioListener<RequestState> pnioListener;
    public final ComputeTask<RequestState> task;
    public final String handlerName;
    public final RequestState requestState;
    protected final HttpServerExchange oapExchange;
    private final PnioMetrics metrics;
    public long timeoutNano;
    public volatile Throwable throwable;
    public volatile int processState;
    private volatile Runnable onDoneRunnable;

    public PnioExchange( String handlerName,
                         byte[] requestBuffer, int responseSize,
                         PnioController controller,
                         ComputeTask<RequestState> task,
                         HttpServerExchange oapExchange, long timeout,
                         PnioListener<RequestState> pnioListener,
                         RequestState requestState,
                         PnioMetrics metrics ) {
        this.handlerName = handlerName;
        this.requestState = requestState;
        this.metrics = metrics;
        this.startTimeNano = System.nanoTime();
        this.requestBuffer = requestBuffer;
        httpResponse = new HttpResponse( new PnioResponseBuffer( responseSize ) );

        this.controller = controller;

        this.task = task;

        this.oapExchange = oapExchange;
        this.timeoutNano = timeout * 1_000_000;

        Preconditions.checkArgument( timeoutNano > 0, "timeoutNano must be greater than 0" );

        this.pnioListener = pnioListener;

        metrics.activeRequests.incrementAndGet();
    }

    public void completeWithFail( Throwable throwable ) {
        if( throwable instanceof TimeoutException ) {
            completeWithTimeout();
        } else if( throwable instanceof InterruptedException ) {
            completeWithInterrupted();
        } else {
            this.throwable = throwable;
            PROCESS_STATE_HANDLE.getAndBitwiseOr( this, EXCEPTION );
        }
    }

    public void completeWithInterrupted() {
        PROCESS_STATE_HANDLE.getAndBitwiseOr( this, INTERRUPTED );
    }

    public void completeWithTimeout() {
        PROCESS_STATE_HANDLE.getAndBitwiseOr( this, TIMEOUT );
    }

    public void complete( Consumer<HttpResponse> response ) {
        response.accept( httpResponse );

        complete();
        response();
    }

    public void complete() {
        PROCESS_STATE_HANDLE.getAndBitwiseOr( this, DONE );
    }

    public void completeWithRejected() {
        PROCESS_STATE_HANDLE.getAndBitwiseOr( this, REJECTED );
    }

    public void completeWithBufferOverflow( boolean request ) {
        PROCESS_STATE_HANDLE.getAndBitwiseOr( this, request ? ProcessState.REQUEST_BUFFER_OVERFLOW : ProcessState.RESPONSE_BUFFER_OVERFLOW );
    }

    public final boolean isDone() {
        return processState > 0;
    }

    public void send() {
        oapExchange.setStatusCode( httpResponse.status );

        httpResponse.headers.forEach( oapExchange::setResponseHeader );
        httpResponse.cookies.forEach( oapExchange::setResponseCookie );

        String contentType = httpResponse.contentType;
        if( contentType != null ) {
            oapExchange.setResponseHeader( Http.Headers.CONTENT_TYPE, contentType );
        }

        PnioResponseBuffer responseBuffer = httpResponse.responseBuffer;
        if( !responseBuffer.isEmpty() ) {
            oapExchange.send( responseBuffer.buffer, 0, responseBuffer.length );
        } else {
            oapExchange.endExchange();
        }
    }

    public void response() {
        try {
            if( ( processState & CONNECTION_CLOSED ) > 0 ) {
                oapExchange.closeConnection();
            } else if( ( processState & EXCEPTION ) > 0 ) {
                metrics.exception.increment();
                pnioListener.onException( this );
            } else if( ( processState & REQUEST_BUFFER_OVERFLOW ) > 0 ) {
                metrics.requestBufferOverflow.increment();
                pnioListener.onRequestBufferOverflow( this );
            } else if( ( processState & RESPONSE_BUFFER_OVERFLOW ) > 0 ) {
                metrics.responseBufferOverflow.increment();
                pnioListener.onResponseBufferOverflow( this );
            } else if( ( processState & TIMEOUT ) > 0 ) {
                metrics.timeout.increment();
                pnioListener.onTimeout( this );
            } else if( ( processState & REJECTED ) > 0 ) {
                metrics.rejected.increment();
                pnioListener.onRejected( this );
            } else if( ( processState & DONE ) > 0 ) {
                metrics.completed.increment();
                pnioListener.onDone( this );
            } else {
                metrics.unknown.increment();
                pnioListener.onUnknown( this );
            }
        } finally {
            if( onDoneRunnable != null ) {
                onDoneRunnable.run();
            }
        }
    }

    public void onDone( Runnable onDoneRunnable ) {
        Preconditions.checkArgument( this.onDoneRunnable == null );

        this.onDoneRunnable = onDoneRunnable;
    }

    public boolean gzipSupported() {
        return oapExchange.gzipSupported();
    }

    public String printState() {
        ArrayList<String> state = new ArrayList<>();

        if( ( processState & DONE ) > 0 ) {
            state.add( "DONE" );
        }
        if( ( processState & TIMEOUT ) > 0 ) {
            state.add( "TIMEOUT" );
        }
        if( ( processState & INTERRUPTED ) > 0 ) {
            state.add( "INTERRUPTED" );
        }
        if( ( processState & EXCEPTION ) > 0 ) {
            state.add( "EXCEPTION" );
        }
        if( ( processState & CONNECTION_CLOSED ) > 0 ) {
            state.add( "CONNECTION_CLOSED" );
        }
        if( ( processState & REJECTED ) > 0 ) {
            state.add( "REJECTED" );
        }
        if( ( processState & REQUEST_BUFFER_OVERFLOW ) > 0 ) {
            state.add( "REQUEST_BUFFER_OVERFLOW" );
        }
        if( ( processState & RESPONSE_BUFFER_OVERFLOW ) > 0 ) {
            state.add( "RESPONSE_BUFFER_OVERFLOW" );
        }
        if( processState == 0 ) {
            state.add( "RUNNING" );
        }

        return String.join( ", ", state );
    }

    /**
     * @param asyncTaskType - asynchronous task type name for monitoring ( pnio_async{type = asyncTaskType} )
     * @param asyncTask
     * @param <T>
     * @return
     */
    public <T> T runAsyncTask( String asyncTaskType, AsyncTask<T, RequestState> asyncTask ) {
        long start = System.nanoTime();
        try {
            T result = asyncTask
                .apply( this )
                .orTimeout( getTimeLeftNano(), TimeUnit.NANOSECONDS )
                .join();

            if( processState == RUNNING ) {
                if( isTimeout() ) {
                    completeWithTimeout();
                    throw new PnioForceTerminateException( asyncTaskType );
                }
            } else {
                throw new PnioForceTerminateException( asyncTaskType );
            }

            return result;
        } catch( PnioForceTerminateException e ) {
            throw e;
        } catch( BufferOverflowException e ) {
            completeWithBufferOverflow( false );
            throw new PnioForceTerminateException( asyncTaskType );
        } catch( CompletionException e ) {
            if( e.getCause() instanceof TimeoutException ) {
                completeWithTimeout();
            } else {
                completeWithFail( e.getCause() );
            }
            throw new PnioForceTerminateException( asyncTaskType );
        } catch( Throwable e ) {
            completeWithFail( e );
            throw new PnioForceTerminateException( asyncTaskType );
        } finally {
            long end = System.nanoTime();

            timers
                .computeIfAbsent( asyncTaskType, _ -> Timer
                    .builder( "pnio_async" )
                    .publishPercentileHistogram()
                    .tags( "type", asyncTaskType, "id", handlerName )
                    .register( Metrics.globalRegistry ) )
                .record( end - start, TimeUnit.NANOSECONDS );
        }
    }

    public boolean isTimeout() {
        return getTimeLeftNano() < 1;
    }

    public long getTimeLeftNano() {
        long now = System.nanoTime();
        long durationInNano = now - getRequestStartTime();

        return timeoutNano - durationInNano;
    }

    public final long getRequestStartTime() {
        return oapExchange.exchange.getRequestStartTime();
    }

    public final boolean isRequestGzipped() {
        return oapExchange.isRequestGzipped();
    }

    public final String getRequestURI() {
        return oapExchange.getRequestURI();
    }

    public final String getStringParameter( String name ) {
        return oapExchange.getStringParameter( name );
    }

    public final boolean getBooleanParameter( String name ) {
        return oapExchange.getBooleanParameter( name );
    }

    public final String getRequestCookieValue( String name ) {
        return oapExchange.getRequestCookieValue( name );
    }

    public final String getRequestHeader( String name ) {
        return oapExchange.getRequestHeader( name );
    }

    public final String ip() {
        return oapExchange.ip();
    }

    public final String ua() {
        return oapExchange.ua();
    }

    public final String referrer() {
        return oapExchange.referrer();
    }

    public final Deque<String> getQueryParameter( String name ) {
        return oapExchange.exchange.getQueryParameters().get( name );
    }

    public String getQueryString() {
        return oapExchange.exchange.getQueryString();
    }

    public String getFullRequestURL() {
        return ( !oapExchange.exchange.isHostIncludedInRequestURI() ? oapExchange.exchange.getRequestScheme() + "://" + oapExchange.exchange.getHostAndPort() : "" )
            + oapExchange.getRequestURI() + "?" + oapExchange.exchange.getQueryString();
    }

    public HttpServerExchange.HttpMethod getRequestMethod() {
        return oapExchange.getRequestMethod();
    }

    public Map<String, Deque<String>> getQueryParameters() {
        return oapExchange.exchange.getQueryParameters();
    }

    public String header( String headerName, String defaultValue ) {
        return oapExchange.header( headerName, defaultValue );
    }

    public void updateTimeoutNano( long timeoutNano ) {
        this.timeoutNano = timeoutNano;
    }

    public void updateTimeoutMs( long timeoutMs ) {
        this.timeoutNano = timeoutMs * 1_000_000;
    }

    @SuppressWarnings( "checkstyle:InterfaceIsType" )
    public interface ProcessState {
        int RUNNING = 0;
        int DONE = 1 << 1;
        int TIMEOUT = 1 << 2;
        int INTERRUPTED = 1 << 3;
        int EXCEPTION = 1 << 4;
        int CONNECTION_CLOSED = 1 << 5;
        int REJECTED = 1 << 6;
        int REQUEST_BUFFER_OVERFLOW = 1 << 7;
        int RESPONSE_BUFFER_OVERFLOW = 1 << 8;
    }

    public static class HttpResponse {
        public final HashMap<String, String> headers = new HashMap<>();
        public final ArrayList<Cookie> cookies = new ArrayList<>();
        public final PnioResponseBuffer responseBuffer;
        public int status = StatusCodes.NO_CONTENT;
        public String contentType;

        public HttpResponse( PnioResponseBuffer responseBuffer ) {
            this.responseBuffer = responseBuffer;
        }

        public HttpResponse redirect( String location ) {
            status = Http.StatusCode.FOUND;
            headers.put( Http.Headers.LOCATION, location );

            return this;
        }

        public HttpResponse addResponseCookie( oap.http.Cookie cookie ) {
            this.cookies.add( cookie );

            return this;
        }

        public HttpResponse addResponseHeader( String name, String value ) {
            this.headers.put( name, value );

            return this;
        }

        public HttpResponse responseNoContent() {
            status = Http.StatusCode.NO_CONTENT;

            return this;
        }

        public HttpResponse responseNotFound() {
            status = Http.StatusCode.NOT_FOUND;

            return this;
        }

        public HttpResponse setBody( String data ) {
            responseBuffer.setAndResize( data );

            return this;
        }
    }
}
