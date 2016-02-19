/*
 * Copyright (c) Madberry Oy
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package oap.util;

import java.util.Random;

/**
 * @author togrul.meherremov
 */
public class RandomRunner {

    private static Random rnd = new Random();

    public static Result<Integer, Integer> rate( int succRate ) {
        if( succRate < 0 || succRate > 100 ) throw new IllegalArgumentException( "Success rate should be within 0..100" );
        int random = rnd.nextInt( 100 );
        return random < succRate ? Result.success( random ) : Result.failure( random );
    }

}
