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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AuthService {

   private final TokenStorage tokenStorage;

   public AuthService( TokenStorage tokenStorage ) {
      this.tokenStorage = tokenStorage;
   }

   public Token generateToken( User user, String organization ) {

      final List<Token> userTokens = tokenStorage.select()
         .filter( token -> token.username.equals( user.username ) )
         .filter( token -> token.organization.equals( organization ) )
         .collect( Collectors.toList() );

      Preconditions.checkState( userTokens.size() < 1,
         format( "There are multiple usernames [%s] within [%s] organization", user.username, organization ) );

      if( userTokens.isEmpty() ) {
         final Token token = new Token();
         token.username = user.username;
         token.role = user.role;
         token.expire = DateTime.now().plusHours( 1 );
         token.id = UUID.randomUUID().toString();

         tokenStorage.store( token );

         return token;
      } else {
         final Token existingToken = Iterables.getOnlyElement( userTokens );

         return tokenStorage.update( existingToken.id, token -> {
            token.expire = DateTime.now().plusHours( 1 );
         } );
      }
   }

   public Token getToken( String tokenId ) {
      return tokenStorage.get( tokenId ).orElse( null );
   }

}
