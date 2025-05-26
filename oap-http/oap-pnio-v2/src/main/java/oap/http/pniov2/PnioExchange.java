package oap.http.pniov2;

import com.google.common.base.Preconditions;
import io.undertow.util.StatusCodes;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static oap.http.pniov2.PnioExchange.ProcessState.CONNECTION_CLOSED;
import static oap.http.pniov2.PnioExchange.ProcessState.DONE;
import static oap.http.pniov2.PnioExchange.ProcessState.EXCEPTION;
import static oap.http.pniov2.PnioExchange.ProcessState.INTERRUPTED;
import static oap.http.pniov2.PnioExchange.ProcessState.REJECTED;
import static oap.http.pniov2.PnioExchange.ProcessState.REQUEST_BUFFER_OVERFLOW;
import static oap.http.pniov2.PnioExchange.ProcessState.RESPONSE_BUFFER_OVERFLOW;
import static oap.http.pniov2.PnioExchange.ProcessState.TIMEOUT;

public class PnioExchange<RequestState> {
    private static final AtomicLong idGenerator = new AtomicLong();
    private static final VarHandle PROCESS_STATE_HANDLE;

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
    public final long timeoutNano;
    public final HttpResponse httpResponse;
    public final PnioController controller;
    public final PnioListener<RequestState> pnioListener;
    public final ComputeTask<RequestState> task;
    public final RequestState requestState;
    protected final HttpServerExchange oapExchange;
    public volatile Throwable throwable;
    public volatile int processState;
    private volatile Runnable onDoneRunnable;

    public PnioExchange( byte[] requestBuffer, int responseSize, PnioController controller,
                         ComputeTask<RequestState> task,
                         HttpServerExchange oapExchange, long timeout,
                         PnioListener<RequestState> pnioListener,
                         RequestState requestState ) {
        this.requestState = requestState;
        this.startTimeNano = System.nanoTime();
        this.requestBuffer = requestBuffer;
        httpResponse = new HttpResponse( new PnioResponseBuffer( responseSize ) );

        this.controller = controller;

        this.task = task;

        this.oapExchange = oapExchange;
        this.timeoutNano = timeout * 1_000_000;

        Preconditions.checkArgument( timeoutNano > 0, "timeoutNano must be greater than 0" );

        this.pnioListener = pnioListener;

        PnioMetrics.activeRequests.incrementAndGet();
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
                PnioMetrics.EXCEPTION.increment();
                pnioListener.onException( this );
            } else if( ( processState & REQUEST_BUFFER_OVERFLOW ) > 0 ) {
                PnioMetrics.REQUEST_BUFFER_OVERFLOW.increment();
                pnioListener.onRequestBufferOverflow( this );
            } else if( ( processState & RESPONSE_BUFFER_OVERFLOW ) > 0 ) {
                PnioMetrics.RESPONSE_BUFFER_OVERFLOW.increment();
                pnioListener.onResponseBufferOverflow( this );
            } else if( ( processState & TIMEOUT ) > 0 ) {
                PnioMetrics.TIMEOUT.increment();
                pnioListener.onTimeout( this );
            } else if( ( processState & REJECTED ) > 0 ) {
                PnioMetrics.REJECTED.increment();
                pnioListener.onRejected( this );
            } else if( ( processState & DONE ) > 0 ) {
                PnioMetrics.COMPLETED.increment();
                pnioListener.onDone( this );
            } else {
                PnioMetrics.UNKNOWN.increment();
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

    @SuppressWarnings( "unchecked" )
    public <T> T runAsyncTask( AsyncTask<T, RequestState> asyncTask ) {
        PnioAsyncTask<T, RequestState> pnioAsyncTask = new PnioAsyncTask<>( asyncTask, this );
        pnioAsyncTask.fork();
        return pnioAsyncTask.join();
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

        public void redirect( String location ) {
            status = Http.StatusCode.FOUND;
            headers.put( Http.Headers.LOCATION, location );
        }

        public void responseNoContent() {
            status = Http.StatusCode.NO_CONTENT;
        }

        public void responseNotFound() {
            status = Http.StatusCode.NOT_FOUND;
        }

    }
}
