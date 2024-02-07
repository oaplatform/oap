
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

import lombok.extern.slf4j.Slf4j;
import oap.util.Result;

import java.util.Objects;
import java.util.Optional;

import static oap.ws.sso.WsSecurity.SYSTEM;

@Slf4j
public class JwtUserAuthenticator implements Authenticator {

    private final JwtTokenGenerator jwtTokenGenerator;
    private final JWTExtractor jwtExtractor;
    private final UserProvider userProvider;

    public JwtUserAuthenticator( UserProvider userProvider, JwtTokenGenerator jwtTokenGenerator, JWTExtractor jwtExtractor ) {
        this.userProvider = Objects.requireNonNull( userProvider );
        this.jwtTokenGenerator = Objects.requireNonNull( jwtTokenGenerator );
        this.jwtExtractor = Objects.requireNonNull( jwtExtractor );
    }

    @Override
    public Result<Authentication, AuthenticationFailure> authenticate( String email, String password, Optional<String> tfaCode ) {
        var authResult = userProvider.getAuthenticated( email, password, tfaCode );
        return getAuthenticationTokens( authResult );
    }

    @Override
    public Result<Authentication, AuthenticationFailure> authenticate( String email, Optional<String> tfaCode ) {
        var authResult = userProvider.getAuthenticated( email, tfaCode );
        return getAuthenticationTokens( authResult );
    }

    public Result<Authentication, AuthenticationFailure> authenticateWithActiveOrgId( String jwtToken, String orgId ) {
        if( jwtExtractor.verifyToken( jwtToken ) ) {
            log.trace( "generating new authentication token with active organization {} ", orgId );
            var user = userProvider.getUser( jwtExtractor.getUserEmail( jwtToken ) );
            if( user.isEmpty() ) {
                return Result.failure( AuthenticationFailure.UNAUTHENTICATED );
            }
            return user.filter( u -> validateUserAccess( u, orgId ) )
                .map( u -> getAuthenticationTokens( u, orgId ) )
                .orElse( Result.failure( AuthenticationFailure.WRONG_ORGANIZATION ) );
        }
        return Result.failure( AuthenticationFailure.TOKEN_NOT_VALID );
    }

    private boolean validateUserAccess( User user, String orgId ) {
        return user.getRoles().containsKey( orgId ) || user.getRoles().containsKey( SYSTEM );
    }

    private Result<Authentication, AuthenticationFailure> getAuthenticationTokens( Result<? extends User, AuthenticationFailure> authResult ) {
        if( !authResult.isSuccess() ) {
            return Result.failure( authResult.getFailureValue() );
        }
        User user = authResult.getSuccessValue();
        try {
            Authentication authentication = generateTokenWithOrgId( user, user.getDefaultOrganization().orElse( "" ) );
            return Result.success( authentication );
        } catch( Exception exception ) {
            log.error( "JWT creation failed {}", exception.getMessage() );
        }
        return null;
    }

    private Result<Authentication, AuthenticationFailure> getAuthenticationTokens( User user, String orgId ) {
        try {
            Authentication authentication = generateTokenWithOrgId( user, orgId );
            return Result.success( authentication );
        } catch( Exception exception ) {
            log.error( "JWT creation failed {}", exception.getMessage() );
        }
        return null;
    }

    private Authentication generateTokens( User user ) {
        var accessToken = jwtTokenGenerator.generateAccessToken( user );
        var refreshToken = jwtTokenGenerator.generateRefreshToken( user );
        log.trace( "generating authentication for user {} -> {}", user.getEmail(), accessToken );
        return new Authentication( accessToken, refreshToken, user );
    }

    private Authentication generateTokenWithOrgId( User user, String activeOrgId ) {
        var accessToken = jwtTokenGenerator.generateAccessTokenWithActiveOrgId( user, activeOrgId );
        var refreshToken = jwtTokenGenerator.generateRefreshToken( user );
        log.trace( "generating authentication for user {} -> {}", user.getEmail(), accessToken );
        return new Authentication( accessToken, refreshToken, user );
    }

    public Result<Authentication, AuthenticationFailure> refreshToken( String refreshToken, Optional<String> orgId ) {
        if( !jwtExtractor.verifyToken( refreshToken ) ) {
            return Result.failure( AuthenticationFailure.TOKEN_NOT_VALID );
        }
        return generateAuthentication( refreshToken, orgId );
    }

    private Result<Authentication, AuthenticationFailure> generateAuthentication( String refreshToken, Optional<String> orgId ) {
        var userEmail = jwtExtractor.getUserEmail( refreshToken );
        var user = userProvider.getUser( userEmail );

        if( user.isEmpty() ) {
            return Result.failure( AuthenticationFailure.UNAUTHENTICATED );
        }
        return buildAuthentication( user.get(), orgId );
    }

    private Result<Authentication, AuthenticationFailure> buildAuthentication( User user, Optional<String> orgId ) {
        if( orgId.isPresent() && !user.getRoles().containsKey( orgId.get() ) ) {
            return Result.failure( AuthenticationFailure.UNAUTHENTICATED );
        }
        String activeOrgId = orgId.orElse( user.getDefaultOrganization().orElse( "" ) );

        var authentication = new Authentication(
            jwtTokenGenerator.generateAccessTokenWithActiveOrgId( user, activeOrgId ),
            jwtTokenGenerator.generateRefreshToken( user ),
            user
        );

        return Result.success( authentication );
    }


    @Override
    public Optional<Authentication> authenticateTrusted( String email ) {
        return userProvider.getUser( email )
            .map( user -> {
                try {
                    return generateTokens( user );
                } catch( Exception exception ) {
                    log.error( "JWT creation failed {}", exception.getMessage() );
                    return null;
                }
            } );
    }

    @Override
    public Optional<Authentication> authenticateWithApiKey( String accessKey, String apiKey ) {
        return userProvider.getAuthenticatedByApiKey( accessKey, apiKey )
            .map( user -> {
                try {
                    return generateTokens( user );
                } catch( Exception exception ) {
                    log.error( "JWT creation failed {}", exception.getMessage() );
                    return null;
                }
            } );
    }

    @Override
    public void invalidate( String email ) {
        // No op
    }
}
