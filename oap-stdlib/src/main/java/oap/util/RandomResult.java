package oap.util;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * @author togrul.meherremov
 */
public final class RandomResult {

    /**
     * Thread-specific random number generators. Each is seeded with the thread
     * ID, so the sequence of pseudo-random numbers are unique between threads.
     */
    private static final ThreadLocal<RandomGenerator> rnd = ThreadLocal.withInitial(
        () -> RandomGeneratorFactory.of( "L128X1024MixRandom" ).create( System.currentTimeMillis() * Thread.currentThread().getId() )
    );

    private RandomResult() {
    }

    public static Result<Integer, Integer> rate( int succRate ) {
        if( succRate < 0 || succRate > 100 )
            throw new IllegalArgumentException( "Success rate should be within 0..100" );
        int random = rnd.get().nextInt( 100 );
        return random < succRate ? Result.success( random ) : Result.failure( random );
    }

    //longest series I've ever seen for
    // 200 attempts: same in raw series:17, usual >= 8
    // 2 mln attempts: same in raw series:26, usual >= 19
    public static void main( String[] args ) {
        int longestSeriesTrue = 0;
        int longestSeriesFalse = 0;
        int seriesTrue = 0;
        int seriesFalse = 0;
        Boolean previousValue = null;
        for ( int i = 0; i < 2000000; i++ ) {
            if ( longestSeriesFalse < seriesFalse ) longestSeriesFalse = seriesFalse;
            if ( longestSeriesTrue < seriesTrue ) longestSeriesTrue = seriesTrue;
            Result<Integer, Integer> rate = RandomResult.rate( 50 );
            if ( previousValue == null ) {
                previousValue = rate.isSuccess();
                if ( rate.isSuccess() ) seriesTrue++;
                else seriesFalse++;
                continue;
            }

            if ( previousValue && rate.isSuccess() ) {
                seriesTrue++;
                continue;
            }
            if ( !previousValue && !rate.isSuccess() ) {
                seriesFalse++;
                continue;
            }
            previousValue = rate.isSuccess();
            seriesFalse = rate.isSuccess() ? 0 : 1;
            seriesTrue = rate.isSuccess() ? 1 : 0;
        }
        System.err.println( "false series:" + longestSeriesFalse );
        System.err.println( "true series:" + longestSeriesTrue );
    }
}
