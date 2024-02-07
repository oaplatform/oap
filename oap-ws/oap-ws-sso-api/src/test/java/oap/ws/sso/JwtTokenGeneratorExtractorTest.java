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

import oap.util.Pair;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import static oap.testng.Asserts.assertString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class JwtTokenGeneratorExtractorTest extends AbstractUserTest {

    private final JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator( "secret", "secret", "issuer", 15 * 60 * 1000, 15 * 60 * 1000 * 24 );
    private final JWTExtractor jwtExtractor = new JWTExtractor( "secret", "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );

    @Test
    public void generateAndExtractToken() {
        final Pair<Date, String> token = jwtTokenGenerator.generateAccessToken( new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) ) );
        assertNotNull( token._1 );
        assertString( token._2 ).isNotEmpty();
        Instant expirationTime = token._1.toInstant().truncatedTo( ChronoUnit.MINUTES );
        Instant expectedExpirationTime = Instant.now().plus( Duration.ofMinutes( 15 ) ).truncatedTo( ChronoUnit.MINUTES );
        assertTrue( expirationTime.compareTo( expectedExpirationTime ) == 0 );
        assertTrue( jwtExtractor.verifyToken( token._2 ) );
        assertEquals( jwtExtractor.getUserEmail( token._2 ), "email@email.com" );
        assertEquals( jwtExtractor.getPermissions( token._2, "org1" ), Set.of( "accounts:list", "accounts:create" ) );
    }
}
