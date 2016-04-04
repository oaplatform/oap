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

package oap.security;


import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public final class HashUtils {

   private HashUtils() {
   }

   public static String hash( String salt, String input ) {
      final StringBuilder stringBuilder = new StringBuilder();
      try {
         final MessageDigest messageDigest = MessageDigest.getInstance( "SHA-256" );

         messageDigest.update( salt.getBytes("UTF-8") );

         final byte[] hashedInput = messageDigest.digest( input.getBytes() );

         for( byte b : hashedInput ) {
            stringBuilder.append( String.format( "%02x", b & 0xff ) );
         }
      } catch( NoSuchAlgorithmException e ) {
         log.error( "No native SHA-256 algorithm support for this JVM", e );
         throw new RuntimeException( e );
      } catch( UnsupportedEncodingException e ) {
         log.error( "UTF-8 encoding is not supported", e );
         throw new RuntimeException( e );
      }

      return stringBuilder.toString();
   }
}
