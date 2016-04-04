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

import oap.util.Lists;

import java.util.List;
import java.util.Optional;

public abstract class OrganizationValidator {

   protected final OrganizationStorage organizationStorage;

   public OrganizationValidator( OrganizationStorage organizationStorage ) {
      this.organizationStorage = organizationStorage;
   }

   public List<String> organizationExists( String oname) {
      return organizationStorage
         .get( oname ).isPresent() ? Lists.empty() :
         Lists.of( "Organization " + oname + " does not exist." );
   }

   public List<String> userAlreadyExists( User user, String oname) {
      final Organization organization = organizationStorage.get( oname ).get();
      final Optional<User> userOptional = organization.users.get( user.username );

      return userOptional.isPresent() ? Lists.of( "User " + user.username + " already exist" ) : Lists.empty();
   }

}
