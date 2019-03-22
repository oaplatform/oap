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

package oap.security.ws;

import lombok.val;
import oap.security.acl.MockUser2;
import oap.storage.Identifier;
import oap.storage.MemoryStorage;
import oap.util.Lists;
import oap.sso.PasswordHasher;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static oap.storage.Storage.Lock.SERIALIZED;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthService2Test {
    private MockAuthProvider authProvider;
    private AuthService2 authService;

    @Test
    public void testGenerateToken() {
        DateTimeUtils.setCurrentMillisFixed( 0 );

        assertThat( authService.generateToken( "id" ) ).isEmpty();

        authProvider.addUser( new MockUser2( "id" ) );

        assertThat( authService.generateToken( "id" ).get().userId ).isEqualTo( "id" );
        assertThat( authService.generateToken( "id" ).get().lastAccess ).isEqualTo( 0 );
    }

    @Test
    public void testLastAccess() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        authProvider.addUser( new MockUser2( "id" ) );


        val token1 = authService.generateToken( "id" ).get();
        assertThat( token1.lastAccess ).isEqualTo( 0 );

        DateTimeUtils.setCurrentMillisFixed( 100 );
        val token2 = authService.getToken( token1.id ).get();
        assertThat( token2 ).isSameAs( token1 );
        assertThat( authService.generateToken( "id" ).get().lastAccess ).isEqualTo( 100 );
    }

    @BeforeMethod
    public void beforeMethod() {

        authProvider = new MockAuthProvider();
        authService = new AuthService2( Lists.of( authProvider ),
            new PasswordHasher( "salt" ),
            new MemoryStorage<>( Identifier.forAnnotationFixed(), SERIALIZED ),
            10000 );
    }

    @AfterMethod
    public void afterMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }
}
