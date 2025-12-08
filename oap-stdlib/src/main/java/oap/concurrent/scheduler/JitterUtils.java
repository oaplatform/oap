package oap.concurrent.scheduler;

import java.util.SplittableRandom;
import java.util.concurrent.locks.LockSupport;

public class JitterUtils {
    private static final SplittableRandom RANDOM = new SplittableRandom();

    public static void parkRandomNanos( long jitter ) {
        if( jitter > 0 ) {
            long nanos = RANDOM.nextLong( 0, jitter + 1 ) * 1_000_000L;
            LockSupport.parkNanos( nanos );
        }
    }
}
