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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Hash {

    private Hash() {
    }

    public static String md5( String input ) {
        return hash( "", input, "MD5" );
    }

    public static String sha256( String salt, String input ) {
        return hash( salt, input, "SHA-256" );
    }

    public static String hash( String salt, String input, String algorithm ) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance( algorithm );

            messageDigest.update( salt.getBytes( UTF_8 ) );

            final byte[] hashedInput = messageDigest.digest( input.getBytes() );

            return Strings.toHexString( hashedInput );
        } catch( NoSuchAlgorithmException e ) {
            throw new RuntimeException( e );
        }
    }

    public static String ak( String value ) {
        return AKHash.hash( value );
    }

    public static String ak( String value, int length ) {
        return AKHash.hash( value, length );
    }
}
