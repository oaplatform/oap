package oap.http.pniov2;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class PnioMetrics {
    public final Counter requests;
    public final Counter rejected;
    public final Counter timeout;
    public final Counter requestBufferOverflow;
    public final Counter responseBufferOverflow;
    public final Counter exception;
    public final Counter completed;
    public final Counter unknown;
    public final AtomicInteger activeRequests = new AtomicInteger();

    private final String id;

    public PnioMetrics( String id ) {
        this.id = id;

        requests = Metrics.counter( "pnio_requests", Tags.of( "type", "requests", "id", id ) );
        rejected = Metrics.counter( "pnio_requests", Tags.of( "type", "rejected", "id", id ) );
        timeout = Metrics.counter( "pnio_requests", Tags.of( "type", "timeout", "id", id ) );
        requestBufferOverflow = Metrics.counter( "pnio_requests", Tags.of( "type", "request_buffer_overflow", "id", id ) );
        responseBufferOverflow = Metrics.counter( "pnio_requests", Tags.of( "type", "response_buffer_overflow", "id", id ) );
        exception = Metrics.counter( "pnio_requests", Tags.of( "type", "exception", "id", id ) );
        completed = Metrics.counter( "pnio_requests", Tags.of( "type", "completed", "id", id ) );
        unknown = Metrics.counter( "pnio_requests", Tags.of( "type", "unknown", "id", id ) );

        Metrics.gauge( "pnio_requests_active", Tags.of( "id", id ), activeRequests, AtomicInteger::get );
    }
}
