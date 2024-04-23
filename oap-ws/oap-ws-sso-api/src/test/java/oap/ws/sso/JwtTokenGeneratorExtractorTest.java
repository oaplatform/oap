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
import oap.util.Dates;
import oap.util.Pair;
import oap.ws.sso.AbstractUserTest.TestSecurityRolesProvider;
import oap.ws.sso.AbstractUserTest.TestUser;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.Date;

import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class JwtTokenGeneratorExtractorTest extends Fixtures {
    private static final JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator( "secret", "secret", "issuer", 15 * 60 * 1000, 15 * 60 * 1000 * 24 );
    private static final JWTExtractor jwtExtractor = new JWTExtractor( "secret", "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );

    public JwtTokenGeneratorExtractorTest() {
        fixture( new SystemTimerFixture() );
    }

    @Test
    public void generateAndExtractToken() {
        Dates.setTimeFixed( 2024, 4, 23, 14, 45, 56, 12 );


        Pair<Date, String> token = jwtTokenGenerator.generateAccessToken( new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) ) );
        assertNotNull( token._1 );
        assertString( token._2 ).isNotEmpty();
        assertThat( token._1 ).isEqualTo( new DateTime( UTC ).plusMinutes( 15 ).toDate() );
        assertTrue( jwtExtractor.verifyToken( token._2 ) );
        assertThat( jwtExtractor.getUserEmail( token._2 ) ).isEqualTo( "email@email.com" );
        assertThat( jwtExtractor.getPermissions( token._2, "org1" ) ).containsExactlyInAnyOrder( "accounts:list", "accounts:create" );
    }
}
