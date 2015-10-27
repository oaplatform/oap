package oap.util;

import oap.testng.AbstractPerformance;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
@Test( enabled = false )
public class IDFAValidatorPerformance extends AbstractPerformance {
    @Test
    public void test() throws Exception {
        final int samples = 10000000;
        benchmark( "idfa-for", samples, 5, ( i ) ->
                assertTrue( IDFAValidator.isValid( "6369B91D-9A30-425C-B5AC-0E8DDF1D5A41" ) )
        );

        final Pattern pattern =
            Pattern.compile( "^\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}$" );
        benchmark( "idfa-regexp", samples, 5, ( i ) ->
                assertTrue( pattern.matcher( "6369B91D-9A30-425C-B5AC-0E8DDF1D5A41" ).find() )
        );
    }
}