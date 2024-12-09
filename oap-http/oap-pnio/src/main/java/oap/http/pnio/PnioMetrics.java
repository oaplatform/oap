package oap.http.pnio;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

import java.util.concurrent.atomic.AtomicInteger;

public class PnioMetrics {
    public static final Counter REQUESTS = Metrics.counter( "pnio_requests" );
    public static final AtomicInteger activeRequests = new AtomicInteger();

    static {
        Metrics.gauge( "pnio_requests_active", activeRequests, AtomicInteger::get );
    }
}
