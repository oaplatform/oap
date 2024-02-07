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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Pair;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AbstractUserTest {


    public static class TestSecurityRolesProvider extends AbstractSecurityRolesProvider {
        protected TestSecurityRolesProvider() {
            super( Map.of( "ADMIN", Set.of( "accounts:list", "accounts:create" ), "USER", Set.of( "accounts:list" ) ) );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestUser implements User {
        public final String email;
        public final String password;
        public final Map<String, String> roles = new HashMap<>();
        public final boolean tfaEnabled;
        public final String apiKey = RandomStringUtils.random( 10, true, true );
        public String defaultOrganization = "";
        public final Map<String, String> defaultAccounts = new HashMap<>();
        @JsonIgnore
        public final View view = new View();

        public TestUser( String email, String password, Pair<String, String> role ) {
            this( email, password, role, false );
        }

        public TestUser( String email, String password, Pair<String, String> role, boolean tfaEnabled ) {
            this.email = email;
            this.password = password;
            this.roles.put( role._1, role._2 );
            this.tfaEnabled = tfaEnabled;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public Optional<String> getRole( String realm ) {
            return Optional.ofNullable( roles.get( realm ) );
        }

        @Override
        public Map<String, String> getRoles() {
            return roles;
        }

        @Override
        public Optional<String> getDefaultOrganization() {
            return Optional.ofNullable( defaultOrganization );
        }

        @Override
        public Map<String, String> getDefaultAccounts() {
            return defaultAccounts;
        }

        @Override
        public Optional<String> getDefaultAccount( String organizationId ) {
            return Optional.ofNullable( roles.get( organizationId ) );
        }

        @Override
        public View getView() {
            return view;
        }

        public class View implements User.View {
            @Override
            public String getEmail() {
                return TestUser.this.getEmail();
            }
        }

    }
}
