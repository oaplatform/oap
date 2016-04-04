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

import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.Validate;

import java.util.Objects;

import static java.lang.String.format;
import static oap.http.Request.HttpMethod.*;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;

public class OrganizationWS extends OrganizationValidator {

   public OrganizationWS( OrganizationStorage organizationStorage ) {
      super( organizationStorage );
   }

   @WsMethod( method = POST, path = "/store" )
   public void store( @WsParam( from = BODY ) Organization organization ) {
      organizationStorage.store( organization );
   }

   @WsMethod( method = GET, path = "/{oname}" )
   public Organization getOrganization( @WsParam( from = PATH ) @Validate( "organizationExists" ) String oname ) {
      return organizationStorage.get( oname ).get();
   }

   @WsMethod( method = DELETE, path = "/remove/{oname}" )
   public void removeOrganization( @WsParam( from = PATH ) String oname ) {
      organizationStorage.delete( oname );
   }

   @WsMethod( method = POST, path = "/{oname}/store-user" )
   @Validate( "userAlreadyExists" )
   public void storeUser( @WsParam( from = BODY ) User user,
                          @WsParam( from = PATH ) @Validate( "organizationExists" ) String oname ) {
      organizationStorage.update( oname,
         organization -> organization.users.get( user.username )
            .ifPresent( u -> {
                  u.username = user.username;
                  u.password = user.password;
                  u.role = user.role;
               }
            )
      );
   }

   @WsMethod( method = GET, path = "/{oname}/user/{username}" )
   public User getUser( @WsParam( from = PATH ) @Validate( "organizationExists" ) String oname,
                        @WsParam( from = PATH ) String username ) {
      final Organization.Users users = organizationStorage.get( oname ).get().users;

      return users.get( username ).orElseThrow( () ->
         new RuntimeException( format( "User [%s] not found", username ) ) );
   }

   @WsMethod( method = DELETE, path = "/{oname}/remove-user/{username}" )
   public void removeUser( @WsParam( from = PATH ) @Validate( "organizationExists" ) String oname,
                           @WsParam( from = PATH ) String username ) {
      organizationStorage.update( oname,
         organization -> organization.users.removeIf( user -> Objects.equals( user.username, username ) )
      );
   }

}
