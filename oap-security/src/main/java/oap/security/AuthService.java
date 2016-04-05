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

import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

   private final TokenStorage tokenStorage;
   private final int expirationTime;

   public AuthService( TokenStorage tokenStorage, int expirationTime ) {
      this.tokenStorage = tokenStorage;
      this.expirationTime = expirationTime;
   }

   public Token generateToken( User user ) {
      final List<Token> userTokens = tokenStorage.select()
         .filter( token -> token.userEmail.equals( user.email ) )
         .toList();

      if( userTokens.isEmpty() ) {
         final Token token = new Token();
         token.userEmail = user.email;
         token.role = user.role;
         token.expire = DateTime.now().plusMinutes( expirationTime );
         token.id = UUID.randomUUID().toString();

         tokenStorage.store( token );

         return token;
      } else {
         final Token existingToken = Iterables.getOnlyElement( userTokens );

         return tokenStorage.update( existingToken.id, token -> {
            token.expire = DateTime.now().plusMinutes( expirationTime );
         } );
      }
   }

   public Optional<Token> getToken( String tokenId ) {
      final Optional<Token> tokenOptional = tokenStorage.get( tokenId );

      if( tokenOptional.isPresent() ) {
         final Token token = tokenOptional.get();

         if( token.expire.isAfterNow() ) {
            return Optional.of( token );
         } else {
            deleteToken( token.id );
         }
      }

      return Optional.empty();
   }

   public void deleteToken( String tokenId ) {
      tokenStorage.delete( tokenId );
   }
}
