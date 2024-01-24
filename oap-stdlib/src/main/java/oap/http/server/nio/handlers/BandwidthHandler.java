package oap.http.server.nio.handlers;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import oap.http.server.nio.AbstractNioHandler;
import oap.util.Lists;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BandwidthHandler extends AbstractNioHandler {
    private static Bandwidth defaultLimit;

    static {
        defaultLimit = Bandwidth.simple( 1_000_000L, Duration.ofSeconds( 1 ) );
    }

    public final List<Bandwidth> bandwidths = Lists.of( defaultLimit );

    private Bucket bucket;

    public void start() {
        LocalBucketBuilder builder = Bucket.builder();
        bandwidths.forEach( builder::addLimit );
        bucket = builder.build();
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining( 1 );
        if( probe == null || probe.isConsumed() ) {
            // allowed
            httpHandler.handleRequest( exchange );
        } else {
            exchange.setStatusCode( StatusCodes.TOO_MANY_REQUESTS );
            long nanosToRetry = TimeUnit.NANOSECONDS.toSeconds( probe.getNanosToWaitForRefill() );
            exchange.getResponseHeaders().add( HttpString.tryFromString( "X-Rate-Limit-Retry-After-Seconds" ), nanosToRetry );
            exchange.setReasonPhrase( "Too many requests" );
        }
    }
}
