/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.util;

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
