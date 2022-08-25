package oap.util;

import java.util.Random;

/**
 * @author togrul.meherremov
 */
public final class RandomResult {

    /**
     * Thread-specific random number generators. Each is seeded with the thread
     * ID, so the sequence of pseudo-random numbers are unique between threads.
     */
    private static ThreadLocal<Random> rnd = ThreadLocal.withInitial(
        () -> new Random( System.currentTimeMillis() * Thread.currentThread().getId() )
    );

    private RandomResult() {
    }

    public static Result<Integer, Integer> rate( int succRate ) {
        if( succRate < 0 || succRate > 100 )
            throw new IllegalArgumentException( "Success rate should be within 0..100" );
        int random = rnd.get().nextInt( 100 );
        return random < succRate ? Result.success( random ) : Result.failure( random );
    }

}
