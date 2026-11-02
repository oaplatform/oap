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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import lombok.extern.slf4j.Slf4j;
import oap.time.JodaClock;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;

@Slf4j
public class JWTExtractor {

    public static final String BEARER = "Bearer ";
    private final String secret;
    private final String issuer;
    private final SecurityRoles roles;

    public JWTExtractor( String secret, String issuer, SecurityRoles roles ) {
        this.secret = secret;
        this.issuer = issuer;
        this.roles = roles;
    }

    public static String extractBearerToken( String authorization ) {
        if( authorization != null && authorization.startsWith( BEARER ) ) {
            return authorization.substring( BEARER.length() );
        }
        return authorization;
    }

    public JwtToken decodeJWT( String token ) {
        if( token == null ) {
            return null;
        }
        try {
            var jwt = SignedJWT.parse( token );
            if( !JWSAlgorithm.HS256.equals( jwt.getHeader().getAlgorithm() )
                || !jwt.verify( new MACVerifier( JwtHmac.key( secret ) ) ) ) throw new IllegalArgumentException( "Invalid JWT signature or algorithm" );
            var claims = jwt.getJWTClaimsSet();
            Date now = Date.from( new JodaClock().instant() );
            if( !issuer.equals( claims.getIssuer() ) ) throw new IllegalArgumentException( "Invalid JWT issuer" );
            if( claims.getExpirationTime() != null && !now.before( claims.getExpirationTime() ) ) throw new ExpiredTokenException();
            var verifier = new DefaultJWTClaimsVerifier<SecurityContext>( new JWTClaimsSet.Builder().issuer( issuer ).build(), Set.of( "iss" ) ) {
                @Override
                protected Date currentTime() {
                    return now;
                }
            };
            verifier.setMaxClockSkew( 0 );
            verifier.verify( claims, null );
            if( claims.getIssueTime() != null && claims.getIssueTime().after( now ) ) throw new IllegalArgumentException( "JWT issued in the future" );
            return new JwtToken( claims, roles );
        } catch( ParseException | JOSEException | BadJWTException e ) {
            throw new IllegalArgumentException( "Invalid JWT", e );
        }
    }

    public TokenStatus verifyToken( String token ) {
        if( token == null ) {
            return TokenStatus.EMPTY;
        }
        try {
            decodeJWT( token );

            return TokenStatus.VALID;
        } catch( ExpiredTokenException e ) {
            return TokenStatus.EXPIRED;
        } catch( IllegalArgumentException e ) {
            return TokenStatus.INVALID;
        }
    }

    private static final class ExpiredTokenException extends IllegalArgumentException {}

    public enum TokenStatus {
        EMPTY, INVALID, VALID, EXPIRED
    }

}
