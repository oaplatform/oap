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
import org.testng.annotations.Test;

import static oap.ws.sso.JWTExtractor.TokenStatus.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.Assert.assertNotNull;


public class JwtTokenGeneratorExtractorTest extends Fixtures {
    @Test
    public void acceptsExistingHs256SignaturesAndRejectsInvalidClaims() throws Exception {
        String valid = signLegacy( "HS256", "{\"iss\":\"issuer\",\"exp\":4102444800,\"id\":\"legacy\",\"counter\":7}", "secret" );
        assertThat( jwtExtractor.verifyToken( valid ) ).isEqualTo( VALID );
        assertThat( jwtExtractor.decodeJWT( valid ).getUserId() ).isEqualTo( "legacy" );
        assertThat( jwtExtractor.decodeJWT( valid ).getCounter() ).isEqualTo( 7 );
        assertThat( jwtExtractor.verifyToken( null ) ).isEqualTo( JWTExtractor.TokenStatus.EMPTY );
        assertThat( jwtExtractor.verifyToken( "invalid" ) ).isEqualTo( JWTExtractor.TokenStatus.INVALID );
        assertThat( jwtExtractor.verifyToken( signLegacy( "HS256", "{\"iss\":\"issuer\",\"exp\":1}", "secret" ) ) ).isEqualTo( JWTExtractor.TokenStatus.EXPIRED );
        for( String claims : java.util.List.of( "{\"iss\":\"wrong\"}", "{}", "{\"iss\":\"issuer\",\"nbf\":4102444800}", "{\"iss\":\"issuer\",\"iat\":4102444800}" ) ) {
            assertThat( jwtExtractor.verifyToken( signLegacy( "HS256", claims, "secret" ) ) ).isEqualTo( JWTExtractor.TokenStatus.INVALID );
        }
        assertThat( jwtExtractor.verifyToken( signLegacy( "HS256", "{\"iss\":\"issuer\"}", "wrong-secret" ) ) ).isEqualTo( JWTExtractor.TokenStatus.INVALID );
        assertThat( jwtExtractor.verifyToken( signLegacy( "HS384", "{\"iss\":\"issuer\"}", "secret" ) ) ).isEqualTo( JWTExtractor.TokenStatus.INVALID );
        assertThat( jwtExtractor.verifyToken( "eyJhbGciOiJub25lIn0.eyJpc3MiOiJpc3N1ZXIifQ." ) ).isEqualTo( JWTExtractor.TokenStatus.INVALID );
    }

    @Test
    public void generatedTokensKeepLegacyHs256SignaturesAndRefreshClaims() throws Exception {
        var user = new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) );
        for( String secret : java.util.List.of( "secret", "long-secret".repeat( 10 ) ) ) {
            var generator = new JwtTokenGenerator( secret, secret, "issuer", 60000, 120000 );
            var token = generator.generateAccessTokenWithActiveOrgId( user, "org1" );
            String[] parts = token.jwt.split( "\\." );
            assertThat( parts[2] ).isEqualTo( signature( "HS256", parts[0] + "." + parts[1], secret ) );
            var extractor = new JWTExtractor( secret, "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );
            assertThat( extractor.decodeJWT( token.jwt ).getOrganizationId() ).isEqualTo( "org1" );
            assertThat( extractor.decodeJWT( generator.generateRefreshToken( user ).jwt ).getPermissions( "org1" ) ).isEmpty();
        }
    }

    private static String signLegacy( String algorithm, String payload, String secret ) throws Exception {
        var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        var charset = java.nio.charset.StandardCharsets.UTF_8;
        String header = "{\"typ\":\"JWT\",\"alg\":\"" + algorithm + "\"}";
        String content = encoder.encodeToString( header.getBytes( charset ) ) + "." + encoder.encodeToString( payload.getBytes( charset ) );
        return content + "." + signature( algorithm, content, secret );
    }

    private static String signature( String algorithm, String content, String secret ) throws Exception {
        var charset = java.nio.charset.StandardCharsets.UTF_8;
        var mac = javax.crypto.Mac.getInstance( "HS384".equals( algorithm ) ? "HmacSHA384" : "HmacSHA256" );
        mac.init( new javax.crypto.spec.SecretKeySpec( secret.getBytes( charset ), mac.getAlgorithm() ) );
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString( mac.doFinal( content.getBytes( charset ) ) );
    }

    private static final JwtTokenGenerator jwtTokenGenerator = new JwtTokenGenerator( "secret", "secret", "issuer", 15 * 60 * 1000, 15 * 60 * 1000 * 24 );
    private static final JWTExtractor jwtExtractor = new JWTExtractor( "secret", "issuer", new SecurityRoles( new TestSecurityRolesProvider() ) );

    public JwtTokenGeneratorExtractorTest() {
        fixture( new SystemTimerFixture( true ) );
    }

    @Test
    public void generateAndExtractToken() {
        Authentication.Token token = jwtTokenGenerator.generateAccessToken( new TestUser( "email@email.com", "password", Pair.of( "org1", "ADMIN" ) ) );
        assertNotNull( token.expires );
        assertThat( token.jwt ).isNotEmpty();
        assertThat( token.expires )
            .isBeforeOrEqualTo( new DateTime( UTC ).plusMinutes( 15 ).toDate() )
            .isAfterOrEqualTo( new DateTime( UTC ).plusMinutes( 14 ).toDate() );
        assertThat( jwtExtractor.verifyToken( token.jwt ) ).isEqualTo( VALID );

        JwtToken jwtToken = jwtExtractor.decodeJWT( token.jwt );

        assertThat( jwtToken.getUserEmail() ).isEqualTo( "email@email.com" );
        assertThat( jwtToken.getPermissions( "org1" ) ).containsExactlyInAnyOrder( "accounts:list", "accounts:create" );
    }
}
