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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;

public class JwtTokenGenerator {
    private final String accessSecret;
    private final String refreshSecret;
    private final String issuer;
    private final long accessSecretExpiration;
    private final long refreshSecretExpiration;

    public JwtTokenGenerator( String accessSecret, String refreshSecret, String issuer, long accessSecretExpiration, long refreshSecretExpiration ) {
        this.accessSecret = accessSecret;
        this.refreshSecret = refreshSecret;
        this.issuer = issuer;
        this.accessSecretExpiration = accessSecretExpiration;
        this.refreshSecretExpiration = refreshSecretExpiration;
    }

    public Authentication.Token generateAccessToken( User user ) {
        return sign( claims( user, accessSecretExpiration ).claim( "roles", user.getRoles() ).build(), accessSecret );
    }

    public Authentication.Token generateAccessTokenWithActiveOrgId( User user, String activeOrganization ) {
        return sign( claims( user, accessSecretExpiration ).claim( "roles", user.getRoles() ).claim( "org_id", activeOrganization ).build(), accessSecret );
    }

    public Authentication.Token generateRefreshToken( User user ) {
        return sign( claims( user, refreshSecretExpiration ).build(), refreshSecret );
    }

    private JWTClaimsSet.Builder claims( User user, long expiration ) {
        return new JWTClaimsSet.Builder().claim( "id", user.getId() ).claim( "user", user.getEmail() )
            .claim( "counter", user.getCounter() ).issuer( issuer ).expirationTime( new Date( System.currentTimeMillis() + expiration ) );
    }

    private Authentication.Token sign( JWTClaimsSet claims, String secret ) {
        try {
            var jwt = new SignedJWT( new JWSHeader.Builder( JWSAlgorithm.HS256 ).type( JOSEObjectType.JWT ).build(), claims );
            jwt.sign( new MACSigner( JwtHmac.key( secret ) ) );
            return new Authentication.Token( claims.getExpirationTime(), jwt.serialize() );
        } catch( JOSEException e ) {
            throw new IllegalStateException( "Cannot sign JWT", e );
        }
    }
}
