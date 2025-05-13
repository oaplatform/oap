package oap.http.pniov2;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.atomic.AtomicInteger;

public class PnioMetrics {
    public static final Counter REQUESTS = Metrics.counter( "pnio_requests", Tags.of( "type", "requests" ) );
    public static final Counter REJECTED = Metrics.counter( "pnio_requests", Tags.of( "type", "rejected" ) );
    public static final Counter TIMEOUT = Metrics.counter( "pnio_requests", Tags.of( "type", "timeout" ) );
    public static final Counter REQUEST_BUFFER_OVERFLOW = Metrics.counter( "pnio_requests", Tags.of( "type", "request_buffer_overflow" ) );
    public static final Counter RESPONSE_BUFFER_OVERFLOW = Metrics.counter( "pnio_requests", Tags.of( "type", "response_buffer_overflow" ) );
    public static final Counter EXCEPTION = Metrics.counter( "pnio_requests", Tags.of( "type", "exception" ) );
    public static final Counter COMPLETED = Metrics.counter( "pnio_requests", Tags.of( "type", "completed" ) );
    public static final Counter UNKNOWN = Metrics.counter( "pnio_requests", Tags.of( "type", "unknown" ) );
    public static final AtomicInteger activeRequests = new AtomicInteger();

    static {
        Metrics.gauge( "pnio_requests_active", activeRequests, AtomicInteger::get );
    }
}
