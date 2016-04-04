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
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.Validate;
import org.joda.time.DateTime;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class LoginWS extends OrganizationValidator {

   private final AuthService authService;
   private final String salt;

   public LoginWS( OrganizationStorage organizationStorage, AuthService authService, String salt ) {
      super( organizationStorage );
      this.authService = authService;
      this.salt = salt;
   }

   @WsMethod( method = GET, path = "/{oname}" )
   @Validate( "organizationExists" )
   public Token login( @WsParam( from = PATH ) String oname,
                       @WsParam( from = QUERY ) String username,
                       @WsParam( from = QUERY ) String password ) {
      final Organization organization = organizationStorage.get( oname ).get();

      final Optional<User> userOptional = organization.users.get( username );

      if( userOptional.isPresent() ) {
         final User user = userOptional.get();

         final String inputPassword = HashUtils.hash( salt, password );
         if( user.password.equals( inputPassword ) ) {
            return authService.generateToken( user, oname );
         }
      }

      return null;
   }

}
