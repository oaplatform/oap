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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import oap.time.JodaClock;

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

    public JwtToken decodeJWT( String token ) throws JWTVerificationException {
        if( token == null ) {
            return null;
        }
        Algorithm algorithm = Algorithm.HMAC256( secret );
        JWTVerifier.BaseVerification verification = ( JWTVerifier.BaseVerification ) JWT.require( algorithm ).withIssuer( issuer );
        JWTVerifier verifier = verification.build( new JodaClock() );
        return new JwtToken( verifier.verify( token ), roles );
    }

    public TokenStatus verifyToken( String token ) {
        if( token == null ) {
            return TokenStatus.EMPTY;
        }
        try {
            decodeJWT( token );

            return TokenStatus.VALID;
        } catch( TokenExpiredException e ) {
            return TokenStatus.EXPIRED;
        } catch( JWTVerificationException e ) {
            return TokenStatus.INVALID;
        }
    }

    public enum TokenStatus {
        EMPTY, INVALID, VALID, EXPIRED
    }

}
