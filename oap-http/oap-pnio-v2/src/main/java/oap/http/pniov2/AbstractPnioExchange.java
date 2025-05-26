package oap.http.pniov2;

import com.google.common.base.Preconditions;
import io.undertow.util.StatusCodes;
import oap.http.Cookie;
import oap.http.Http;
import oap.http.server.nio.HttpServerExchange;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static oap.http.pniov2.AbstractPnioExchange.ProcessState.DONE;
import static oap.http.pniov2.AbstractPnioExchange.ProcessState.EXCEPTION;
import static oap.http.pniov2.AbstractPnioExchange.ProcessState.INTERRUPTED;
import static oap.http.pniov2.AbstractPnioExchange.ProcessState.REJECTED;
import static oap.http.pniov2.AbstractPnioExchange.ProcessState.TIMEOUT;

public abstract class AbstractPnioExchange<E extends AbstractPnioExchange<E>> {
    private static final AtomicLong idGenerator = new AtomicLong();

    public final long id = idGenerator.incrementAndGet();

    public final long startTimeNano;
    public final byte[] requestBuffer;
    public final long timeoutNano;
    public final HttpResponse httpResponse;
    public final PnioController controller;
    public final PnioListener pnioListener;
    public final ComputeTask<E> task;
    protected final HttpServerExchange oapExchange;
    public volatile Throwable throwable;
    public volatile ProcessState processState;
    private volatile Runnable onDoneRunnable;

    public AbstractPnioExchange( byte[] requestBuffer, int responseSize, PnioController controller,
                                 ComputeTask<E> task,
                                 HttpServerExchange oapExchange, long timeout,
                                 PnioListener pnioListener ) {
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
            this.processState = EXCEPTION;
        }
    }

    public void completeWithInterrupted() {
        this.processState = INTERRUPTED;
    }

    public void completeWithTimeout() {
        this.processState = TIMEOUT;
    }

    public void complete() {
        this.processState = DONE;
    }

    public void completeWithRejected() {
        this.processState = REJECTED;
    }

    public void completeWithBufferOverflow( boolean request ) {
        this.processState = request ? ProcessState.REQUEST_BUFFER_OVERFLOW : ProcessState.RESPONSE_BUFFER_OVERFLOW;
    }

    public final boolean isDone() {
        return processState == DONE;
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
            switch( processState ) {
                case CONNECTION_CLOSED -> oapExchange.closeConnection();
                case EXCEPTION -> {
                    PnioMetrics.EXCEPTION.increment();
                    pnioListener.onException( this );
                }
                case REQUEST_BUFFER_OVERFLOW -> {
                    PnioMetrics.REQUEST_BUFFER_OVERFLOW.increment();
                    pnioListener.onRequestBufferOverflow( this );
                }
                case RESPONSE_BUFFER_OVERFLOW -> {
                    PnioMetrics.RESPONSE_BUFFER_OVERFLOW.increment();
                    pnioListener.onResponseBufferOverflow( this );
                }
                case TIMEOUT -> {
                    PnioMetrics.TIMEOUT.increment();
                    pnioListener.onTimeout( this );
                }
                case REJECTED -> {
                    PnioMetrics.REJECTED.increment();
                    pnioListener.onRejected( this );
                }
                case DONE -> {
                    PnioMetrics.COMPLETED.increment();
                    pnioListener.onDone( this );
                }
                default -> {
                    PnioMetrics.UNKNOWN.increment();
                    pnioListener.onUnknown( this );
                }
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

    @SuppressWarnings( "unchecked" )
    public <T> T runAsyncTask( AsyncTask<T, E> asyncTask ) {
        PnioAsyncTask<T, E> pnioAsyncTask = new PnioAsyncTask<>( asyncTask, ( E ) this );
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

    public final String getStringParameter( String name ) {
        return oapExchange.getStringParameter( name );
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

    public enum ProcessState {
        RUNNING,
        DONE,
        TIMEOUT,
        INTERRUPTED,
        EXCEPTION,
        CONNECTION_CLOSED,
        REJECTED,
        REQUEST_BUFFER_OVERFLOW,
        RESPONSE_BUFFER_OVERFLOW
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
