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

package oap.ws.sso;

import oap.util.Result;

import java.util.Optional;
import java.util.function.Function;

public interface UserProvider {
    Optional<? extends User> getUser( String email );

    Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password, Optional<String> tfaCode );

    Result<? extends User, AuthenticationFailure> getAuthenticated( String email, Optional<String> tfaCode );

    Optional<? extends User> getAuthenticatedByApiKey( String accessKey, String apiKey );

    //eliminating most used letters in english from source
    static String toAccessKey( String email ) {
        int[] transitions = { 6, 11, 3, 10, 4, 1, 5, 0, 7, 2, 9, 8 };
        StringBuilder result = new StringBuilder();
        Function<Character, Boolean> isGoodLetter = c ->
            ( c > 64 && c < 91 || c > 96 && c < 123 )
                && c != 'E' && c != 'e'
                && c != 'T' && c != 't'
                && c != 'A' && c != 'a'
                && c != 'O' && c != 'o'
                && c != 'I' && c != 'i'
                && c != 'N' && c != 'n';
        for( int t : transitions ) {
            if( t >= email.length() || !isGoodLetter.apply( email.charAt( t ) ) ) {
                var c = email.charAt( t % email.length() );
                var base = Character.toUpperCase( isGoodLetter.apply( c ) ? c : 'A' + ( c % 26 ) );
                result.append( ( char ) ( base + t <= 'Z' ? base + t : base - t ) );
            } else
                result.append( Character.toUpperCase( email.charAt( t ) ) );
        }
        return result.toString();
    }
}
