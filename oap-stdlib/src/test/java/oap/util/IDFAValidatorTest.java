package oap.util;

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class IDFAValidatorTest extends AbstractTest {

    @Test
    public void testIsValid() throws Exception {
        assertTrue( IDFAValidator.isValid( "6369B91D-9A30-425C-B5AC-0E8DDF1D5A41" ) );
        assertTrue( IDFAValidator.isValid( "6369b91d-9a30-425c-b5ac-0e8ddf1d5a41" ) );

        assertFalse( IDFAValidator.isValid( "369B91D-9A30-425C-B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D+9A30-425C-B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A3-425C-B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30+425C-B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425-B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425C+B5AC-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425C-B5A-0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425C-B5AC+0E8DDF1D5A41" ) );
        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425C-B5AC-0E8DDF1D5A4Z" ) );

        assertFalse( IDFAValidator.isValid( "6369B91D-9A30-425C-B5AC-0E8DDF1D5A4" ) );

        assertFalse( IDFAValidator.isValid( "6369B91D" ) );
    }
}