/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp;

import oap.util.Result;
import oap.ws.sso.AuthenticationFailure;
import oap.ws.sso.SecurityRoles;
import oap.ws.sso.User;
import oap.ws.sso.UserProvider;
import oap.ws.sso.UserWithCookies;

import java.io.Serial;
import java.util.Map;
import java.util.Optional;

public class TestUserProvider implements UserProvider {
    public static final String ACCESS_KEY = "test-access-key";
    public static final String API_KEY = "test-api-key";

    private final TestUser systemUser = new TestUser();

    @Override
    public Optional<? extends User> getAuthenticatedByApiKey( String accessKey, String apiKey ) {
        if( ACCESS_KEY.equals( accessKey ) && API_KEY.equals( apiKey ) ) return Optional.of( systemUser );
        return Optional.empty();
    }

    @Override
    public Optional<? extends User> getUser( String email ) {
        return Optional.empty();
    }

    @Override
    public Result<UserWithCookies, String> getAuthenticatedByAccessToken( Optional<String> accessToken,
                                                                          Optional<String> refreshToken,
                                                                          Optional<String> sessionUserId,
                                                                          SecurityRoles roles, String realm,
                                                                          String... wssPermissions ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email, String password,
                                                                           Optional<String> tfaCode ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<? extends User, AuthenticationFailure> getAuthenticated( String email,
                                                                           Optional<String> tfaCode ) {
        throw new UnsupportedOperationException();
    }

    public static class TestUser implements User {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public String getId() {
            return "test-system-user";
        }

        @Override
        public String getEmail() {
            return "admin@test.local";
        }

        @Override
        public Optional<String> getRole( String realm ) {
            return Optional.ofNullable( getRoles().get( realm ) );
        }

        @Override
        public Map<String, String> getRoles() {
            return Map.of( "SYSTEM", "ADMIN" );
        }

        @Override
        public Optional<String> getDefaultOrganization() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> getDefaultAccounts() {
            return Map.of();
        }

        @Override
        public Optional<String> getDefaultAccount( String organizationId ) {
            return Optional.empty();
        }

        @Override
        public long getCounter() {
            return 0;
        }
    }
}
