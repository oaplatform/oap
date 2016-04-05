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

import oap.io.Resources;
import oap.json.TypeIdFactory;
import oap.testng.AbstractTest;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AuthServiceTest extends AbstractTest {

   private AuthService authService;

   @BeforeTest
   public void setUp() {
      TypeIdFactory.register( User.class, User.class.getName() );

      TokenStorage tokenStorage = new TokenStorage( Resources.filePath( AuthServiceTest.class, "" ).get() );

      authService = new AuthService( tokenStorage );

      tokenStorage.start();
   }

   @Test
   public void testShouldGenerateNewToken() {
      final User user = new User();
      user.email = "test@example.com";
      user.password = "12345";
      user.role = Role.ADMIN;

      final Token token = authService.generateToken( user );

      assertEquals( token.role, Role.ADMIN );
      assertEquals( token.userEmail, "test@example.com" );
      assertNotNull( token.id );
      assertNotNull( token.expire );
   }

   @Test
   public void testShouldUpdateExpirationTimeOfExistingToken() throws InterruptedException {
      final User user = new User();
      user.email = "test@example.com";
      user.password = "12345";
      user.role = Role.ADMIN;

      final DateTime expire = authService.generateToken( user ).expire;
      final DateTime updatedExpire = authService.generateToken( user ).expire;

      assertNotEquals( expire, updatedExpire );
   }
}