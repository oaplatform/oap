package oap.util;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public final class IDFAValidator {
    public static String filter( String idfa ) {
        return isValid( idfa ) ? idfa : null;
    }

    public static boolean isValid( String idfa ) {
        if( idfa == null ) return true;
        if( idfa.length() != 36 ) return false;

        return check( idfa, 0, 8 ) && idfa.charAt( 8 ) == '-' &&
            check( idfa, 9, 4 ) && idfa.charAt( 13 ) == '-' &&
            check( idfa, 14, 4 ) && idfa.charAt( 18 ) == '-' &&
            check( idfa, 19, 4 ) && idfa.charAt( 23 ) == '-' &&
            check( idfa, 24, 12 );
    }

    private static boolean check( String idfa, int start, int length ) {
        final int end = start + length;

        for( int i = start; i < end; i++ ) {
            final char ch = idfa.charAt( i );
            if( !((ch >= '0' && ch <= '9') ||
                (ch >= 'A' && ch <= 'F') ||
                (ch >= 'a' && ch <= 'f')
            ) ) return false;
        }

        return true;
    }
}
