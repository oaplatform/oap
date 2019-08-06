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

import oap.http.testng.HttpAsserts;
import oap.testng.Fixtures;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.testng.WsFixture;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.ws.WsParam.From.SESSION;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

public class SecureWSTest extends Fixtures {
    WsFixture wsFixture;

    {
        fixture( wsFixture = new WsFixture( getClass(), ( ws, kernel ) -> {
            TestUserStorage userStorage = new TestUserStorage();
            kernel.register( "user-storage", userStorage );
            AuthService authService = new AuthService( userStorage, 100 );
            kernel.register( "auth", new AuthWS(
                authService,
                null,
                100 ) );
            kernel.register( "security", new SecurityInterceptor( new DefaultTokenService( authService ),
                new Roles( Map.of(
                    "ADMIN", List.of( "ALLOWED" ),
                    "USER", List.of()
                ) ) ) );
            kernel.register( "secure", new SecureWS() );
        }, "ws-secure.conf" ) );
    }

    @Test
    public void allowed() {
        wsFixture.server.kernel.<TestUserStorage>service( "user-storage" ).get()
            .addUser( new TestUser( "admin@admin.com", "pass", "ADMIN" ) );
        assertGet( HttpAsserts.httpUrl( "/auth/login?email=admin@admin.com&password=pass" ) )
            .hasCode( 200 );
        assertGet( HttpAsserts.httpUrl( "/secure" ) )
            .responded( 200, "OK", TEXT_PLAIN.withCharset( UTF_8 ), "admin@admin.com" );

    }

    @Test
    public void notLoggedIn() {
        wsFixture.server.kernel.<TestUserStorage>service( "user-storage" ).get()
            .addUser( new TestUser( "admin@admin.com", "pass", "ADMIN" ) );
        assertGet( HttpAsserts.httpUrl( "/secure" ) )
            .hasCode( 401 );
    }

    @Test
    public void denied() {
        wsFixture.server.kernel.<TestUserStorage>service( "user-storage" ).get()
            .addUser( new TestUser( "admin@admin.com", "pass", "USER" ) );
        assertGet( HttpAsserts.httpUrl( "/auth/login?email=admin@admin.com&password=pass" ) )
            .hasCode( 200 );
        assertGet( HttpAsserts.httpUrl( "/secure" ) )
            .hasCode( 403 );
    }

    @SuppressWarnings( "unused" )
    public static class SecureWS {
        @WsSecurity( permissions = "ALLOWED" )
        @WsMethod( path = "/", produces = "text/plain" )
        public String secure( @WsParam( from = SESSION ) User user ) {
            return user.getEmail();
        }
    }

    public static class TestUserStorage implements UserStorage {
        public final List<User> users = new ArrayList<>();

        public void addUser( TestUser user ) {
            users.add( user );
        }

        @Override
        public Optional<User> getUser( String email ) {
            return users.stream().filter( u -> u.getEmail().equalsIgnoreCase( email ) ).findAny();
        }

        @Override
        public Optional<User> getAuthenticated( String email, String password ) {
            return users.stream().filter( u -> u.getEmail().equalsIgnoreCase( email ) && u.getPassword().equals( password ) ).findAny();
        }
    }

    public static class TestUser implements User {
        public final String email;
        public final String password;
        public final String role;

        public TestUser( String email, String password, String role ) {
            this.email = email;
            this.password = password;
            this.role = role;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getRole() {
            return role;
        }
    }
}
