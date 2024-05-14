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

import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.util.Pair;
import oap.ws.sso.AbstractUserTest.TestSecurityRolesProvider;
import oap.ws.sso.AbstractUserTest.TestUser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;
import static oap.ws.sso.JWTExtractor.TokenStatus.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.Assert.assertNotNull;


public class JwtTokenGeneratorExtractorTest extends Fixtures {
    private static final JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator( "secret", "secret", "issuer", 15 * 60 * 1000, 15 * 60 * 1000 * 24 );
    private static final JWTExtractor jwtExtractor = new JWTExtractor( "secret", "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );

    public JwtTokenGeneratorExtractorTest() {
        fixture( new SystemTimerFixture() );
    }

    @Test
    public void generateAndExtractToken() {
        DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() );

        Authentication.Token token = jwtTokenGenerator.generateAccessToken( new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) ) );
        assertNotNull( token.expires );
        assertString( token.jwt ).isNotEmpty();
        assertThat( token.expires ).isEqualTo( new DateTime( UTC ).plusMinutes( 15 ).toDate() );
        assertThat( jwtExtractor.verifyToken( token.jwt ) ).isEqualTo( VALID );

        JwtToken jwtToken = jwtExtractor.decodeJWT( token.jwt );

        assertThat( jwtToken.getUserEmail() ).isEqualTo( "email@email.com" );
        assertThat( jwtToken.getPermissions( "org1" ) ).containsExactlyInAnyOrder( "accounts:list", "accounts:create" );
    }
}
